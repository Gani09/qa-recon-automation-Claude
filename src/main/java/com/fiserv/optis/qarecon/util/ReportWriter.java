package com.fiserv.optis.qarecon.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fiserv.optis.qarecon.engine.ReconciliationEngine.ReconciliationResult;
import com.fiserv.optis.qarecon.model.ReconciliationSpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ReportWriter {
    private ReportWriter(){}

    public static void writeAll(ReconciliationSpec spec, ReconciliationResult r, String outDir) throws IOException {
        Path base = Path.of(outDir.replace("{{date}}", LocalDate.now().toString()));
        Files.createDirectories(base);

        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        Files.writeString(base.resolve("recon-result.json"), om.writeValueAsString(r), StandardCharsets.UTF_8);

        writeCsv(base.resolve("left-only.csv"), "key", r.leftOnlyKeys);
        writeCsv(base.resolve("right-only.csv"), "key", r.rightOnlyKeys);

        List<String> lines = new ArrayList<>();
        lines.add("key,leftField,rightField,type,leftValue,rightValue,delta,tolerance");
        r.fieldDiffs.stream()
            .map(d -> String.join(",", safe(d.key), safe(d.leftField), safe(d.rightField), safe(d.type),
                                   safe(d.leftValue), safe(d.rightValue), safe(d.delta), safe(d.tolerance)))
            .forEach(lines::add);
        Files.write(base.resolve("mismatches.csv"), lines, StandardCharsets.UTF_8);
    }

    private static void writeCsv(Path path, String header, Collection<String> keys) throws IOException {
        List<String> lines = new ArrayList<>(keys.size()+1);
        lines.add(header);
        for (String k : keys) lines.add(safe(k));
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static String safe(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        s = s.replace("\r\n", " ").replace("\n", " ").replace("\r", " ");
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0) {
            s = '"' + s.replace(""", """") + '"';
        }
        return s;
    }
}
