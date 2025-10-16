package com.fiserv.optis.qarecon.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

public final class FieldMapping {
    private String leftField;
    private String rightField;
    private String compareAs;
    private String tolerance;

    public FieldMapping(){}
    public FieldMapping(String leftField, String rightField, String compareAs, String tolerance){
        this.leftField = leftField; this.rightField = rightField;
        this.compareAs = compareAs == null ? "string" : compareAs;
        this.tolerance = tolerance;
    }

    public String getLeftField(){ return leftField; }
    public void setLeftField(String s){ this.leftField = s; }
    public String getRightField(){ return rightField; }
    public void setRightField(String s){ this.rightField = s; }
    public String getCompareAs(){ return compareAs; }
    public void setCompareAs(String s){ this.compareAs = s; }
    public String getTolerance(){ return tolerance; }
    public void setTolerance(String s){ this.tolerance = s; }

    public static FieldMapping from(Map<String,String> row){
        return new FieldMapping(val(row,"left_field"), val(row,"right_field"), val(row,"compare_as"), val(row,"tolerance"));
    }

    public BigDecimal numericToleranceOr(BigDecimal def){ return (tolerance==null || tolerance.isBlank()) ? def : new BigDecimal(tolerance); }
    public Duration timeToleranceOr(Duration def){ return (tolerance==null || tolerance.isBlank()) ? def : Duration.parse(tolerance); }

    private static String val(Map<String,String> m, String k){ String v = m.get(k); return v==null ? null : v.trim(); }
}
