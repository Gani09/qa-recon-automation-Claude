package com.fiserv.optis.qarecon.model;

import java.util.List;

public class ScenarioDto {

    private String scenarioId;
    private String scenarioName;
    private List<String> steps;

    // Default constructor
    public ScenarioDto() {}

    // Full constructor
    public ScenarioDto(String scenarioId, String scenarioName, List<String> steps) {
        this.scenarioId = scenarioId;
        this.scenarioName = scenarioName;
        this.steps = steps;
    }

    // Getters and setters
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