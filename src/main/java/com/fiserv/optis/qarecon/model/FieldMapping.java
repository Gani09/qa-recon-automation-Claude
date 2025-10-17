package com.fiserv.optis.qarecon.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

public final class FieldMapping {

    private String leftField;
    private String rightField;
    private String compareAs;
    private String tolerance; // numeric tolerance or ISO-8601 duration for datetime

    public BigDecimal numericTolerance;
    public Duration timeTolerance;

    // Default constructor for Jackson
    public FieldMapping() {}

    // Full constructor
    public FieldMapping(String leftField, String rightField, String compareAs, String tolerance) {
        this.leftField = leftField;
        this.rightField = rightField;
        this.compareAs = compareAs == null ? "string" : compareAs;
        this.tolerance = tolerance;
    }

    public static FieldMapping from(Map<String, String> row) {
        return new FieldMapping(
                val(row, "left_field"),
                val(row, "right_field"),
                val(row, "compare_as"),
                val(row, "tolerance")
        );
    }

    public BigDecimal numericToleranceOr(BigDecimal def) {
        if (tolerance == null || tolerance.isBlank()) return def;
        return new BigDecimal(tolerance);
    }

    public Duration timeToleranceOr(Duration def) {
        if (tolerance == null || tolerance.isBlank()) return def;
        return Duration.parse(tolerance);
    }

    private static String val(Map<String, String> m, String k) {
        String v = m.get(k);
        return v == null ? null : v.trim();
    }

    // Getters and setters
    public String getLeftField() {
        return leftField;
    }

    public void setLeftField(String leftField) {
        this.leftField = leftField;
    }

    public String getRightField() {
        return rightField;
    }

    public void setRightField(String rightField) {
        this.rightField = rightField;
    }

    public String getCompareAs() {
        return compareAs;
    }

    public void setCompareAs(String compareAs) {
        this.compareAs = compareAs;
    }

    public String getTolerance() {
        return tolerance;
    }

    public void setTolerance(String tolerance) {
        this.tolerance = tolerance;
    }
}