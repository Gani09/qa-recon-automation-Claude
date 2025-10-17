package com.fiserv.optis.qarecon.util;

import org.apache.poi.ss.usermodel.*;
import org.bson.Document;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ExcelUtils {
    public static List<Map<String, String>> readSheet(String fileName, String sheetName) throws Exception {
        try (InputStream is = ExcelUtils.class.getClassLoader().getResourceAsStream(fileName);
             Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheet(sheetName);
            Iterator<Row> rowIterator = sheet.iterator();
            List<String> headers = new ArrayList<>();
            List<Map<String, String>> data = new ArrayList<>();
            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                for (Cell cell : headerRow) {
                    headers.add(cell.getStringCellValue());
                }
            }
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> rowData = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowData.put(headers.get(i), cell.toString());
                }
                data.add(rowData);
            }
            return data;
        }
    }

    public static List<Map<String, String>> readSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        Iterator<Row> rowIterator = sheet.iterator();
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> data = new ArrayList<>();
        if (rowIterator.hasNext()) {
            Row headerRow = rowIterator.next();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }
        }
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Map<String, String> rowData = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                rowData.put(headers.get(i), cell.toString());
            }
            data.add(rowData);
        }
        return data;
    }

    public static Document buildFilterFromExcelRows(List<Map<String, String>> filterRows) {
        List<Document> filters = new ArrayList<>();
        for (Map<String, String> row : filterRows) {
            String key = row.get("filter_key");
            String condition = row.get("condition");
            String value = row.get("filter_value");
            String valueType = row.get("value_type");

            java.util.function.Function<String, Object> parseValue =  v -> {
                if (v == null) return null;
                String cleaned = v.replaceAll( "^[\\\"']+|[\\\"']+$","");
                switch (valueType != null ? valueType.toLowerCase() : "") {
                    case "date": {
                        String dateStr = cleaned.startsWith("ISODate(")
                                ? cleaned.substring("ISODate(".length(), cleaned.length() - 1).replaceAll( "[\\\"']",  "")
                                : cleaned;
                        if (dateStr.matches(  "\\d{4}-\\d{2}-\\d{2}")) {
                            dateStr += "T00:00:00+00:00";
                        }
                        OffsetDateTime odt = OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        return Date.from(odt.toInstant());
                    }
                    case "numeric": {
                        try {
                            if (cleaned.contains(".")) {
                                return Double.parseDouble(cleaned);
                            } else {
                                return Long.parseLong(cleaned);
                            }
                        } catch (Exception e) {
                            return cleaned;
                        }
                    }
                    case "string":
                    default:
                        return cleaned;
                }
            };

            if ("in".equalsIgnoreCase(condition)) {
                List<Object> values = Arrays.stream(value.split( "\\|"))
                        .map(String::trim)
                        .map(parseValue)
                        .collect(java.util.stream.Collectors.toList());
                filters.add(new Document(key, new Document("$in", values)));
            } else if ("eq".equalsIgnoreCase(condition) || condition == null || condition.isEmpty()) {
                filters.add(new Document(key, parseValue.apply(value)));
            } else {
                // Handles gt, lt, gte, lte, ne, etc.
                filters.add(new Document(key, new Document("$" + condition.toLowerCase(), parseValue.apply(value))));
            }
        }
        return filters.isEmpty() ? new Document() : new Document("$and", filters);
    }

    public static List<Map<String, String>> readFieldMappings(String fileName, String sheetName) throws Exception {
        return readSheet(fileName, sheetName);
    }
}