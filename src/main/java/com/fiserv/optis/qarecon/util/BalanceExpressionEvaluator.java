package com.fiserv.optis.qarecon.util;

import org.bson.Document;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates mathematical expressions against MongoDB documents
 * Supports: +, -, *, /, (, )
 */
public class BalanceExpressionEvaluator {

    /**
     * Evaluate an expression like "field1 + field2 - field3" against a document
     * @param expression The expression to evaluate (e.g., "debitAmount + creditAmount")
     * @param doc The document containing the field values
     * @param fieldSuffix Optional suffix for field names (e.g., "_aggregated")
     * @return The evaluated result
     */
    public static BigDecimal evaluate(String expression, Document doc, String fieldSuffix) {
        if (expression == null || expression.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Replace field names with their values
        String processedExpression = replaceFieldsWithValues(expression, doc, fieldSuffix);

        // Evaluate the mathematical expression
        return evaluateMathExpression(processedExpression);
    }

    /**
     * Replace all field names in the expression with their actual values
     */
    private static String replaceFieldsWithValues(String expression, Document doc, String fieldSuffix) {
        // Find all field names (alphanumeric + underscore + dot for nested fields)
        Pattern pattern = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_.]*");
        Matcher matcher = pattern.matcher(expression);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String fieldName = matcher.group();

            // Skip if it's a number or operator keyword
            if (isNumeric(fieldName)) {
                continue;
            }

            // Get field value from document
            String fullFieldName = fieldSuffix != null && !fieldSuffix.isEmpty()
                    ? fieldName + fieldSuffix
                    : fieldName;

            BigDecimal value = getFieldValue(doc, fullFieldName);

            // Replace with the value
            matcher.appendReplacement(result, value.toString());
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Get field value from document (supports nested fields with dot notation)
     */
    private static BigDecimal getFieldValue(Document doc, String fieldPath) {
        Object value = doc;

        for (String key : fieldPath.split("\\.")) {
            if (value instanceof Document) {
                value = ((Document) value).get(key);
            } else {
                return BigDecimal.ZERO;
            }
        }

        return toBigDecimal(value);
    }

    /**
     * Convert object to BigDecimal
     */
    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(((Number) v).toString());
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Evaluate a mathematical expression using a simple stack-based calculator
     * Supports: +, -, *, /, (, )
     */
    private static BigDecimal evaluateMathExpression(String expression) {
        try {
            // Remove all whitespace
            expression = expression.replaceAll("\\s+", "");

            // Use a simple recursive descent parser
            return parse(expression);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid expression: " + expression, e);
        }
    }

    /**
     * Simple expression parser
     */
    private static BigDecimal parse(String expr) {
        return parseAddSub(expr, new int[]{0});
    }

    private static BigDecimal parseAddSub(String expr, int[] pos) {
        BigDecimal result = parseMulDiv(expr, pos);

        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op != '+' && op != '-') break;

            pos[0]++; // consume operator
            BigDecimal right = parseMulDiv(expr, pos);

            if (op == '+') {
                result = result.add(right);
            } else {
                result = result.subtract(right);
            }
        }

        return result;
    }

    private static BigDecimal parseMulDiv(String expr, int[] pos) {
        BigDecimal result = parsePrimary(expr, pos);

        while (pos[0] < expr.length()) {
            char op = expr.charAt(pos[0]);
            if (op != '*' && op != '/') break;

            pos[0]++; // consume operator
            BigDecimal right = parsePrimary(expr, pos);

            if (op == '*') {
                result = result.multiply(right);
            } else {
                result = result.divide(right, 10, java.math.RoundingMode.HALF_UP);
            }
        }

        return result;
    }

    private static BigDecimal parsePrimary(String expr, int[] pos) {
        if (pos[0] >= expr.length()) {
            throw new IllegalArgumentException("Unexpected end of expression");
        }

        char c = expr.charAt(pos[0]);

        // Handle parentheses
        if (c == '(') {
            pos[0]++; // consume '('
            BigDecimal result = parseAddSub(expr, pos);
            if (pos[0] >= expr.length() || expr.charAt(pos[0]) != ')') {
                throw new IllegalArgumentException("Missing closing parenthesis");
            }
            pos[0]++; // consume ')'
            return result;
        }

        // Handle unary minus
        if (c == '-') {
            pos[0]++;
            return parsePrimary(expr, pos).negate();
        }

        // Handle unary plus
        if (c == '+') {
            pos[0]++;
            return parsePrimary(expr, pos);
        }

        // Parse number
        int start = pos[0];
        while (pos[0] < expr.length()) {
            c = expr.charAt(pos[0]);
            if (!Character.isDigit(c) && c != '.') break;
            pos[0]++;
        }

        String numStr = expr.substring(start, pos[0]);
        return new BigDecimal(numStr);
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}