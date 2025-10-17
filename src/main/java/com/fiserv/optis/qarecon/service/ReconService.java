package com.fiserv.optis.qarecon.service;

import com.fiserv.optis.qarecon.constants.ReportsContext;
import com.fiserv.optis.qarecon.model.FeatureDto;
import com.fiserv.optis.qarecon.runner.CucumberProgrammaticRunner;
import com.fiserv.optis.qarecon.util.GherkinParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReconService {

    @Autowired
    FeatureService service;

    public String performReconciliation(FeatureDto featuredto) {
        // each time set to null to avoid repetition of data
        ReportsContext.genericPojoMapForFailureWithKey = null;

        StringBuilder fullReport = new StringBuilder();
        String out=null;
        //FeatureDto f = service.getFeature(req.getFeatureName()).orElseThrow();

        try {
            String featureBody = GherkinParser.toGherkinString(featuredto);
            out = CucumberProgrammaticRunner.runFeatureWithGlue(featuredto.getFeatureName(),
                    featureBody, "com.fiserv.optis.qarecon.runner");
            fullReport.append("Run Id is : ").append(ReportsContext.runId).append("\n").append(out).append("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fullReport.toString();
    }
}