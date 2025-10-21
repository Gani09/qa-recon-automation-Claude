package com.fiserv.optis.qarecon.model;

public class ReconConfigSummaryDto {

    private String configId;
    private String configName;

    // Default constructor
    public ReconConfigSummaryDto() {}

    // Full constructor
    public ReconConfigSummaryDto(String configId, String configName) {
        this.configId = configId;
        this.configName = configName;
    }

    // Getters and setters
    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }
}