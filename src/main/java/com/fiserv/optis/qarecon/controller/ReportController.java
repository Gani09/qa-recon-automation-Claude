package com.fiserv.optis.qarecon.controller;

import com.fiserv.optis.qarecon.dbEntities.ReportEntity;
import com.fiserv.optis.qarecon.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/{reportId}")
    public String getReportByReportId(@PathVariable String reportId) {
        return reportService.getReportByReportId(reportId)
                .map(ReportEntity::toString)
                .orElse("Report not found");
    }

    @GetMapping("/all")
    public List<String> getAllReports() {
        return reportService.getAllReports()
                .stream()
                .map(ReportEntity::toString)
                .collect(Collectors.toList());
    }
}