package com.fiserv.optis.qarecon.controller;

import com.fiserv.optis.qarecon.model.entities.ReconConfigEntity;
import com.fiserv.optis.qarecon.service.ReconConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/config")
public class ReconConfigController {

    private final ReconConfigService service;

    public ReconConfigController(ReconConfigService service) {
        this.service = service;
    }

    @Operation(summary = "Lists all Configurations")
    @ApiResponse(responseCode = "200", description = "Lists all available configurations in the database")
    @GetMapping("/listAll")
    public List<ReconConfigEntity> listAll() {
        return service.getAllConfigs();
    }

    @Operation(summary = "Get configuration by ID", description = "Fetches a reconciliation configuration by its unique ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuration found"),
            @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    @GetMapping("/view/id/{id}")
    public ResponseEntity<ReconConfigEntity> getById(
            @Parameter(description = "The unique configuration ID", required = true)
            @PathVariable String id) {
        return service.getConfigById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get configuration by name",
            description = "Fetches a reconciliation configuration by its unique name"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuration found"),
            @ApiResponse(responseCode = "404", description = "Configuration not found")
    })
    @GetMapping("/view/name/{name}")
    public ResponseEntity<ReconConfigEntity> getByName(
            @Parameter(description = "The unique configuration name", required = true)
            @PathVariable String name) {
        return service.getConfigByName(name)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Upload and save a configuration Excel file",
            description = "Accepts an Excel file, reads the file and writes the configuration to database for the specified source and target collections."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuration processed and saved successfully"),
            @ApiResponse(responseCode = "400", description = "Failed to process file")
    })
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadConfig(
            @Parameter(description = "Excel file containing configuration", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Source collection name", required = true)
            @RequestParam("source") String source,
            @Parameter(description = "Target collection name", required = true)
            @RequestParam("target") String target) {
        try {
            ReconConfigEntity saved = service.processAndSaveConfig(file, source, target);
            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Failed to process file: " + ex.getMessage());
        }
    }
}