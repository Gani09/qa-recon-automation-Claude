package com.fiserv.optis.qarecon.dbEntities;

import com.fiserv.optis.qarecon.model.Detail;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "QA_Automation_Reports")
public class ReportEntity {

    @Id
    private String reportId;
    private int totalExecuted;
    private int totalPassed;
    private int totalFailed;
    private List<Detail> details;

    // Getters and setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public Integer getTotalExecuted() { return totalExecuted; }
    public void setTotalExecuted(Integer totalExecuted) { this.totalExecuted = totalExecuted; }

    public Integer getTotalPassed() { return totalPassed; }
    public void setTotalPassed(Integer totalPassed) { this.totalPassed = totalPassed; }

    public Integer getTotalFailed() { return totalFailed; }
    public void setTotalFailed(Integer totalFailed) { this.totalFailed = totalFailed; }

    public List<Detail> getDetails() { return details; }
    public void setDetails(List<Detail> details) { this.details = details; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ReportEntity:\n");
        sb.append(" reportId: ").append(reportId).append("\n");
        sb.append(" totalExecuted: ").append(totalExecuted).append("\n");
        sb.append(" totalPassed: ").append(totalPassed).append("\n");
        sb.append(" totalFailed: ").append(totalFailed).append("\n");
        sb.append(" details:");
        if (details != null && !details.isEmpty()) {
            for (Detail detail : details) {
                sb.append(detail.toString());
            }
        } else {
            sb.append(" none");
        }
        return sb.toString();
    }
}