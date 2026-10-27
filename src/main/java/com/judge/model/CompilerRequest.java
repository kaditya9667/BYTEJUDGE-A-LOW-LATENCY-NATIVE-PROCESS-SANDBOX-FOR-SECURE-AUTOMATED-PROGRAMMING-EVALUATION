package com.judge.model;

/**
 * Request payload for the interactive online compiler.
 */
public class CompilerRequest {
    private String code;
    private String stdin;
    private String language = "java";

    public CompilerRequest() {}

    public CompilerRequest(String code, String stdin) {
        this.code = code;
        this.stdin = stdin;
        this.language = "java";
    }

    public CompilerRequest(String code, String stdin, String language) {
        this.code = code;
        this.stdin = stdin;
        this.language = language != null ? language : "java";
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStdin() {
        return stdin;
    }

    public void setStdin(String stdin) {
        this.stdin = stdin;
    }

    public String getLanguage() {
        return language != null ? language : "java";
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
