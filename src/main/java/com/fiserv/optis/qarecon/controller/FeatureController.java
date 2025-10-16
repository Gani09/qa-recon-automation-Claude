package com.fiserv.optis.qarecon.controller;

import com.fiserv.optis.qarecon.model.FeatureDto;
import com.fiserv.optis.qarecon.model.FeatureSummaryDto;
import com.fiserv.optis.qarecon.model.entities.FeatureEntity;
import com.fiserv.optis.qarecon.model.mapper.FeatureMapper;
import com.fiserv.optis.qarecon.service.FeatureService;
import com.fiserv.optis.qarecon.util.GherkinParser;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/feature")
public class FeatureController {
    private final FeatureService featureService;
    public FeatureController(FeatureService featureService){ this.featureService = featureService; }

    @GetMapping("/listAll")
    public List<FeatureSummaryDto> listAllFeatures(){
        return featureService.getAllFeatures().stream()
                .map(f -> new FeatureSummaryDto(f.getFeatureId(), f.getFeatureName()))
                .toList();
    }

    @GetMapping("/view/id/{id}")
    public ResponseEntity<FeatureEntity> getFeature(@PathVariable String id){
        return featureService.getFeatureById(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/view/name/{featureName}")
    public ResponseEntity<FeatureEntity> getFeatureByName(@PathVariable String featureName){
        return featureService.getFeatureByName(featureName).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public ResponseEntity<FeatureEntity> createFeature(@RequestBody String gherkinText){
        FeatureDto dto = GherkinParser.gherkinToDto(gherkinText);
        FeatureEntity entity = FeatureMapper.toEntity(dto);
        FeatureEntity saved = featureService.saveFeature(entity);
        return ResponseEntity.ok(saved);
    }
}
