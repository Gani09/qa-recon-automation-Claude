package com.fiserv.optis.qarecon.util;

import com.fiserv.optis.qarecon.model.AggregationStrategy;
import com.fiserv.optis.qarecon.model.BalanceFieldConfig;
import com.fiserv.optis.qarecon.model.BalanceComparisonRule;
import org.apache.poi.ss.usermodel.*;
import org.bson.Document;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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



    public static List<BalanceFieldConfig> readBalanceFieldConfigs(
            Workbook workbook,
            String balanceFieldsSheet,
            String balanceConfigSheet) {

        // Read balance_config sheet for grouping
        List<Map<String, String>> configRows = readSheet(workbook, balanceConfigSheet);
        Map<String, List<String>> groupingByCollection = new HashMap<>();

        for (Map<String, String> row : configRows) {
            String collection = row.get("collection");
            String groupByStr = row.get("groupByFields");

            if (collection != null && groupByStr != null) {
                List<String> groupByFields = Arrays.stream(groupByStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                groupingByCollection.put(collection.toLowerCase(), groupByFields);
            }
        }

        // Read balance_fields sheet
        List<Map<String, String>> rows = readSheet(workbook, balanceFieldsSheet);
        List<BalanceFieldConfig> configs = new ArrayList<>();

        for (Map<String, String> row : rows) {
            String collection = row.get("collection");
            String fieldName = row.get("fieldName");
            String strategyStr = row.get("aggregationStrategy");

            if (collection == null || fieldName == null) {
                continue;
            }

            AggregationStrategy strategy = AggregationStrategy.SUM;
            if (strategyStr != null && !strategyStr.trim().isEmpty()) {
                try {
                    strategy = AggregationStrategy.valueOf(strategyStr.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Use default
                }
            }

            // Get groupByFields from balance_config
            List<String> groupByFields = groupingByCollection.get(collection.toLowerCase());
            if (groupByFields == null) {
                groupByFields = new ArrayList<>();
            }

            BalanceFieldConfig config = new BalanceFieldConfig(
                    collection.trim(),
                    fieldName.trim(),
                    strategy,
                    groupByFields
            );
            configs.add(config);
        }

        return configs;
    }


    public static List<Map<String, String>> readFieldMappings(String fileName, String sheetName) throws Exception {
        return readSheet(fileName, sheetName);
    }

    // Add to ExcelUtils.java

    public static List<BalanceComparisonRule> readBalanceComparisonRules(
            Workbook workbook,
            String sheetName) {

        List<Map<String, String>> rows = readSheet(workbook, sheetName);
        List<BalanceComparisonRule> rules = new ArrayList<>();

        for (Map<String, String> row : rows) {
            String ruleName = row.get("ruleName");
            String sourceExpression = row.get("sourceExpression");
            String targetExpression = row.get("targetExpression");
            String operatorStr = row.get("operator");
            String tolerance = row.get("tolerance");

            if (ruleName == null || sourceExpression == null || targetExpression == null) {
                continue;
            }

            BalanceComparisonRule.ComparisonOperator operator =
                    BalanceComparisonRule.ComparisonOperator.EQUALS;
            if (operatorStr != null && !operatorStr.trim().isEmpty()) {
                try {
                    operator = BalanceComparisonRule.ComparisonOperator.valueOf(
                            operatorStr.trim().toUpperCase()
                    );
                } catch (IllegalArgumentException e) {
                    // Use default
                }
            }

            BalanceComparisonRule rule = new BalanceComparisonRule(
                    ruleName.trim(),
                    sourceExpression.trim(),
                    targetExpression.trim(),
                    operator,
                    tolerance != null ? tolerance.trim() : "0.01"
            );
            rules.add(rule);
        }

        return rules;
    }

}