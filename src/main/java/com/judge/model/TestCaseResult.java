package com.judge.model;

/**
 * Detailed evaluation result for an individual test case.
 */
public class TestCaseResult {
    private int testCaseIndex;
    private boolean isHidden;
    private Verdict status;
    private long executionTimeMs;
    private String input;
    private String expectedOutput;
    private String actualOutput;
    private String errorOutput;

    public TestCaseResult() {}

    public TestCaseResult(int testCaseIndex, boolean isHidden, Verdict status, long executionTimeMs,
                          String input, String expectedOutput, String actualOutput, String errorOutput) {
        this.testCaseIndex = testCaseIndex;
        this.isHidden = isHidden;
        this.status = status;
        this.executionTimeMs = executionTimeMs;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.actualOutput = actualOutput;
        this.errorOutput = errorOutput;
    }

    public int getTestCaseIndex() {
        return testCaseIndex;
    }

    public void setTestCaseIndex(int testCaseIndex) {
        this.testCaseIndex = testCaseIndex;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }

    public void setIsHidden(boolean isHidden) {
        this.isHidden = isHidden;
    }

    public Verdict getStatus() {
        return status;
    }

    public void setStatus(Verdict status) {
        this.status = status;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public String getActualOutput() {
        return actualOutput;
    }

    public void setActualOutput(String actualOutput) {
        this.actualOutput = actualOutput;
    }

    public String getErrorOutput() {
        return errorOutput;
    }

    public void setErrorOutput(String errorOutput) {
        this.errorOutput = errorOutput;
    }
}
