package io.domotik8s.model.group;

import io.domotik8s.model.Property;
import io.domotik8s.model.PropertyState;
import io.domotik8s.model.PropertyStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@SuperBuilder
public class Selector {

    private Logger logger = LoggerFactory.getLogger(Selector.class);


    public enum Type {
        LabelSelector, ValueSelector, CompositeSelector
    }


    /*
      SHARED
     */

    private Type type;

    /*
      LabelSelector
     */

    private String name;


    private String labelValue;

    public enum Comparator { Equals, Contains }

    private Comparator comparator;

    private boolean ignoreCase = false;


    /*
      VALUE
     */

    private String value;


    /*
      COMPOSITE
     */

    public enum Logic { And, Or }

    private Logic logic;

    private Set<Selector> children;


    /*
      METHODS
     */

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

        String propertyLabelValue = labels.get(name);
        if (propertyLabelValue == null) return false;

        // default comparator: Equals
        boolean retVal = ignoreCase == true ? propertyLabelValue.equalsIgnoreCase(labelValue) : propertyLabelValue.equals(labelValue);
        if (comparator == Comparator.Contains) {
            if (ignoreCase == true) retVal = propertyLabelValue.toLowerCase().contains(labelValue.toLowerCase());
            else                    retVal = propertyLabelValue.contains(labelValue);
        }

        logger.trace("Label selector result for property {}: {}", property.getMetadata().getName(), retVal);
        return retVal;
    }

    private boolean selectValue(Property property) {
        Optional<Object> valueOpt = Optional.ofNullable(property)
                .map(Property::getStatus)
                .map(PropertyStatus::getState)
                .map(PropertyState::getValue);

        boolean retVal = valueOpt.isPresent() && value.equals(valueOpt.get());
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
