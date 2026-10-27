package com.judge.service;

import com.judge.model.CompilerRequest;
import com.judge.model.CompilerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interactive Online Compiler Sandbox Service for Java and C++.
 * Features security policy analysis, isolated temporary workspace,
 * stream draining, 5-second watchdog timeouts, and JVM memory caps.
 */
@Service
public class OnlineCompilerService {

    private static final Logger log = LoggerFactory.getLogger(OnlineCompilerService.class);
    private static final long TIME_LIMIT_SECONDS = 5L;
    private static final int MAX_OUTPUT_BYTES = 128 * 1024; // 128 KB buffer limit

    private static final Pattern PUBLIC_CLASS_PATTERN = Pattern.compile("public\\s+class\\s+([A-Za-z0-9_$]+)");
    private static final Pattern ANY_CLASS_PATTERN = Pattern.compile("class\\s+([A-Za-z0-9_$]+)");

    private final SecurityValidator securityValidator;
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();
    private final String javacPath;
    private final String javaPath;
    private final String gppPath;

    public OnlineCompilerService(SecurityValidator securityValidator) {
        this.securityValidator = securityValidator;
        this.javacPath = resolveBinary("javac");
        this.javaPath = resolveBinary("java");
        this.gppPath = resolveGpp();
        log.info("Online Compiler initialized with javac: {}, java: {}, g++: {}", javacPath, javaPath, gppPath);
    }

    private String resolveBinary(String binaryName) {
        Path jdkBin = Paths.get(System.getProperty("java.home"), "bin");
        boolean isWin = isWindows();
        Path exe = jdkBin.resolve(isWin ? binaryName + ".exe" : binaryName);
        if (Files.exists(exe)) {
            return exe.toAbsolutePath().toString();
        }
        return binaryName;
    }

