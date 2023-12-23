package io.domotik8s.model.group;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group implements KubernetesObject {

    private final String apiVersion = "domotik8s.io/v1beta1";

    private final String kind = "Group";

    private V1ObjectMeta metadata;

    private GroupSpec spec;

    private GroupStatus status;

}
