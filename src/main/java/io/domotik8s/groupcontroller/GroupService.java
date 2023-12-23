package io.domotik8s.groupcontroller;

import io.domotik8s.model.Property;
import io.domotik8s.model.group.*;
import io.domotik8s.model.num.NumberProperty;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GroupService {

    private Logger logger = LoggerFactory.getLogger(GroupService.class);


    @Qualifier("groupClient")
    private final GenericKubernetesApi<Group, GroupList> groupClient;

    @Qualifier("booleanPropertyInformer")
    private final SharedIndexInformer<NumberProperty> booleanPropertyInformer;

    @Qualifier("numberPropertyInformer")
    private final SharedIndexInformer<NumberProperty> numberPropertyInformer;


    public void addPropertyToGroup(Group group, Property property) {
        Selector selector = Optional.ofNullable(group).map(Group::getSpec).map(GroupSpec::getSelector).orElse(new Selector() {
            public boolean select(Property property) {
                return false;
            }
        });

        if (selector.select(property)) {
            GroupStatus status = Optional.ofNullable(group).map(Group::getStatus).orElse(new GroupStatus());
            group.setStatus(status);

            Set<PropertySelector> members = Optional.ofNullable(status.getMembers()).orElse(new HashSet<>());
            status.setMembers(members);

            if (members.add(PropertySelector.of(property))){
                groupClient.updateStatus(group, (g) -> g.getStatus());
                logger.debug("Added property {} to group {}", property.getMetadata().getName(), group.getMetadata().getName());
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
    }

}
