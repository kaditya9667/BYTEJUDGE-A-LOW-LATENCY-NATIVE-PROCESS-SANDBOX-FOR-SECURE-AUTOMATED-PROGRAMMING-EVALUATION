package com.judge.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Record of a past user submission for history tracking and performance analytics.
 */
public class SubmissionRecord {
    private String id;
    private String problemId;
    private String language;
    private String code;
    private Verdict verdict;
    private int passedTests;
    private int totalTests;
    private long runtimeMs;
    private String submittedAt;

    public SubmissionRecord() {
        this.id = UUID.randomUUID().toString();
        this.submittedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss"));
    }

    public SubmissionRecord(String problemId, String language, String code,
                            Verdict verdict, int passedTests, int totalTests, long runtimeMs) {
        this();
        this.problemId = problemId;
        this.language = language != null ? language : "java";
        this.code = code;
        this.verdict = verdict;
        this.passedTests = passedTests;
        this.totalTests = totalTests;
        this.runtimeMs = runtimeMs;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProblemId() { return problemId; }
    public void setProblemId(String problemId) { this.problemId = problemId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Verdict getVerdict() { return verdict; }
    public void setVerdict(Verdict verdict) { this.verdict = verdict; }

    public int getPassedTests() { return passedTests; }
    public void setPassedTests(int passedTests) { this.passedTests = passedTests; }

    public int getTotalTests() { return totalTests; }
    public void setTotalTests(int totalTests) { this.totalTests = totalTests; }

    public long getRuntimeMs() { return runtimeMs; }
    public void setRuntimeMs(long runtimeMs) { this.runtimeMs = runtimeMs; }

    public String getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }
}
