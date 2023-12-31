package io.domotik8s.groupcontroller.listener;

import io.domotik8s.groupcontroller.GroupService;
import io.domotik8s.model.group.Group;
import io.domotik8s.model.num.NumberProperty;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NumberPropertyListener implements ResourceEventHandler<NumberProperty> {


    private Logger logger = LoggerFactory.getLogger(NumberPropertyListener.class);

    @Qualifier("numberPropertyInformer")
    private final SharedIndexInformer<NumberProperty> numberPropertyInformer;

    @Qualifier("groupInformer")
    private final SharedIndexInformer<Group> groupInformer;

    private final GroupService groupService;


    @PostConstruct
    private void register() {
        numberPropertyInformer.addEventHandler(this);
    }


    @Override
    public void onAdd(NumberProperty property) {
        List<Group> allGroups = groupInformer.getIndexer().list();
        allGroups.forEach(group -> groupService.addOrRemovePropertyToGroup(group, property));
    }

    @Override
    public void onUpdate(NumberProperty before, NumberProperty after) {
        List<Group> allGroups = groupInformer.getIndexer().list();
        allGroups.forEach(group -> groupService.addOrRemovePropertyToGroup(group, after));
    }

    @Override
    public void onDelete(NumberProperty property, boolean b) {
        List<Group> allGroups = groupInformer.getIndexer().list();
        allGroups.forEach(group -> groupService.removePropertyFromGroup(group, property));
    }

}
