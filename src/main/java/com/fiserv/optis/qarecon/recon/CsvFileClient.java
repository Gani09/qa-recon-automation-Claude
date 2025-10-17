package com.fiserv.optis.qarecon.recon;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CsvFileClient implements DataSourceClient {

    @Override
    public List<Map<String, Object>> fetch(String path) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(path));
        if (lines.isEmpty()) return Collections.emptyList();

        String[] headers = lines.get(0).split(",");
        List<Map<String, Object>> out = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",");
            Map<String, Object> row = new HashMap<>();
            for (int j = 0; j < headers.length && j < parts.length; j++) {
                row.put(headers[j].trim(), parts[j].trim());
            }
            out.add(row);
        }
        return out;
    }
}