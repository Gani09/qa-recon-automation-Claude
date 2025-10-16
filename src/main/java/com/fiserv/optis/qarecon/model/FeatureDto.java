package com.fiserv.optis.qarecon.model;

import java.util.List;

public class FeatureDto {
    private String featureId;
    private String featureName;
    private GherkinDto gherkin;

    public String getFeatureId() { return featureId; }
    public void setFeatureId(String featureId) { this.featureId = featureId; }
    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }
    public GherkinDto getGherkin() { return gherkin; }
    public void setGherkin(GherkinDto gherkin) { this.gherkin = gherkin; }

    public static class GherkinDto {
        private String background;
        private List<ScenarioDto> scenarios;

        public String getBackground() { return background; }
        public void setBackground(String background) { this.background = background; }
        public List<ScenarioDto> getScenarios() { return scenarios; }
        public void setScenarios(List<ScenarioDto> scenarios) { this.scenarios = scenarios; }
    }
}
