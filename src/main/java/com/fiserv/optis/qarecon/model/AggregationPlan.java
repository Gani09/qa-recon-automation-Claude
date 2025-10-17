package com.fiserv.optis.qarecon.model;

import java.util.List;

public final class AggregationPlan {

    public List<String> groupBy;
    public List<Op> aggregations;

    // Default constructor
    public AggregationPlan() {}

    // Full constructor
    public AggregationPlan(List<String> groupBy, List<Op> aggregations) {
        this.groupBy = groupBy;
        this.aggregations = aggregations;
    }

    public static class Op {
        public String name;
        public String field;
        public String op; // sum|min|max|count|first|last

        // Default constructor
        public Op() {}

        // Full constructor
        public Op(String name, String field, String op) {
            this.name = name;
            this.field = field;
            this.op = op;
        }
    }
}