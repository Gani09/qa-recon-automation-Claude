package com.fiserv.optis.qarecon.repository;

import com.fiserv.optis.qarecon.model.entities.ReconConfigEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ReconConfigRepository extends MongoRepository<ReconConfigEntity, String> {
    Optional<ReconConfigEntity> findByConfigName(String configName);
}
