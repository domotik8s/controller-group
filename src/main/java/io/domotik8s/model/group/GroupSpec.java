package io.domotik8s.model.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSpec {

    private Aggregation aggregation;

    private Selector selector;

}
