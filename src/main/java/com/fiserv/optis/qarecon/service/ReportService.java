package com.fiserv.optis.qarecon.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.fiserv.optis.qarecon.dbEntities.ReportEntity;
import com.fiserv.optis.qarecon.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) { this.reportRepository = reportRepository; }

    public List<ReportEntity> getAllReports() { return reportRepository.findAll(); }

    public Optional<ReportEntity> getReportByReportId(String reportId) {
        return reportRepository.getReportByReportId(reportId);
    }

    public ReportEntity saveReport(ReportEntity reportEntity) { return reportRepository.save(reportEntity); }
}