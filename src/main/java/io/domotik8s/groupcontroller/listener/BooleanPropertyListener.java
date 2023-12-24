package io.domotik8s.groupcontroller.listener;

import io.domotik8s.groupcontroller.GroupService;
import io.domotik8s.model.bool.BooleanProperty;
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
public class BooleanPropertyListener implements ResourceEventHandler<BooleanProperty> {


    private Logger logger = LoggerFactory.getLogger(BooleanPropertyListener.class);

    @Qualifier("booleanPropertyInformer")
    private final SharedIndexInformer<BooleanProperty> booleanPropertyInformer;

    @Qualifier("groupInformer")
    private final SharedIndexInformer<Group> groupInformer;

    private final GroupService groupService;


    @PostConstruct
    private void register() {
        booleanPropertyInformer.addEventHandler(this);
    }


    @Override
    public void onAdd(BooleanProperty property) {
        List<Group> allGroups = groupInformer.getIndexer().list();
        allGroups.forEach(group -> groupService.addPropertyToGroup(group, property));
    }

    @Override
    public void onUpdate(BooleanProperty before, BooleanProperty after) {
        List<Group> allGroups = groupInformer.getIndexer().list();
        allGroups.forEach(group -> groupService.addPropertyToGroup(group, after));
    }

    @Override
    public void onDelete(BooleanProperty property, boolean b) {
        List<Group> allGroups = groupInformer.getIndexer().list();
        allGroups.forEach(group -> groupService.removePropertyFromGroup(group, property));
    }

}
