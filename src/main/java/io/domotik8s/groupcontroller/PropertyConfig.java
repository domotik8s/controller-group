package io.domotik8s.groupcontroller;

import io.domotik8s.model.bool.BooleanProperty;
import io.domotik8s.model.bool.BooleanPropertyList;
import io.domotik8s.model.num.NumberProperty;
import io.domotik8s.model.num.NumberPropertyList;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class PropertyConfig {

    @Bean("booleanPropertyClient")
    public GenericKubernetesApi<BooleanProperty, BooleanPropertyList> booleanPropertyClient(ApiClient client) {
        return new GenericKubernetesApi(
                BooleanProperty.class, BooleanPropertyList.class,
                "domotik8s.io", "v1beta1", "booleanproperties",
                client
        );
    }

    @Bean("booleanPropertyInformer")
    public SharedIndexInformer<BooleanProperty> booleanPropertyInformer(
            SharedInformerFactory informerFactory,
            @Qualifier("booleanPropertyClient") GenericKubernetesApi<BooleanProperty, BooleanPropertyList> booleanPropertyClient
    ) {
        return informerFactory.sharedIndexInformerFor(booleanPropertyClient, BooleanProperty.class, 0);
    }


    @Bean("numberPropertyClient")
    public GenericKubernetesApi<NumberProperty, NumberPropertyList> numberPropertyClient(ApiClient client) {
        return new GenericKubernetesApi(
                NumberProperty.class, NumberPropertyList.class,
                "domotik8s.io", "v1beta1", "numberproperties",
                client
        );
    }

    @Bean("numberPropertyInformer")
    public SharedIndexInformer<NumberProperty> numberPropertyInformer(
            SharedInformerFactory informerFactory,
            @Qualifier("numberPropertyClient") GenericKubernetesApi<NumberProperty, NumberPropertyList> numberPropertyClient
    ) {
        return informerFactory.sharedIndexInformerFor(numberPropertyClient, NumberProperty.class, 0);
    }

}
