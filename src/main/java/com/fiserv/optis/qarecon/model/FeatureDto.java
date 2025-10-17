package com.fiserv.optis.qarecon.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.*;

public class FeatureDto {

    private String featureId;

    @NotBlank
    private String featureName;

    private GherkinDto gherkin;

    // Default constructor
    public FeatureDto() {}

    // Full constructor
    public FeatureDto(String featureId, String featureName, GherkinDto gherkin) {
        this.featureId = featureId;
        this.featureName = featureName;
        this.gherkin = gherkin;
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

    public GherkinDto getGherkin() {
        return gherkin;
    }

    public void setGherkin(GherkinDto gherkin) {
        this.gherkin = gherkin;
    }

    // Inner class GherkinDto
    public static class GherkinDto {
        private String background;

        @NotNull
        private List<ScenarioDto> scenarios = new ArrayList<>();

        // Getters and setters
        public String getBackground() {
            return background;
        }

        public void setBackground(String background) {
            this.background = background;
        }

        public List<ScenarioDto> getScenarios() {
            return scenarios;
        }

        public void setScenarios(List<ScenarioDto> scenarios) {
            this.scenarios = scenarios;
        }
    }
}