package io.domotik8s.model.group;

import io.domotik8s.model.Property;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertySelector {

    public static PropertySelector of(Property property) {
        String apiVersion = Optional.ofNullable(property).map(Property::getApiVersion).orElse(null);
        String kind = Optional.ofNullable(property).map(Property::getKind).orElse(null);
        String namespace = Optional.ofNullable(property).map(Property::getMetadata).map(V1ObjectMeta::getNamespace).orElse(null);
        String name = Optional.ofNullable(property).map(Property::getMetadata).map(V1ObjectMeta::getName).orElse(null);

        return PropertySelector.builder()
                .apiVersion(apiVersion)
                .kind(kind)
                .namespace(namespace)
                .name(name)
                .build();
    }


    private String apiVersion;

    private String kind;

    private String namespace;

    private String name;

}