    private String resolveGpp() {
        return isWindows() ? "g++.exe" : "g++";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Executes arbitrary user code in Java or C++ with optional standard input.
     */
    public CompilerResponse execute(CompilerRequest request) {
        String code = request.getCode();
        if (code == null || code.trim().isEmpty()) {
            return new CompilerResponse("RUNTIME_ERROR", "", "Error: Code cannot be empty.", 0, 1, "Empty source code");
        }

        String language = (request.getLanguage() != null ? request.getLanguage().toLowerCase().trim() : "java");

        // 1. Security static check
        SecurityValidator.SecurityCheckResult sec = securityValidator.validate(code, language);
        if (!sec.passed()) {
            return new CompilerResponse(
                    "SECURITY_VIOLATION",
                    "",
                    sec.violationMessage(),
                    0,
                    1,
                    "Security Violation: Sandbox Disallowed Operation"
            );
        }

        boolean isCpp = language.contains("cpp") || language.contains("c++");
        Path sandboxDir = null;

        try {
            // 2. Create isolated temporary directory
            sandboxDir = Files.createTempDirectory("compiler_sandbox_");
            Path sourceFile;
            ProcessBuilder compilePb;
            ProcessBuilder runPb;

            if (isCpp) {
                sourceFile = sandboxDir.resolve("solution.cpp");
                Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

                String outExe = isWindows() ? "solution.exe" : "solution";
                compilePb = new ProcessBuilder(gppPath, "-O3", "-std=c++20", sourceFile.getFileName().toString(), "-o", outExe);
                compilePb.directory(sandboxDir.toFile());
                compilePb.redirectErrorStream(true);

                Path exePath = sandboxDir.resolve(outExe);
                runPb = new ProcessBuilder(exePath.toAbsolutePath().toString());
                runPb.directory(sandboxDir.toFile());
            } else {
                String className = extractClassName(code);
                sourceFile = sandboxDir.resolve(className + ".java");
                Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

                compilePb = new ProcessBuilder(javacPath, sourceFile.getFileName().toString());
                compilePb.directory(sandboxDir.toFile());
                compilePb.redirectErrorStream(true);

                runPb = new ProcessBuilder(
                        javaPath,
                        "-Xmx64M",
                        "-Dfile.encoding=UTF-8",
                        "-cp", ".",
                        className
                );
                runPb.directory(sandboxDir.toFile());
            }

            // 3. Compilation phase
            Process compileProcess = compilePb.start();
            ByteArrayOutputStream compileBuffer = new ByteArrayOutputStream();

            CompletableFuture<Void> compileDrainer = CompletableFuture.runAsync(() -> {
                try (InputStream is = compileProcess.getInputStream()) {
                    is.transferTo(compileBuffer);
                } catch (IOException ignored) {}
            }, ioExecutor);

            boolean compileFinished = compileProcess.waitFor(7, TimeUnit.SECONDS);
            if (!compileFinished) {
                compileProcess.destroyForcibly();
                return CompilerResponse.compilationError("Compilation timed out after 7 seconds.");
            }

            compileDrainer.get(500, TimeUnit.MILLISECONDS);
            int compileExit = compileProcess.exitValue();
            if (compileExit != 0) {
                String errorLog = compileBuffer.toString(StandardCharsets.UTF_8).trim();
                return CompilerResponse.compilationError(errorLog.isEmpty() ? "Compilation failed with unknown error." : errorLog);
            }

            // 4. Execution phase
            long runStart = System.currentTimeMillis();
            final Process runProcess = runPb.start();

            ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrBuffer = new ByteArrayOutputStream();

            // Non-blocking async stdout drainer
            CompletableFuture<Void> stdoutDrainer = CompletableFuture.runAsync(() -> {
                try (InputStream is = runProcess.getInputStream()) {
                    byte[] buf = new byte[1024];
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        if (stdoutBuffer.size() < MAX_OUTPUT_BYTES) {
                            stdoutBuffer.write(buf, 0, n);
                        }
                    }
                } catch (IOException ignored) {}
            }, ioExecutor);

            // Non-blocking async stderr drainer
            CompletableFuture<Void> stderrDrainer = CompletableFuture.runAsync(() -> {
                try (InputStream is = runProcess.getErrorStream()) {
                    byte[] buf = new byte[1024];
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        if (stderrBuffer.size() < MAX_OUTPUT_BYTES) {
                            stderrBuffer.write(buf, 0, n);
                        }
                    }
                } catch (IOException ignored) {}
            }, ioExecutor);

            // Feed user standard input if provided
            try (OutputStream os = runProcess.getOutputStream()) {
                if (request.getStdin() != null && !request.getStdin().isEmpty()) {
                    os.write(request.getStdin().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            } catch (IOException e) {
                log.debug("Process closed stdin pipe early: {}", e.getMessage());
            }

            // 5. Watchdog enforcement (5.0s timeout)
            boolean finishedInTime = runProcess.waitFor(TIME_LIMIT_SECONDS, TimeUnit.SECONDS);
            long executionTimeMs = System.currentTimeMillis() - runStart;

            if (!finishedInTime) {
                runProcess.destroyForcibly();
                runProcess.waitFor(500, TimeUnit.MILLISECONDS);
                stdoutDrainer.cancel(true);
                stderrDrainer.cancel(true);
                return CompilerResponse.timeLimitExceeded(TIME_LIMIT_SECONDS * 1000, stdoutBuffer.toString(StandardCharsets.UTF_8));
            }

            try {
                stdoutDrainer.get(300, TimeUnit.MILLISECONDS);
                stderrDrainer.get(300, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {}

            int exitCode = runProcess.exitValue();
            String stdout = stdoutBuffer.toString(StandardCharsets.UTF_8);
            String stderr = stderrBuffer.toString(StandardCharsets.UTF_8);

            if (exitCode != 0) {
                return new CompilerResponse(
                        "RUNTIME_ERROR",
                        stdout,
                        stderr.trim().isEmpty() ? "Process exited with code " + exitCode : stderr,
                        executionTimeMs,
                        exitCode,
                        "Runtime Error (Exit Code " + exitCode + ")"
                );
            }

            return new CompilerResponse(
                    "SUCCESS",
                    stdout,
                    stderr,
                    executionTimeMs,
                    0,
                    String.format("Program finished in %d ms (Exit Code 0)", executionTimeMs)
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CompilerResponse("RUNTIME_ERROR", "", "Execution interrupted: " + e.getMessage(), 0, 1, "Interrupted");
        } catch (Exception e) {
            log.error("Internal compiler error", e);
            return new CompilerResponse("RUNTIME_ERROR", "", "System execution error: " + e.getMessage(), 0, 1, e.getMessage());
        } finally {
            cleanupSandbox(sandboxDir);
        }
    }

    private String extractClassName(String code) {
        Matcher publicMatcher = PUBLIC_CLASS_PATTERN.matcher(code);
        if (publicMatcher.find()) {
            return publicMatcher.group(1);
        }
        Matcher anyMatcher = ANY_CLASS_PATTERN.matcher(code);
        if (anyMatcher.find()) {
            return anyMatcher.group(1);
        }
        return "Main";
    }

    private void cleanupSandbox(Path sandboxDir) {
        if (sandboxDir == null || !Files.exists(sandboxDir)) return;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Files.walkFileTree(sandboxDir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.deleteIfExists(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.deleteIfExists(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                return;
            } catch (Exception e) {
                try {
                    Thread.sleep(80);
                } catch (InterruptedException ignored) {}
            }
        }
    }
}
