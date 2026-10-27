package com.judge.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate evaluation result returned to the client.
 */
public class ExecutionResult {
    private Verdict overallVerdict;
    private int totalTests;
    private int passedTests;
    private long totalExecutionTimeMs;
    private String compilerError;
    private String message;
    private List<TestCaseResult> testCaseResults;
    private double percentileBeaten = 0.0;

    public ExecutionResult() {
        this.testCaseResults = new ArrayList<>();
    }

    public ExecutionResult(Verdict overallVerdict, int totalTests, int passedTests,
                           long totalExecutionTimeMs, String compilerError,
                           String message, List<TestCaseResult> testCaseResults) {
        this.overallVerdict = overallVerdict;
        this.totalTests = totalTests;
        this.passedTests = passedTests;
        this.totalExecutionTimeMs = totalExecutionTimeMs;
        this.compilerError = compilerError;
        this.message = message;
        this.testCaseResults = testCaseResults != null ? testCaseResults : new ArrayList<>();
    }

    public static ExecutionResult compilationError(String compilerError) {
        ExecutionResult result = new ExecutionResult();
        result.setOverallVerdict(Verdict.COMPILATION_ERROR);
        result.setCompilerError(compilerError);
        result.setMessage("Compilation Error");
        return result;
    }

    public static ExecutionResult securityViolation(String securityMessage) {
        ExecutionResult result = new ExecutionResult();
        result.setOverallVerdict(Verdict.SECURITY_VIOLATION);
        result.setCompilerError(securityMessage);
        result.setMessage("Security Violation: Action Disallowed");
        return result;
    }

    public double getPercentileBeaten() {
        return percentileBeaten;
    }

    public void setPercentileBeaten(double percentileBeaten) {
        this.percentileBeaten = percentileBeaten;
    }

    public Verdict getOverallVerdict() {
        return overallVerdict;
    }

    public void setOverallVerdict(Verdict overallVerdict) {
        this.overallVerdict = overallVerdict;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    public int getPassedTests() {
        return passedTests;
    }

    public void setPassedTests(int passedTests) {
        this.passedTests = passedTests;
    }

    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }

    public void setTotalExecutionTimeMs(long totalExecutionTimeMs) {
        this.totalExecutionTimeMs = totalExecutionTimeMs;
    }

    public String getCompilerError() {
        return compilerError;
    }

    public void setCompilerError(String compilerError) {
        this.compilerError = compilerError;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<TestCaseResult> getTestCaseResults() {
        return testCaseResults;
    }

    public void setTestCaseResults(List<TestCaseResult> testCaseResults) {
        this.testCaseResults = testCaseResults;
    }
}
