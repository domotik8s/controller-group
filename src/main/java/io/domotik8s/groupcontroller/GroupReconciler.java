package io.domotik8s.groupcontroller;

import io.domotik8s.model.group.Group;
import io.domotik8s.model.group.GroupList;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GroupReconciler implements Reconciler {

    private Logger logger = LoggerFactory.getLogger(GroupReconciler.class);

    @Qualifier("groupClient")
    private final GenericKubernetesApi<Group, GroupList> client;

    @Qualifier("groupInformer")
    private final SharedIndexInformer<Group> informer;


    @Override
    public Result reconcile(Request request) {
        String key = createKey(request);
        logger.trace("Handling resource {}", key);
        // Group resource = informer.getIndexer().getByKey(key);
        return new Result(false);
    }


    protected String createKey(Request request) {
        StringBuilder key = new StringBuilder();
        if (request.getNamespace() != null) {
            key.append(request.getNamespace());
            key.append("/");
        }
        key.append(request.getName());
        return key.toString();
    }

}
