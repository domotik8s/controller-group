package io.domotik8s.model.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aggregation {

    public enum Method {
        Min, Max, Sum, ArithmeticMean
    }

    private Method method;


    public Object aggregate(List<Object> values) {
        if (method == Method.Min)
            return min(values);
        else if (method == Method.Max)
            return max(values);
        else if (method == Method.Sum)
            return sum(values);
        else if (method == Method.ArithmeticMean)
            return aMean(values);
        return null;
    }

    private List<Double> filterNumbers(List<Object> values) {
        return values.stream()
                .filter(v -> v instanceof Number)
                .map(v -> ((Number) v).doubleValue())
                .collect(Collectors.toList());
    }

    private Double min(List<Object> values) {
        return filterNumbers(values).stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(Double.NaN);
    }

    private Double max(List<Object> values) {
        return filterNumbers(values).stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(Double.NaN);
    }

    private Double sum(List<Object> values) {
        return filterNumbers(values).stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private Double aMean(List<Object> values) {
        List<Double> numbers = filterNumbers(values);
        double sum = filterNumbers(values).stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        long count = numbers.size();
        return count > 0 ? sum / count : 0;
    }

}
