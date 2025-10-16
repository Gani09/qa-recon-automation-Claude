package com.fiserv.optis.qarecon.model;

public class FeatureSummaryDto {
    private String featureId;
    private String featureName;

    public FeatureSummaryDto(){}
    public FeatureSummaryDto(String featureId, String featureName){
        this.featureId = featureId; this.featureName = featureName;
    }
    public String getFeatureId(){ return featureId; }
    public void setFeatureId(String id){ this.featureId = id; }
    public String getFeatureName(){ return featureName; }
    public void setFeatureName(String n){ this.featureName = n; }
}
