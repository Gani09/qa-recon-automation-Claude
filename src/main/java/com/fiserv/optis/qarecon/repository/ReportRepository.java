package com.fiserv.optis.qarecon.repository;

import com.fiserv.optis.qarecon.dbEntities.ReportEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ReportRepository extends MongoRepository<ReportEntity, String> {
    Optional<ReportEntity> getReportByReportId(String reportId);
}
