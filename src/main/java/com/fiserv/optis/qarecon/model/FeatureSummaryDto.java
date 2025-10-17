package com.fiserv.optis.qarecon.model;

public class FeatureSummaryDto {

    private String featureId;
    private String featureName;

    // Default constructor
    public FeatureSummaryDto() {}

    // Full constructor
    public FeatureSummaryDto(String featureId, String featureName) {
        this.featureId = featureId;
        this.featureName = featureName;
    }

    // Getters and setters
    public String getFeatureId() {
        return featureId;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }
}