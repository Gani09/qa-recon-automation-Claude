package com.fiserv.optis.qarecon.model;

import org.bson.Document;
import com.fiserv.optis.qarecon.model.entities.ReconConfigEntity;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class ReconciliationSpec {

    public String profileName;
    public String sourceCollection;
    public String targetCollection;
    public Document sourceFilter;
    public Document targetFilter;
    public List<JoinKey> joinKeys;
    public AggregationPlan leftAgg;
    public AggregationPlan rightAgg;
    public List<FieldMapping> mappings;
    public BigDecimal defaultNumericTolerance = new BigDecimal("0.00");
    public Duration defaultTimeTolerance = Duration.ZERO;
    public String runId;
    public OutputSettings output = new OutputSettings();
    //public String configFile;
    public ReconConfigEntity configEntity;
    public String mappingCollection;
    public List<Document> mappingDocs;
    public List<Map<String, String>> balanceFieldMappings;
    // In ReconciliationSpec.java, add:
    public List<BalanceFieldConfig> balanceFieldConfigs;
    public List<BalanceComparisonRule> balanceComparisonRules;

    public static final class OutputSettings {
        public String baseDir = "target/reports";
    }
}