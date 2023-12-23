package io.domotik8s.groupcontroller.config;

import io.domotik8s.groupcontroller.GroupReconciler;
import io.domotik8s.model.group.Group;
import io.domotik8s.model.group.GroupList;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class GroupConfig {

    @Bean("groupClient")
    public GenericKubernetesApi<Group, GroupList> groupClient(ApiClient client) {
        return new GenericKubernetesApi(
                Group.class, GroupList.class,
                "domotik8s.io", "v1beta1", "groups",
                client
        );
    }

    @Bean("groupInformer")
    public SharedIndexInformer<Group> groupInformer(
            SharedInformerFactory informerFactory,
            @Qualifier("groupClient") GenericKubernetesApi<Group, GroupList> groupClient
    ) {
        return informerFactory.sharedIndexInformerFor(groupClient, Group.class, 0);
    }

    @Bean("groupController")
    public Controller groupController(
            SharedInformerFactory informerFactory,
            GroupReconciler reconciler,
            @Qualifier("groupInformer") SharedIndexInformer<Group> groupInformer
    ) {
        return ControllerBuilder
                .defaultBuilder(informerFactory)
                .watch(workQueue -> ControllerBuilder
                        .controllerWatchBuilder(Group.class, workQueue)
                        .withResyncPeriod(Duration.of(1, ChronoUnit.SECONDS))
                        .build())
                .withWorkerCount(1)
                .withReconciler(reconciler)
                .withReadyFunc(groupInformer::hasSynced)
                .withName("GroupController")
                .build();
    }

}
