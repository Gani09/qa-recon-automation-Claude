package com.fiserv.optis.qarecon.model;

import java.util.List;

public class BalanceFieldConfig {

    private String collection; // "source" or "target"
    private String fieldName;
    private AggregationStrategy aggregationStrategy;
    private List<String> groupByFields;

    public BalanceFieldConfig() {}

    public BalanceFieldConfig(String collection, String fieldName,
                              AggregationStrategy aggregationStrategy,
                              List<String> groupByFields) {
        this.collection = collection;
        this.fieldName = fieldName;
        this.aggregationStrategy = aggregationStrategy;
        this.groupByFields = groupByFields;
    }

    // Getters and setters
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

    public List<String> getGroupByFields() {
        return groupByFields;
    }

    public void setGroupByFields(List<String> groupByFields) {
        this.groupByFields = groupByFields;
    }
}