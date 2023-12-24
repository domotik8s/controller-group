package io.domotik8s.groupcontroller;

import io.domotik8s.model.Property;
import io.domotik8s.model.PropertyState;
import io.domotik8s.model.PropertyStatus;
import io.domotik8s.model.bool.BooleanProperty;
import io.domotik8s.model.bool.BooleanPropertyState;
import io.domotik8s.model.bool.BooleanPropertyStatus;
import io.domotik8s.model.group.*;
import io.domotik8s.model.num.NumberProperty;
import io.domotik8s.model.num.NumberPropertyState;
import io.domotik8s.model.num.NumberPropertyStatus;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GroupService {

    private Logger logger = LoggerFactory.getLogger(GroupService.class);


    @Qualifier("groupClient")
    private final GenericKubernetesApi<Group, GroupList> groupClient;

    @Qualifier("booleanPropertyInformer")
    private final SharedIndexInformer<BooleanProperty> booleanPropertyInformer;

    @Qualifier("numberPropertyInformer")
    private final SharedIndexInformer<NumberProperty> numberPropertyInformer;


    public void addPropertyToGroup(Group group, Property property) {
        Selector selector = Optional.ofNullable(group).map(Group::getSpec).map(GroupSpec::getSelector).orElse(new Selector() {
            public boolean select(Property property) {
            return false;
            }
        });

        boolean selected = selector.select(property);
        if (selected) {
            GroupStatus status = Optional.ofNullable(group).map(Group::getStatus).orElse(new GroupStatus());
            group.setStatus(status);

            Set<PropertySelector> members = Optional.ofNullable(status.getMembers()).orElse(new HashSet<>());
            status.setMembers(members);

            boolean added = members.add(PropertySelector.of(property));
            if (added) {
                KubernetesApiResponse<Group> response = groupClient.updateStatus(group, (g) -> g.getStatus());
                logger.debug("Updating group returned response: {} {}", response.getHttpStatusCode());
                logger.debug("Added property {} to group {}", property.getMetadata().getName(), group.getMetadata().getName());
                updateGroupAggregation(group);
            }
        }
    }

    public void removePropertyFromGroup(Group group, Property property) {
        PropertySelector selector = PropertySelector.of(property);

        Optional<Set<PropertySelector>> membersOpt = Optional.ofNullable(group)
                .map(Group::getStatus)
                .map(GroupStatus::getMembers);

        if (membersOpt.isPresent() && membersOpt.get().remove(selector)) {
            groupClient.updateStatus(group, (g) -> g.getStatus());
            logger.debug("Removed property {} from group {}", property.getMetadata().getName(), group.getMetadata().getName());
            updateGroupAggregation(group);
        }
    }


    public void refreshGroup(Group group) {
        // Something in the Group's desired state has changed. We need to assume the selectors are different to we
        // have to clear all members and run each property through the selector again

        GroupStatus status = Optional.of(group)
                .map(Group::getStatus)
                .orElse(new GroupStatus());
        group.setStatus(status);

        Set<PropertySelector> members = Optional.of(group)
                .map(Group::getStatus)
                .map(GroupStatus::getMembers)
                .orElse(new HashSet<>());
        status.setMembers(members);

        members.clear();

        Selector selector = Optional.of(group)
                .map(Group::getSpec)
                .map(GroupSpec::getSelector)
                .orElse(new Selector() {
                    public boolean select(Property property) {
                        return false;
                    }
                });

        Set<Property> selected = new HashSet<>();
        selected.addAll(numberPropertyInformer.getIndexer().list().stream().filter(p -> selector.select(p)).collect(Collectors.toSet()));
        selected.addAll(booleanPropertyInformer.getIndexer().list().stream().filter(p -> selector.select(p)).collect(Collectors.toSet()));

        members.addAll(selected.stream()
            .map(p -> PropertySelector.of(p))
            .collect(Collectors.toSet()));

        groupClient.updateStatus(group, (g) -> g.getStatus());
        logger.debug("Refreshed group memberships of group {}", group.getMetadata().getName());

        updateGroupAggregation(group);
    }


    public void updateGroupAggregation(Group group) {
        Optional<Aggregation> aggrOpt = Optional.ofNullable(group)
                .map(Group::getSpec)
                .map(GroupSpec::getAggregation);

        if (aggrOpt.isEmpty()) {
            group.getStatus().setResult(null);
            groupClient.updateStatus(group, g -> g.getStatus());
            return;
        }

        Set<PropertySelector> selectors = Optional.ofNullable(group)
                .map(Group::getStatus)
                .map(GroupStatus::getMembers)
                .orElse(Set.of());

        List<Number> numberValues = selectors.stream()
                .filter(s -> "NumberProperty".equals(s.getKind()))
                .map(s -> numberPropertyInformer.getIndexer().list().stream()
                            .filter(np -> np.getMetadata().getName().equals(s.getName()))
                            .findFirst())
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .map(np -> Optional.of(np).map(NumberProperty::getStatus).map(NumberPropertyStatus::getState).map(NumberPropertyState::getValue))
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
            .collect(Collectors.toList());

        List<Boolean> booleanValues = selectors.stream()
                .filter(s -> "BooleanProperty".equals(s.getKind()))
                .map(s -> booleanPropertyInformer.getIndexer().list().stream()
                        .filter(np -> np.getMetadata().getName().equals(s.getName()))
                        .findFirst())
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .map(bp -> Optional.of(bp).map(BooleanProperty::getStatus).map(BooleanPropertyStatus::getState).map(BooleanPropertyState::getValue))
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .collect(Collectors.toList());

        List<Object> values = new ArrayList<>();
        values.addAll(numberValues);
        values.addAll(booleanValues);

        Aggregation aggregate = aggrOpt.get();
        Object aggregateValue = aggregate.aggregate(values);

        GroupStatus status = Optional.of(group.getStatus()).orElse(new GroupStatus());
        group.setStatus(status);

        AggregationResult result = Optional.of(status).map(GroupStatus::getResult).orElse(new AggregationResult());
        status.setResult(result);

        result.setValue(aggregateValue);

        groupClient.updateStatus(group, (g) -> g.getStatus());
        logger.debug("Refreshed group aggregation result of group {}", group.getMetadata().getName());
    }

}
