package com.judge.model;

/**
 * Request payload for compiling and running solution code against problem test cases.
 */
public class ExecutionRequest {
    private String problemId;
    private String code;
    private boolean isSubmit; // false for "Run Sample Tests", true for "Submit Solution"

    private String language = "java"; // "java" or "cpp"
    private String customInput; // Optional custom stdin for testing arbitrary inputs

    public ExecutionRequest() {}

    public ExecutionRequest(String problemId, String code, boolean isSubmit) {
        this.problemId = problemId;
        this.code = code;
        this.isSubmit = isSubmit;
    }

    public ExecutionRequest(String problemId, String code, boolean isSubmit, String customInput) {
        this.problemId = problemId;
        this.code = code;
        this.isSubmit = isSubmit;
        this.customInput = customInput;
    }

    public String getProblemId() {
        return problemId;
    }

    public void setProblemId(String problemId) {
        this.problemId = problemId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isSubmit() {
        return isSubmit;
    }

    public void setSubmit(boolean submit) {
        this.isSubmit = submit;
    }

    public void setIsSubmit(boolean isSubmit) {
        this.isSubmit = isSubmit;
    }

    public String getLanguage() {
        return language != null ? language : "java";
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCustomInput() {
        return customInput;
    }

    public void setCustomInput(String customInput) {
        this.customInput = customInput;
    }
}
