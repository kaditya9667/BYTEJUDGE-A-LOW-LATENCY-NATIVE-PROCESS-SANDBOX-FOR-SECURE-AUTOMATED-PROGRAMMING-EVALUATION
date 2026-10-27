package com.judge.model;

/**
 * Represents a single test case for an algorithmic problem.
 */
public class TestCase {
    private String input;
    private String expectedOutput;
    private boolean isHidden;

    public TestCase() {}

    public TestCase(String input, String expectedOutput, boolean isHidden) {
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.isHidden = isHidden;
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

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }

    public void setIsHidden(boolean isHidden) {
        this.isHidden = isHidden;
    }
}
