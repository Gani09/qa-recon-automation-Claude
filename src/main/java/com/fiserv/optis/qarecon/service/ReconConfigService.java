package com.fiserv.optis.qarecon.service;

import com.fiserv.optis.qarecon.model.BalanceFieldConfig;
import com.fiserv.optis.qarecon.model.BalanceComparisonRule;
import com.fiserv.optis.qarecon.model.entities.ReconConfigEntity;
import com.fiserv.optis.qarecon.model.FieldMapping;
import com.fiserv.optis.qarecon.repository.ReconConfigRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fiserv.optis.qarecon.util.ExcelUtils;
import java.io.InputStream;
import java.util.*;


import com.fiserv.optis.qarecon.model.JoinKey;

@Service
public class ReconConfigService {
    private final ReconConfigRepository repository;

    public ReconConfigService(ReconConfigRepository repository) { this.repository = repository; }

    public List<ReconConfigEntity> getAllConfigs() { return repository.findAll(); }

    public Optional<ReconConfigEntity> getConfigById(String id) { return repository.findById(id); }

    public Optional<ReconConfigEntity> getConfigByName(String name) { return repository.findByConfigName(name); }

    public void deleteConfigById(String id) { repository.deleteById(id); }

    public ReconConfigEntity processAndSaveConfig(MultipartFile file, String source, String target) throws Exception {
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {

            // Existing code for filters, join keys, field mappings...
            var filtersSheet = ExcelUtils.readSheet(workbook, "filters");
            var filters = new HashMap<String, Object>();
            filters.put("sourceFilter", ExcelUtils.buildFilterFromExcelRows(
                    filtersSheet.stream()
                            .filter(row -> "source".equalsIgnoreCase(row.get("collection")))
                            .toList()));
            filters.put("targetFilter", ExcelUtils.buildFilterFromExcelRows(
                    filtersSheet.stream()
                            .filter(row -> "target".equalsIgnoreCase(row.get("collection")))
                            .toList()));

            var joinKeysSheet = ExcelUtils.readSheet(workbook, "join_keys");
            List<JoinKey> joinKeys = joinKeysSheet.stream()
                    .map(row -> new JoinKey(
                            row.get("SourceField"),
                            row.get("SourceFieldAs"),
                            row.get("TargetField"),
                            row.get("TargetFieldAs")
                    ))
                    .toList();

            var fieldMappingsSheet = ExcelUtils.readSheet(workbook, "field_mappings");
            List<FieldMapping> fieldMappings = fieldMappingsSheet.stream()
                    .map(FieldMapping::from)
                    .toList();

            // MODIFIED: Read balance fields with balance_config for grouping
            List<BalanceFieldConfig> balanceFieldConfigs =
                    ExcelUtils.readBalanceFieldConfigs(workbook, "balance_fields", "balance_config");

            // NEW: Read balance comparison rules
            List<BalanceComparisonRule> comparisonRules = new ArrayList<>();
            try {
                comparisonRules = ExcelUtils.readBalanceComparisonRules(workbook, "balance_comparison_rules");
            } catch (Exception e) {
                // Sheet optional - use empty list if not present
            }

            ReconConfigEntity entity = new ReconConfigEntity();
            entity.setConfigId(UUID.randomUUID().toString());
            entity.setConfigName("Recon_Config_" + source + "-" + target);
            entity.setFilters(filters);
            entity.setJoinKeys(joinKeys);
            entity.setFieldMappings(fieldMappings);
            entity.setBalanceFieldConfigs(balanceFieldConfigs);
            entity.setBalanceComparisonRules(comparisonRules); // NEW

            return repository.save(entity);
        }
    }
}