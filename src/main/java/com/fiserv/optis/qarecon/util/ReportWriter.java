package com.fiserv.optis.qarecon.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fiserv.optis.qarecon.engine.ReconciliationEngine.ReconciliationResult;
import com.fiserv.optis.qarecon.model.ReconciliationSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public final class ReportWriter {

    public static void writeAll(ReconciliationSpec spec, ReconciliationResult r, String outDir) throws IOException {
        Path base = Path.of(outDir.replace("{{date}}", java.time.LocalDate.now().toString()));
        Files.createDirectories(base);

        // JSON result
        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        Files.writeString(base.resolve("recon-result.json"), om.writeValueAsString(r));

        // CSVs
        writeCsv(base.resolve("left-only.csv"), r.leftOnlyKeys);
        writeCsv(base.resolve("right-only.csv"), r.rightOnlyKeys);

        // mismatches
        var header = "key,leftField,rightField,type,leftValue,rightValue,delta,tolerance";
        var rows = r.fieldDiffs.stream().map(d -> String.join(",",
                safe(d.key), safe(d.leftField), safe(d.rightField), safe(d.type),
                safe(d.leftValue), safe(d.rightValue), safe(d.delta), safe(d.tolerance)
        )).collect(Collectors.toList());
        Files.write(base.resolve("mismatches.csv"),
        java.util.stream.Stream.concat(java.util.stream.Stream.of(header), rows.stream()).collect(Collectors.toList()));
    }

    private static void writeCsv(Path path, Set<String> keys) throws IOException {
        Files.write(path, java.util.stream.Stream.concat(
                java.util.stream.Stream.of("key"),
        keys.stream().map(ReportWriter::safe)
        ).collect(Collectors.toList()));
    }

    private static String safe(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        if (s.contains(",") || s.contains("\"")) {
            s = s.replace("\"", "\"\"");
            s = "\"" + s + "\"";
        }
        return s;
    }
}