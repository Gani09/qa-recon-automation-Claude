package com.fiserv.optis.qarecon.util;

import com.fiserv.optis.qarecon.model.AggregationStrategy;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for performing various aggregation operations on document fields
 */
public class AggregationUtil {

    /**
     * Aggregate balance fields from a list of documents based on strategy
     */
    public static BigDecimal aggregateBalances(
            List<Document> documents,
            List<String> balanceFields,
            AggregationStrategy strategy) {

        if (documents == null || documents.isEmpty() || balanceFields == null || balanceFields.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Collect all balance values from all documents
        List<BigDecimal> allValues = documents.stream()
                .flatMap(doc -> balanceFields.stream()
                        .map(field -> getNestedValue(doc, field))
                        .map(AggregationUtil::toBigDecimal)
                        .filter(val -> val != null))
                .collect(Collectors.toList());

        if (allValues.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return applyStrategy(allValues, strategy);
    }

    /**
     * Apply aggregation strategy to a list of values
     */
    private static BigDecimal applyStrategy(List<BigDecimal> values, AggregationStrategy strategy) {
        switch (strategy) {
            case SUM:
                return values.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

            case FIRST:
                return values.get(0);

            case LAST:
                return values.get(values.size() - 1);

            case MIN:
                return values.stream()
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

            case MAX:
                return values.stream()
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

            case AVG:
                BigDecimal sum = values.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return sum.divide(
                        BigDecimal.valueOf(values.size()),
                        10,
                        RoundingMode.HALF_UP
                );

            case COUNT:
                return BigDecimal.valueOf(values.size());

            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * Get nested value from document using dot notation
     */
    private static Object getNestedValue(Document doc, String fieldPath) {
        Object value = doc;
        for (String key : fieldPath.split("\\.")) {
            if (value instanceof Document) {
                value = ((Document) value).get(key);
            } else {
                return null;
            }
        }
        return value;
    }

    /**
     * Convert object to BigDecimal
     */
    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(((Number) v).toString());
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Build grouping key from document based on group-by fields
     */
    public static String buildGroupKey(Document doc, List<String> groupByFields) {
        if (groupByFields == null || groupByFields.isEmpty()) {
            return "DEFAULT_GROUP";
        }

        return groupByFields.stream()
                .map(field -> {
                    Object value = getNestedValue(doc, field);
                    return value != null ? String.valueOf(value) : "null";
                })
                .collect(Collectors.joining("|"));
    }
}