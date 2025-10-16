package com.fiserv.optis.qarecon.model;

import java.time.Instant;
import java.util.UUID;

public class RunResult {
    private String runId = UUID.randomUUID().toString();
    private String featureName;
    private String scenarioName;
    private String status = "QUEUED";
    private Instant startedAt;
    private Instant finishedAt;
    private String reportText;

    public String getRunId(){ return runId; }
    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public String getReportText() { return reportText; }
    public void setReportText(String reportText) { this.reportText = reportText; }
}
