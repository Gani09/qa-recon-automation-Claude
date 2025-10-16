package com.fiserv.optis.qarecon.repository;

import com.fiserv.optis.qarecon.model.entities.FeatureEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface FeatureRepository extends MongoRepository<FeatureEntity, String> {
    Optional<FeatureEntity> findByFeatureName(String featureName);
}
