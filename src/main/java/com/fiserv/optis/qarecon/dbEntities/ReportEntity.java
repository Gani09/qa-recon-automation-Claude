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

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }
    public int getTotalExecuted() { return totalExecuted; }
    public void setTotalExecuted(int totalExecuted) { this.totalExecuted = totalExecuted; }
    public int getTotalPassed() { return totalPassed; }
    public void setTotalPassed(int totalPassed) { this.totalPassed = totalPassed; }
    public int getTotalFailed() { return totalFailed; }
    public void setTotalFailed(int totalFailed) { this.totalFailed = totalFailed; }
    public List<Detail> getDetails() { return details; }
    public void setDetails(List<Detail> details) { this.details = details; }

    @Override public String toString(){
        StringBuilder sb = new StringBuilder("ReportEntity:\n");
        sb.append("  reportId: ").append(reportId).append('\n');
        sb.append("  totalExecuted: ").append(totalExecuted).append('\n');
        sb.append("  totalPassed: ").append(totalPassed).append('\n');
        sb.append("  totalFailed: ").append(totalFailed).append('\n');
        sb.append("  details: ").append(details==null? "none" : details.size()).append('\n');
        return sb.toString();
    }
}
