package com.fiserv.optis.qarecon.service;

import com.fiserv.optis.qarecon.model.entities.FeatureEntity;
import com.fiserv.optis.qarecon.repository.FeatureRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FeatureService {
    private final FeatureRepository repo;
    public FeatureService(FeatureRepository repo){ this.repo = repo; }
    public List<FeatureEntity> getAllFeatures(){ return repo.findAll(); }
    public Optional<FeatureEntity> getFeatureById(String id){ return repo.findById(id); }
    public Optional<FeatureEntity> getFeatureByName(String name){ return repo.findByFeatureName(name); }
    public FeatureEntity saveFeature(FeatureEntity e){ return repo.save(e); }
    public void deleteFeatureById(String id){ repo.deleteById(id); }
}
