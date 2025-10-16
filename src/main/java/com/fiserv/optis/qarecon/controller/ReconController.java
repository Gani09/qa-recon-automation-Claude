package com.fiserv.optis.qarecon.controller;

import com.fiserv.optis.qarecon.model.FeatureDto;
import com.fiserv.optis.qarecon.model.RunRequest;
import com.fiserv.optis.qarecon.model.RunResult;
import com.fiserv.optis.qarecon.model.mapper.FeatureMapper;
import com.fiserv.optis.qarecon.service.FeatureService;
import com.fiserv.optis.qarecon.service.ReconService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/recon")
public class ReconController {
    private final FeatureService featureService;
    private final ReconService reconService;

    public ReconController(FeatureService featureService, ReconService reconService){
        this.featureService = featureService; this.reconService = reconService;
    }

    @PostMapping(value="/runs", produces="text/plain")
    public ResponseEntity<String> run(@RequestBody RunRequest req) throws Exception {
        FeatureDto f;
        if (req.getFeatureId()!=null && !req.getFeatureId().isBlank()) {
            f = featureService.getFeatureById(req.getFeatureId()).map(FeatureMapper::toDto)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found"));
        } else if (req.getFeatureName()!=null && !req.getFeatureName().isBlank()) {
            f = featureService.getFeatureByName(req.getFeatureName()).map(FeatureMapper::toDto)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("featureId or featureName is required");
        }
        String out = reconService.performReconciliation(f);
        return ResponseEntity.ok(out);
    }
}
