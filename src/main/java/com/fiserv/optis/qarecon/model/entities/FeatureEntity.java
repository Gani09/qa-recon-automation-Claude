package com.fiserv.optis.qarecon.model.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "QA_Automation_Features")
public class FeatureEntity {

    @Id
    private String featureId;

    @Indexed(unique = true)
    private String featureName;

    private Gherkin gherkin;

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

    public Gherkin getGherkin() {
        return gherkin;
    }

    public void setGherkin(Gherkin gherkin) {
        this.gherkin = gherkin;
    }

    // Inner class for Gherkin
    public static class Gherkin {
        private String background;
        private List<Scenario> scenarios;

        // Getters and setters
        // Getters and setters for Gherkin
        public String getBackground() {
            return background;
        }

        public void setBackground(String background) {
            this.background = background;
        }

        public List<Scenario> getScenarios() {
            return scenarios;
        }

        public void setScenarios(List<Scenario> scenarios) {
            this.scenarios = scenarios;
        }
    }

    // Inner class for Scenario
    public static class Scenario {
        private String scenarioId;
        private String scenarioName;
        private List<String> steps;

        // Getters and setters for Scenario
        public String getScenarioId() {
            return scenarioId;
        }

        public void setScenarioId(String scenarioId) {
            this.scenarioId = scenarioId;
        }

        public String getScenarioName() {
            return scenarioName;
        }

        public void setScenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
        }

        public List<String> getSteps() {
            return steps;
        }

        public void setSteps(List<String> steps) {
            this.steps = steps;
        }
    }
}