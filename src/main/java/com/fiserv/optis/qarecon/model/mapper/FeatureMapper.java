package com.fiserv.optis.qarecon.model.mapper;

import com.fiserv.optis.qarecon.model.FeatureDto;
import com.fiserv.optis.qarecon.model.ScenarioDto;
import com.fiserv.optis.qarecon.model.entities.FeatureEntity;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FeatureMapper {

    public static FeatureDto toDto(FeatureEntity entity){
        FeatureDto dto = new FeatureDto();
        dto.setFeatureId(entity.getFeatureId());
        dto.setFeatureName(entity.getFeatureName());
        if (entity.getGherkin()!=null){
            FeatureDto.GherkinDto g = new FeatureDto.GherkinDto();
            g.setBackground(entity.getGherkin().getBackground());
            if (entity.getGherkin().getScenarios()!=null){
                List<ScenarioDto> scenarios = entity.getGherkin().getScenarios().stream().map(s -> {
                    ScenarioDto sd = new ScenarioDto();
                    sd.setScenarioId(s.getScenarioId());
                    sd.setScenarioName(s.getScenarioName());
                    sd.setSteps(s.getSteps());
                    return sd;
                }).collect(Collectors.toList());
                g.setScenarios(scenarios);
            }
            dto.setGherkin(g);
        }
        return dto;
    }

    public static FeatureEntity toEntity(FeatureDto dto){
        FeatureEntity e = new FeatureEntity();
        e.setFeatureId(dto.getFeatureId());
        e.setFeatureName(dto.getFeatureName());
        FeatureEntity.Gherkin g = new FeatureEntity.Gherkin();
        if (dto.getGherkin()!=null){
            g.setBackground(dto.getGherkin().getBackground());
            if (dto.getGherkin().getScenarios()!=null){
                List<FeatureEntity.Scenario> scenarios = dto.getGherkin().getScenarios().stream().map(sdto -> {
                    FeatureEntity.Scenario s = new FeatureEntity.Scenario();
                    s.setScenarioId(sdto.getScenarioId()!=null? sdto.getScenarioId() : UUID.randomUUID().toString());
                    s.setScenarioName(sdto.getScenarioName());
                    s.setSteps(sdto.getSteps());
                    return s;
                }).collect(Collectors.toList());
                g.setScenarios(scenarios);
            }
        }
        e.setGherkin(g);
        return e;
    }
}
