package com.fiserv.optis.qarecon.model;

import java.util.Map;
import java.util.Objects;

public final class JoinKey {
    public final String sourceField;
    public final String sourceFieldAs;
    public final String targetField;
    public final String targetFieldAs;

    public JoinKey(String sourceField, String sourceFieldAs, String targetField, String targetFieldAs){
        this.sourceField = sourceField; this.sourceFieldAs = sourceFieldAs;
        this.targetField = targetField; this.targetFieldAs = targetFieldAs;
    }

    public static JoinKey of(Map<String,String> row){
        String sf = row.getOrDefault("SourceField", "").trim();
        String sfa = row.getOrDefault("SourceFieldAs", "").trim();
        String tf = row.getOrDefault("TargetField", "").trim();
        String tfa = row.getOrDefault("TargetFieldAs", "").trim();
        return new JoinKey(sf,sfa,tf,tfa);
    }

    @Override public String toString(){ return "Source: "+sourceField+" as "+sourceFieldAs+", Target: "+targetField+" as "+targetFieldAs; }
    @Override public int hashCode(){ return Objects.hash(sourceField, sourceFieldAs, targetField, targetFieldAs); }
    @Override public boolean equals(Object o){
        if(!(o instanceof JoinKey j)) return false;
        return Objects.equals(sourceField,j.sourceField) && Objects.equals(sourceFieldAs,j.sourceFieldAs)
            && Objects.equals(targetField,j.targetField) && Objects.equals(targetFieldAs,j.targetFieldAs);
    }
}
