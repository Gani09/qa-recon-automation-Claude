package com.fiserv.optis.qarecon.service;

import com.fiserv.optis.qarecon.dbEntities.ReportEntity;
import com.fiserv.optis.qarecon.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReportService {
    private final ReportRepository repo;
    public ReportService(ReportRepository repo){ this.repo = repo; }
    public List<ReportEntity> getAllReports(){ return repo.findAll(); }
    public Optional<ReportEntity> getReportByReportId(String id){ return repo.getReportByReportId(id); }
    public ReportEntity saveReport(ReportEntity e){ return repo.save(e); }
}
