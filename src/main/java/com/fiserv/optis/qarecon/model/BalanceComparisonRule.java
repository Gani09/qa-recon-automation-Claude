package com.fiserv.optis.qarecon.model;

public class BalanceComparisonRule {
    private String ruleName;
    private String sourceExpression;
    private String targetExpression;
    private ComparisonOperator operator;
    private String tolerance;

    public BalanceComparisonRule() {}

    public BalanceComparisonRule(String ruleName, String sourceExpression,
                                 String targetExpression, ComparisonOperator operator,
                                 String tolerance) {
        this.ruleName = ruleName;
        this.sourceExpression = sourceExpression;
        this.targetExpression = targetExpression;
        this.operator = operator;
        this.tolerance = tolerance;
    }

    // Getters and setters
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getSourceExpression() { return sourceExpression; }
    public void setSourceExpression(String sourceExpression) { this.sourceExpression = sourceExpression; }

    public String getTargetExpression() { return targetExpression; }
    public void setTargetExpression(String targetExpression) { this.targetExpression = targetExpression; }

    public ComparisonOperator getOperator() { return operator; }
    public void setOperator(ComparisonOperator operator) { this.operator = operator; }

    public String getTolerance() { return tolerance; }
    public void setTolerance(String tolerance) { this.tolerance = tolerance; }

    public enum ComparisonOperator {
        EQUALS, GREATER_THAN, LESS_THAN, GREATER_OR_EQUAL, LESS_OR_EQUAL
    }
}