package com.fiserv.optis.qarecon.controller;

import com.fiserv.optis.qarecon.model.FeatureSummaryDto;
import com.fiserv.optis.qarecon.model.entities.FeatureEntity;
import com.fiserv.optis.qarecon.model.mapper.FeatureMapper;
import com.fiserv.optis.qarecon.model.FeatureDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import com.fiserv.optis.qarecon.service.FeatureService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
import com.fiserv.optis.qarecon.util.GherkinParser;

@RestController
@RequestMapping("/api/feature")
public class FeatureController {

    private final FeatureService featureService;

    public FeatureController(FeatureService featureService) {
        this.featureService = featureService;
    }

    @Operation(
            summary = "Lists all Gherkin Features",
            description = "Returns a list of all Gherkin features with their IDs and names."
    )
    @ApiResponse(responseCode = "200", description = "List of all feature summaries with IDs and names")
    @GetMapping("/listAll")
    public List<FeatureSummaryDto> listAllFeatures() {
        return featureService.getAllFeatures().stream()
                .map( f -> new FeatureSummaryDto(f.getFeatureId(), f.getFeatureName()))
                .toList();
    }

    @Operation(
            summary = "View the feature by ID",
            description = "View a Gherkin feature by its unique ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feature found"),
            @ApiResponse(responseCode = "404", description = "Feature not found")
    })
    @GetMapping("/view/id/{id}")
    public ResponseEntity<FeatureEntity> getFeature(
            @Parameter(description = "The unique feature ID", required = true)
            @PathVariable String id) {
        return featureService.getFeatureById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "View the feature by name",
            description = "Fetches a Gherkin feature by its unique name."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feature found"),
            @ApiResponse(responseCode = "404", description = "Feature not found")
    })
    @GetMapping("/view/name/{featureName}")
    public ResponseEntity<FeatureEntity> getFeatureByName(
            @Parameter(description = "The feature name", required = true)
            @PathVariable String featureName) {
        return featureService.getFeatureByName(featureName)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Create a new Gherkin feature",
            description = "Parses the provided Gherkin text and creates a new feature in the database."
    )
    @ApiResponse(responseCode = "200", description = "Feature created successfully")
    @PostMapping("/create")
    public ResponseEntity<FeatureEntity> createFeature(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Gherkin text representing the feature",
                    required = true
            )
            @RequestBody String gherkinText) {
        FeatureDto dto = GherkinParser.gherkinToDto(gherkinText);
        FeatureEntity entity = FeatureMapper.toEntity(dto);
        FeatureEntity saved = featureService.saveFeature(entity);
        return ResponseEntity.ok(saved);
    }

    @Operation(
            summary = "Update an existing Gherkin feature",
            description = "Updates a Gherkin feature by its ID using the provided Gherkin text."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feature updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid Gherkin syntax"),
            @ApiResponse(responseCode = "404", description = "Feature not found")
    })
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateFeature(
            @Parameter(description = "The unique feature ID", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Gherkin text representing the updated feature",
                    required = true
            )
            @RequestBody String gherkinText) {
        Optional<FeatureEntity> existing = featureService.getFeatureById(id);
        if (existing.isPresent()) {
            try {
                FeatureDto dto = GherkinParser.gherkinToDto(gherkinText);
                FeatureEntity entity = FeatureMapper.toEntity(dto);
                entity.setFeatureId(id);
                FeatureEntity updated = featureService.saveFeature(entity);
                return ResponseEntity.ok(updated);
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body("Invalid Gherkin syntax: " + ex.getMessage());
            }
        } else {
            return ResponseEntity.status(404).body("Feature not found.");
        }
    }

    @Operation(
            summary = "Delete a Gherkin feature by ID",
            description = "Deletes a Gherkin feature from the database using its unique ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feature has been deleted."),
            @ApiResponse(responseCode = "404", description = "Feature not found.")
    })
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFeature(
            @Parameter(description = "The unique feature ID", required = true)
            @PathVariable String id) {
        Optional<FeatureEntity> existing = featureService.getFeatureById(id);
        if (existing.isPresent()) {
            featureService.deleteFeatureById(id);
            return ResponseEntity.ok("Feature has been deleted.");
        } else {
            return ResponseEntity.status(404).body("Feature not found.");
        }
    }
}