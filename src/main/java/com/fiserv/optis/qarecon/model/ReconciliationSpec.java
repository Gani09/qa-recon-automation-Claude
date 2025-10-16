package com.fiserv.optis.qarecon.model;

import org.bson.Document;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReconciliationSpec {
    public String runId;
    public String sourceCollection;
    public String targetCollection;
    public Document sourceFilter = new Document();
    public Document targetFilter = new Document();
    public List<JoinKey> joinKeys = new ArrayList<>();
    public Mode mode = Mode.ONE_TO_ONE;
    public List<FieldMapping> mappings = new ArrayList<>();
    public BigDecimal defaultNumericTolerance = new BigDecimal("0.00");
    public Duration defaultTimeTolerance = Duration.ZERO;
    public OutputSettings output = new OutputSettings();
    public Map<String, List<String>> balanceFields;

    public static class OutputSettings { public String baseDir = "target/reports/{{date}}"; }
}
