package com.judge.model;

/**
 * Execution output response returned by the interactive online compiler.
 */
public class CompilerResponse {
    private String status; // SUCCESS, COMPILATION_ERROR, TIME_LIMIT_EXCEEDED, RUNTIME_ERROR
    private String stdout;
    private String stderr;
    private long executionTimeMs;
    private int exitCode;
    private String message;

    public CompilerResponse() {}

    public CompilerResponse(String status, String stdout, String stderr, long executionTimeMs, int exitCode, String message) {
        this.status = status;
        this.stdout = stdout;
        this.stderr = stderr;
        this.executionTimeMs = executionTimeMs;
        this.exitCode = exitCode;
        this.message = message;
    }

    public static CompilerResponse compilationError(String compilerLog) {
        return new CompilerResponse(
                "COMPILATION_ERROR",
                "",
                compilerLog,
                0,
                1,
                "Compilation failed"
        );
    }

    public static CompilerResponse timeLimitExceeded(long timeMs, String stdout) {
        return new CompilerResponse(
                "TIME_LIMIT_EXCEEDED",
                stdout,
                "Execution timed out: Process was forcibly terminated by watchdog after " + (timeMs / 1000.0) + " seconds.",
                timeMs,
                137, // SIGKILL equivalent
                "Time Limit Exceeded (> " + (timeMs / 1000.0) + "s)"
        );
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
