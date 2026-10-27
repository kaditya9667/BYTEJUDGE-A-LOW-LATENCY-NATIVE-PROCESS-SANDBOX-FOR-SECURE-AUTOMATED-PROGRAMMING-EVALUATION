package com.judge.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Result payload containing the repaired Java code and list of applied fixes.
 */
public class AutoFixResponse {

    private boolean fixed;
    private String fixedCode;
    private List<String> fixes = new ArrayList<>();
    private String message;

    public AutoFixResponse() {}

    public AutoFixResponse(boolean fixed, String fixedCode, List<String> fixes, String message) {
        this.fixed = fixed;
        this.fixedCode = fixedCode;
        this.fixes = fixes != null ? fixes : new ArrayList<>();
        this.message = message;
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public String getFixedCode() {
        return fixedCode;
    }

    public void setFixedCode(String fixedCode) {
        this.fixedCode = fixedCode;
    }

    public List<String> getFixes() {
        return fixes;
    }

    public void setFixes(List<String> fixes) {
        this.fixes = fixes;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
