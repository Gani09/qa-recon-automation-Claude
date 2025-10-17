package com.fiserv.optis.qarecon.controller;

import com.fiserv.optis.qarecon.constants.ReportsContext;
import com.fiserv.optis.qarecon.model.FeatureDto;
import com.fiserv.optis.qarecon.model.RunRequest;
import com.fiserv.optis.qarecon.model.RunResult;
import com.fiserv.optis.qarecon.model.ScenarioDto;
import com.fiserv.optis.qarecon.runner.CucumberProgrammaticRunner;
import com.fiserv.optis.qarecon.service.FeatureService;
import com.fiserv.optis.qarecon.service.ReconService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.fiserv.optis.qarecon.model.mapper.FeatureMapper;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/recon")
public class ReconController {

    @Autowired
    ReconService rSvc;

    private final FeatureService service;
    private final Map<String, RunResult> runs = new ConcurrentHashMap<>();

    public ReconController(FeatureService service) {
        this.service = service;
    }

    @Operation(
            summary = "Run reconciliation",
            description = "Initiates a reconciliation run based on the provided feature and scenario details."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reconciliation run started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Feature or Scenario not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error during reconciliation")
    })
    @PostMapping(value = "/runs", produces = "text/plain")
    public ResponseEntity<String> run(@RequestBody RunRequest req) throws Exception {
        RunResult r = new RunResult();
        r.setFeatureName(req.getFeatureName());
        r.setScenarioName(req.getScenarioName());
        r.setStatus("RUNNING");
        r.setStartedAt(Instant.now());
        runs.put(r.getRunId(), r);
        ReportsContext.runId = r.getRunId();

        // Fetch feature by ID or name
        Optional<FeatureDto> featureOpt = Optional.empty();
        if (req.getFeatureId() != null && !req.getFeatureId().isBlank()) {
            featureOpt = service.getFeatureById(req.getFeatureId()).map(FeatureMapper::toDto);
        } else if (req.getFeatureName() != null && !req.getFeatureName().isBlank()) {
            featureOpt = service.getFeatureByName(req.getFeatureName()).map(FeatureMapper::toDto);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("featureId or featureName is required");
        }

        FeatureDto f = featureOpt.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found"));
        List<ScenarioDto> targets;
        if (req.getScenarioId() != null && !req.getScenarioId().isBlank()) {
            targets = f.getGherkin().getScenarios().stream()
                    .filter(s -> req.getScenarioId().equals(s.getScenarioId()))
                    .toList();
            if (targets.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Scenario not found");
            }
        } else {
            targets = f.getGherkin().getScenarios();
        }



        return ResponseEntity.ok(rSvc.performReconciliation(f));
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<RunResult> getRun(@PathVariable String runId) {
        RunResult r = runs.get(runId);
        if (r == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(r);
    }
}