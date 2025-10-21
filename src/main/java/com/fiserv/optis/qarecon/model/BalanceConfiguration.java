package com.fiserv.optis.qarecon.model;

import java.util.List;

/**
 * Enhanced balance configuration that separates grouping from field definitions
 */
public class BalanceConfiguration {

    private BalanceGrouping sourceGrouping;
    private BalanceGrouping targetGrouping;
    private List<BalanceField> balanceFields;

    public BalanceConfiguration() {}

    public BalanceConfiguration(BalanceGrouping sourceGrouping,
                                BalanceGrouping targetGrouping,
                                List<BalanceField> balanceFields) {
        this.sourceGrouping = sourceGrouping;
        this.targetGrouping = targetGrouping;
        this.balanceFields = balanceFields;
    }

    // Getters and setters
    public BalanceGrouping getSourceGrouping() {
        return sourceGrouping;
    }

    public void setSourceGrouping(BalanceGrouping sourceGrouping) {
        this.sourceGrouping = sourceGrouping;
    }

    public BalanceGrouping getTargetGrouping() {
        return targetGrouping;
    }

    public void setTargetGrouping(BalanceGrouping targetGrouping) {
        this.targetGrouping = targetGrouping;
    }

    public List<BalanceField> getBalanceFields() {
        return balanceFields;
    }

    public void setBalanceFields(List<BalanceField> balanceFields) {
        this.balanceFields = balanceFields;
    }

    /**
     * Grouping configuration for source or target
     */
    public static class BalanceGrouping {
        private String collection;  // "source" or "target"
        private List<String> groupByFields;

        public BalanceGrouping() {}

        public BalanceGrouping(String collection, List<String> groupByFields) {
            this.collection = collection;
            this.groupByFields = groupByFields;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public List<String> getGroupByFields() {
            return groupByFields;
        }

        public void setGroupByFields(List<String> groupByFields) {
            this.groupByFields = groupByFields;
        }
    }

    /**
     * Individual balance field without grouping info
     */
    public static class BalanceField {
        private String collection;  // "source" or "target"
        private String fieldName;
        private AggregationStrategy aggregationStrategy;

        public BalanceField() {}

        public BalanceField(String collection, String fieldName, AggregationStrategy strategy) {
            this.collection = collection;
            this.fieldName = fieldName;
            this.aggregationStrategy = strategy;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public AggregationStrategy getAggregationStrategy() {
            return aggregationStrategy;
        }

        public void setAggregationStrategy(AggregationStrategy aggregationStrategy) {
            this.aggregationStrategy = aggregationStrategy;
        }
    }

    /**
     * Helper method to get grouping for a specific collection
     */
    public List<String> getGroupByFieldsForCollection(String collection) {
        if ("source".equalsIgnoreCase(collection)) {
            return sourceGrouping != null ? sourceGrouping.getGroupByFields() : null;
        } else if ("target".equalsIgnoreCase(collection)) {
            return targetGrouping != null ? targetGrouping.getGroupByFields() : null;
        }
        return null;
    }

    /**
     * Helper method to get all balance fields for a specific collection
     */
    public List<BalanceField> getBalanceFieldsForCollection(String collection) {
        return balanceFields.stream()
                .filter(field -> collection.equalsIgnoreCase(field.getCollection()))
                .toList();
    }
}