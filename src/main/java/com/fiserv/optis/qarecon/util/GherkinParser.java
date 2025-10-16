package com.fiserv.optis.qarecon.util;

import com.fiserv.optis.qarecon.model.FeatureDto;
import com.fiserv.optis.qarecon.model.ScenarioDto;
import java.util.*;

public class GherkinParser {

    public static FeatureDto gherkinToDto(String gherkin) {
        String[] lines = gherkin.split("\\r?\\n");
        String name = null;
        String background = null;
        List<ScenarioDto> scenarios = new ArrayList<>();
        ScenarioDto currentScenario = null;
        List<String> currentSteps = null;
        boolean inBackground = false;

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Feature:")) {
                name = line.substring("Feature:".length()).trim();
            } else if (line.startsWith("Background:")) {
                inBackground = true;
                background = "";
            } else if (line.startsWith("Scenario:")) {
                if (currentScenario != null) {
                    currentScenario.setSteps(currentSteps);
                    scenarios.add(currentScenario);
                }
                currentScenario = new ScenarioDto();
                currentScenario.setScenarioName(line.substring("Scenario:".length()).trim());
                currentSteps = new ArrayList<>();
                inBackground = false;
            } else if (line.startsWith("Given") || line.startsWith("When") ||
                    line.startsWith("Then") || line.startsWith("And") || line.startsWith("But")) {
                if (inBackground) {
                    background = (background == null ? "" : background + "\n") + line;
                } else if (currentSteps != null) {
                    currentSteps.add(line);
                }
            }
        }

        // Add the last scenario if present
        if (currentScenario != null) {
            currentScenario.setSteps(currentSteps);
            scenarios.add(currentScenario);
        }

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Missing or invalid Feature name.");
        }

        if (scenarios.isEmpty()) {
            throw new IllegalArgumentException("No scenarios found in Gherkin.");
        }

        for (ScenarioDto s : scenarios) {
            if (s.getScenarioName() == null || s.getScenarioName().isEmpty()) {
                throw new IllegalArgumentException("Scenario without a name found.");
            }
            if (s.getSteps() == null || s.getSteps().isEmpty()) {
                throw new IllegalArgumentException("Scenario '" + s.getScenarioName() + "' has no steps.");
            }
        }

        FeatureDto.GherkinDto gherkinDto = new FeatureDto.GherkinDto();
        gherkinDto.setBackground(background);
        gherkinDto.setScenarios(scenarios);

        FeatureDto featureDto = new FeatureDto();
        featureDto.setFeatureName(name);
        featureDto.setGherkin(gherkinDto);

        return featureDto;
    }

    // Utility to reconstruct Gherkin text from FeatureDto
    public static String toGherkinString(FeatureDto feature) {
        StringBuilder sb = new StringBuilder();
        sb.append("Feature: ").append(feature.getFeatureName()).append("\n");
        if (feature.getGherkin() != null && feature.getGherkin().getBackground() != null) {
            sb.append(" Background:\n   ").append(feature.getGherkin().getBackground()).append("\n");
        }
        if (feature.getGherkin() != null && feature.getGherkin().getScenarios() != null) {
            for (ScenarioDto scenario : feature.getGherkin().getScenarios()) {
                sb.append(" Scenario: ").append(scenario.getScenarioName()).append("\n");
                if (scenario.getSteps() != null) {
                    for (String step : scenario.getSteps()) {
                        sb.append("   ").append(step).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }
}