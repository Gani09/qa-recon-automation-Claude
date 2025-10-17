package com.fiserv.optis.qarecon.model;

public enum AggregationStrategy {
    SUM,        // Sum all values
    FIRST,      // Take first value encountered
    LAST,       // Take last value encountered
    MIN,        // Minimum value
    MAX,        // Maximum value
    AVG,        // Average value
    COUNT       // Count of records
}