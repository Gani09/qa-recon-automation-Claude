package com.fiserv.optis.qarecon.service;

import com.fiserv.optis.qarecon.model.entities.ReconConfigEntity;
import com.fiserv.optis.qarecon.model.FieldMapping;
import com.fiserv.optis.qarecon.repository.ReconConfigRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fiserv.optis.qarecon.util.ExcelUtils;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
            var filtersSheet = ExcelUtils.readSheet(workbook, "filters");
            var filters = new java.util.HashMap<String, Object>();
            filters.put("sourceFilter", ExcelUtils.buildFilterFromExcelRows(
                    filtersSheet.stream().filter( row -> "source".equalsIgnoreCase(row.get("collection"))).toList()));
            filters.put("targetFilter", ExcelUtils.buildFilterFromExcelRows(
                    filtersSheet.stream().filter( row -> "target".equalsIgnoreCase(row.get("collection"))).toList()));

            var joinKeysSheet = ExcelUtils.readSheet(workbook, "join_keys");
            var fieldMappingsSheet = ExcelUtils.readSheet(workbook, "field_mappings");
            var balanceFieldsSheet = ExcelUtils.readSheet(workbook, "balance_fields");

            ReconConfigEntity entity = new ReconConfigEntity();
            entity.setConfigId(UUID.randomUUID().toString());
            entity.setConfigName("Recon_Config_"+ source + "-" + target);
            entity.setFilters(filters);

            List<JoinKey> joinKeys = joinKeysSheet.stream()
                    .map( row -> new JoinKey(
                    row.get("SourceField"),
                    row.get("SourceFieldAs"),
                    row.get("TargetField"),
                    row.get("TargetFieldAs")
            )) .toList();
            entity.setJoinKeys(joinKeys);
            List<FieldMapping> fieldMappings = fieldMappingsSheet.stream()
                    .map(FieldMapping::from)
                    .toList();
            entity.setFieldMappings(fieldMappings);

            Map<String, List<String>> balanceFields = new java.util.HashMap<>();
            for (var row : balanceFieldsSheet) {
                String key = row.get("collection");
                String value = row.get("fieldName");
                if (key != null && value != null) {
                    balanceFields.computeIfAbsent(key, k ->new java.util.ArrayList<>()).add(value);
                }
            }
            entity.setBalanceFields(balanceFields);

            return repository.save(entity);
        }
    }
}