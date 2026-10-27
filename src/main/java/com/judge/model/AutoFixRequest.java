package com.judge.model;

/**
 * Payload for automated code repair / quick-fix requests.
 */
public class AutoFixRequest {

    private String code;
    private String stderr;

    public AutoFixRequest() {}

    public AutoFixRequest(String code, String stderr) {
        this.code = code;
        this.stderr = stderr;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }
}
