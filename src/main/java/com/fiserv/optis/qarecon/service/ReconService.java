package com.fiserv.optis.qarecon.service;

import com.fiserv.optis.qarecon.model.FeatureDto;
import com.fiserv.optis.qarecon.util.GherkinParser;
import com.fiserv.optis.qarecon.runner.CucumberProgrammaticRunner;
import org.springframework.stereotype.Service;

@Service
public class ReconService {
    public String performReconciliation(FeatureDto featureDto) throws Exception {
        String featureBody = GherkinParser.toGherkinString(featureDto);
        return CucumberProgrammaticRunner.runFeatureWithGlue(
                featureDto.getFeatureName()==null? "ReconFeature" : featureDto.getFeatureName(),
                featureBody,
                "com.fiserv.optis.qarecon.runner"
        );
    }
}
