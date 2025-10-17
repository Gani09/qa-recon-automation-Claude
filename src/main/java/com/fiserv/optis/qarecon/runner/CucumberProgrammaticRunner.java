package com.fiserv.optis.qarecon.runner;

import io.cucumber.core.cli.Main;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CucumberProgrammaticRunner {

    public static String runFeatureWithGlue(String featureName, String featureBody, String gluePackage) throws Exception {
        Path tempDir = Files.createTempDirectory("features");
        Path feat = tempDir.resolve(featureName.replaceAll("[^a-zA-Z0-9_-]", "_") + ".feature");
        Files.writeString(feat, featureBody);

        String[] argv = new String[]{
                "--glue", gluePackage,
                "--plugin", "pretty",
                "--plugin", "com.fiserv.optis.qarecon.report.SummaryJsonFormatter",
                feat.toAbsolutePath().toString()
        };

        byte status = Main.run(argv, Thread.currentThread().getContextClassLoader());
        Path summaryPath = Path.of("target", "cucumber-summary.txt");
        return Files.readString(summaryPath);
    }
}