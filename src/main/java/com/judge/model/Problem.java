package com.judge.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Problem definition containing metadata, problem statement, starter code, and test cases.
 */
public class Problem {
    private String id;
    private String title;
    private String difficulty; // Easy, Medium, Hard
    private String description;
    private List<String> examples;
    private List<String> constraints;
    private String starterCode;
    private String starterCodeCpp;
    private List<TestCase> testCases;

    public Problem() {
        this.examples = new ArrayList<>();
        this.constraints = new ArrayList<>();
        this.testCases = new ArrayList<>();
    }

    public Problem(String id, String title, String difficulty, String description,
                   List<String> examples, List<String> constraints,
                   String starterCode, List<TestCase> testCases) {
        this(id, title, difficulty, description, examples, constraints, starterCode, null, testCases);
    }

    public Problem(String id, String title, String difficulty, String description,
                   List<String> examples, List<String> constraints,
                   String starterCode, String starterCodeCpp, List<TestCase> testCases) {
        this.id = id;
        this.title = title;
        this.difficulty = difficulty;
        this.description = description;
        this.examples = examples != null ? examples : new ArrayList<>();
        this.constraints = constraints != null ? constraints : new ArrayList<>();
        this.starterCode = starterCode;
        this.starterCodeCpp = starterCodeCpp;
        this.testCases = testCases != null ? testCases : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    public List<String> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }

    public String getStarterCode() {
        return starterCode;
    }

    public void setStarterCode(String starterCode) {
        this.starterCode = starterCode;
    }

    public String getStarterCodeCpp() {
        return starterCodeCpp;
    }

    public void setStarterCodeCpp(String starterCodeCpp) {
        this.starterCodeCpp = starterCodeCpp;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCase> testCases) {
        this.testCases = testCases;
    }
}
