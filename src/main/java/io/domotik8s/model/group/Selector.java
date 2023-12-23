package io.domotik8s.model.group;

import io.domotik8s.groupcontroller.NumberPropertyListener;
import io.domotik8s.model.Property;
import io.domotik8s.model.PropertyState;
import io.domotik8s.model.PropertyStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Array;
import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@SuperBuilder
public class Selector {

    private Logger logger = LoggerFactory.getLogger(Selector.class);


    public static enum Type {
        LabelSelector, ValueSelector, CompositeSelector
    }

    public static enum Logic {
        And, Or
    }


    private Type type;

    private String name;

    private String value;

    private Logic logic;

    private Set<Selector> children;


    public boolean select(Property property) {
        if (Type.LabelSelector == type)
            return selectLabel(property);
        else if (Type.ValueSelector == type)
            return selectValue(property);
        else if (Type.CompositeSelector == type)
            return selectComposite(property);
        return false;
    }

    private boolean selectLabel(Property property) {
        Map<String, String> labels = Optional.ofNullable(property)
                .map(Property::getMetadata)
                .map(V1ObjectMeta::getLabels)
                .orElse(Map.of());
        boolean retVal = labels.get(name) != null && labels.get(name).equals(value);
        logger.trace("Label selector result for property {}: {}", property.getMetadata().getName(), retVal);
        return retVal;
    }

    private boolean selectValue(Property property) {
        Optional<Object> valueOpt = Optional.ofNullable(property)
                .map(Property::getStatus)
                .map(PropertyStatus::getState)
                .map(PropertyState::getValue);
        boolean retVal = value.equals(valueOpt.get());
        logger.trace("Value selector result for property {}: {}", property.getMetadata().getName(), retVal);
        return retVal;
    }

    private boolean selectComposite(Property property) {
        List<Boolean> results = Optional.ofNullable(this.children)
                .orElse(Set.of()).stream()
                .map(c -> c.select(property))
                .collect(Collectors.toList());
        boolean retVal = false;
        if (this.logic == null || this.logic == Logic.And) {
            long trueCount = results.stream().filter(v -> v == true).count();
            retVal = trueCount == results.size();
        } else if (this.logic == Logic.Or) {
            retVal = results.contains(true);
        }
        logger.trace("Composite selector result for property {}: {}", property.getMetadata().getName(), retVal);
        return retVal;
    }

}
