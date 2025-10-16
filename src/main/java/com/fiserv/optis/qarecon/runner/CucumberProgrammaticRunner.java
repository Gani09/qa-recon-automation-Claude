package com.fiserv.optis.qarecon.runner;

import io.cucumber.core.cli.Main;
import java.nio.file.Files;
import java.nio.file.Path;

public class CucumberProgrammaticRunner {
    public static String runFeatureWithGlue(String featureName, String featureBody, String gluePackage) throws Exception {
        Path tempDir = Files.createTempDirectory("features");
        Path feat = tempDir.resolve(featureName.replaceAll("[^a-zA-Z0-9_-]", "-") + ".feature");
        Files.writeString(feat, featureBody);

        String[] argv = new String[]{
                "--glue", gluePackage,
                "--plugin", "pretty",
                feat.toAbsolutePath().toString()
        };
        Main.run(argv, Thread.currentThread().getContextClassLoader());
        return "Cucumber run finished for " + featureName + " (" + feat + ")";
    }
}
