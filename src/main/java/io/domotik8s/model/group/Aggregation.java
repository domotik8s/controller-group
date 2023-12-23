package io.domotik8s.model.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aggregation {

    public enum Method {
        Sum, ArithmeticMean
    }

    private Method method;


    public Object aggregate(List<Object> values) {
        if (method == Method.Sum)
            return sum(values);
        else if (method == Method.ArithmeticMean)
            return aMean(values);
        return null;
    }

    private Double sum(List<Object> values) {
        List<Double> numbers = values.stream()
                .filter(v -> v instanceof Number)
                .map(v -> ((Number) v).doubleValue())
                .collect(Collectors.toList());
        return numbers.stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private Double aMean(List<Object> values) {
        List<Double> numbers = values.stream()
                .filter(v -> v instanceof Number)
                .map(v -> ((Number) v).doubleValue())
                .collect(Collectors.toList());
        double sum = sum(values);
        long count = numbers.size();
        return count > 0 ? sum / count : 0;
    }

}
