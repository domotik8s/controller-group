package io.domotik8s.groupcontroller;

import io.domotik8s.model.Property;
import io.domotik8s.model.group.*;
import io.domotik8s.model.num.NumberProperty;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NumberPropertyListener implements ResourceEventHandler<NumberProperty> {


    private Logger logger = LoggerFactory.getLogger(NumberPropertyListener.class);

    @Qualifier("numberPropertyInformer")
    private final SharedIndexInformer<NumberProperty> numberPropertyInformer;

    @Qualifier("groupInformer")
    private final SharedIndexInformer<Group> groupInformer;

    @Qualifier("groupClient")
    private final GenericKubernetesApi<Group, GroupList> groupClient;



    @PostConstruct
    private void register() {
        numberPropertyInformer.addEventHandler(this);
    }


    @Override
    public void onAdd(NumberProperty property) {
        add(property);
    }

    @Override
    public void onUpdate(NumberProperty before, NumberProperty after) {
        add(after);
    }

    @Override
    public void onDelete(NumberProperty property, boolean b) {
        delete(property);
    }


    private void add(NumberProperty property) {
        List<Group> allGroups = groupInformer.getIndexer().list();
        allGroups.forEach(group -> {
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

                String apiVersion = Optional.ofNullable(property).map(Property::getApiVersion).orElse(null);
                String kind = Optional.ofNullable(property).map(Property::getKind).orElse(null);
                String namespace = Optional.ofNullable(property).map(Property::getMetadata).map(V1ObjectMeta::getNamespace).orElse(null);
                String name = Optional.ofNullable(property).map(Property::getMetadata).map(V1ObjectMeta::getName).orElse(null);

                if (members.add(PropertySelector.builder()
                        .apiVersion(apiVersion)
                        .kind(kind)
                        .namespace(namespace)
                        .name(name)
                        .build())){
                    groupClient.updateStatus(group, (g) -> g.getStatus());
                    logger.debug("Added property {} to group {}", property.getMetadata().getName(), group.getMetadata().getName());
                }
            }
        });
    }

    private void delete(NumberProperty property) {
        String apiVersion = Optional.ofNullable(property).map(Property::getApiVersion).orElse(null);
        String kind = Optional.ofNullable(property).map(Property::getKind).orElse(null);
        String namespace = Optional.ofNullable(property).map(Property::getMetadata).map(V1ObjectMeta::getNamespace).orElse(null);
        String name = Optional.ofNullable(property).map(Property::getMetadata).map(V1ObjectMeta::getName).orElse(null);

        PropertySelector selector = PropertySelector.builder()
                .apiVersion(apiVersion)
                .kind(kind)
                .namespace(namespace)
                .name(name)
                .build();

        List<Group> allGroups = groupInformer.getIndexer().list();
        allGroups.forEach(group -> {
            Optional<Set<PropertySelector>> membersOpt = Optional.ofNullable(group)
                    .map(Group::getStatus)
                    .map(GroupStatus::getMembers);
            if (membersOpt.isPresent() && membersOpt.get().remove(selector)) {
                groupClient.updateStatus(group, (g) -> g.getStatus());
                logger.debug("Removed property {} from group {}", property.getMetadata().getName(), group.getMetadata().getName());
            }
        });
    }

}
