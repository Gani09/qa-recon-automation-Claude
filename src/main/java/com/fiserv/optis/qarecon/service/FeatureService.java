package com.fiserv.optis.qarecon.service;

import org.springframework.stereotype.Service;

import com.fiserv.optis.qarecon.model.entities.FeatureEntity;
import com.fiserv.optis.qarecon.repository.FeatureRepository;
import java.util.List;
import java.util.Optional;

@Service
public class FeatureService {

    private final FeatureRepository featureRepository;

    public FeatureService(FeatureRepository featureRepository) { this.featureRepository = featureRepository; }

    public List<FeatureEntity> getAllFeatures() { return featureRepository.findAll(); }

    public Optional<FeatureEntity> getFeatureById(String id) { return featureRepository.findById(id); }

    public Optional<FeatureEntity> getFeatureByName(String featureName) {
        return featureRepository.findByFeatureName(featureName);
    }

    public FeatureEntity saveFeature(FeatureEntity feature) { return featureRepository.save(feature); }

    public void deleteFeatureById(String id) { featureRepository.deleteById(id); }
}