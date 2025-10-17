package com.fiserv.optis.qarecon.model.mapper;

import com.fiserv.optis.qarecon.model.FeatureDto;
import com.fiserv.optis.qarecon.model.ScenarioDto;
import com.fiserv.optis.qarecon.model.entities.FeatureEntity;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FeatureMapper {

    public static FeatureDto toDto(FeatureEntity entity) {
        FeatureDto dto = new FeatureDto();
        dto.setFeatureId(entity.getFeatureId());
        dto.setFeatureName(entity.getFeatureName());

        if (entity.getGherkin() != null) {
            FeatureDto.GherkinDto gherkinDto = new FeatureDto.GherkinDto();
            gherkinDto.setBackground(entity.getGherkin().getBackground());

            if (entity.getGherkin().getScenarios() != null) {
                List<ScenarioDto> scenarios = entity.getGherkin().getScenarios().stream()
                        .map(s -> {
                            ScenarioDto sdto = new ScenarioDto();
                            sdto.setScenarioId(s.getScenarioId());
                            sdto.setScenarioName(s.getScenarioName());
                            sdto.setSteps(s.getSteps());
                            return sdto;
                        })
                        .collect(Collectors.toList());
                gherkinDto.setScenarios(scenarios);
            }

            dto.setGherkin(gherkinDto);
        }

        return dto;
    }

    public static FeatureEntity toEntity(FeatureDto dto) {
        FeatureEntity entity = new FeatureEntity();
        entity.setFeatureId(dto.getFeatureId());
        entity.setFeatureName(dto.getFeatureName());

        FeatureEntity.Gherkin gherkin = new FeatureEntity.Gherkin();
        if (dto.getGherkin() != null) {
            gherkin.setBackground(dto.getGherkin().getBackground());

            if (dto.getGherkin().getScenarios() != null) {
                List<FeatureEntity.Scenario> scenarios = dto.getGherkin().getScenarios().stream()
                        .map(sdto -> {
                            FeatureEntity.Scenario scenario = new FeatureEntity.Scenario();
                            scenario.setScenarioId(
                                    sdto.getScenarioId() != null ? sdto.getScenarioId() : UUID.randomUUID().toString()
                            );
                            scenario.setScenarioName(sdto.getScenarioName());
                            scenario.setSteps(sdto.getSteps());
                            return scenario;
                        })
                        .collect(Collectors.toList());
                gherkin.setScenarios(scenarios);
            }

            entity.setGherkin(gherkin);
        }

        return entity;
    }
}