package io.domotik8s.model.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertySelector {

    private String apiVersion;

    private String kind;

    private String namespace;

    private String name;

}
