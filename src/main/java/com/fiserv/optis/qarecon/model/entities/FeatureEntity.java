package com.fiserv.optis.qarecon.model.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "QA_Automation_Features")
public class FeatureEntity {
    @Id private String featureId;
    @Indexed(unique = true) private String featureName;
    private Gherkin gherkin;

    public String getFeatureId(){ return featureId; }
    public void setFeatureId(String featureId){ this.featureId = featureId; }
    public String getFeatureName(){ return featureName; }
    public void setFeatureName(String featureName){ this.featureName = featureName; }
    public Gherkin getGherkin(){ return gherkin; }
    public void setGherkin(Gherkin gherkin){ this.gherkin = gherkin; }

    public static class Gherkin {
        private String background;
        private List<Scenario> scenarios;
        public String getBackground(){ return background; }
        public void setBackground(String b){ this.background=b; }
        public List<Scenario> getScenarios(){ return scenarios; }
        public void setScenarios(List<Scenario> s){ this.scenarios=s; }
    }
    public static class Scenario {
        private String scenarioId;
        private String scenarioName;
        private java.util.List<String> steps;
        public String getScenarioId(){ return scenarioId; }
        public void setScenarioId(String id){ this.scenarioId=id; }
        public String getScenarioName(){ return scenarioName; }
        public void setScenarioName(String n){ this.scenarioName=n; }
        public java.util.List<String> getSteps(){ return steps; }
        public void setSteps(java.util.List<String> s){ this.steps=s; }
    }
}
