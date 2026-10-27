package com.judge.service;

import com.judge.model.*;
import com.judge.repository.ProblemRepository;
import com.judge.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance Online Judge Sandbox Engine for Java and C++.
 * Features security policy inspection, method harness auto-wrapping,
 * real-time SSE streaming, memory caps, and runtime percentile tracking.
 */
@Service
public class CodeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutionService.class);
    private static final long TIME_LIMIT_SECONDS = 5L;
    private static final int MAX_OUTPUT_BYTES = 64 * 1024; // 64 KB

    private static final Pattern PUBLIC_CLASS_PATTERN = Pattern.compile("public\\s+class\\s+([A-Za-z0-9_$]+)");
    private static final Pattern ANY_CLASS_PATTERN = Pattern.compile("class\\s+([A-Za-z0-9_$]+)");

    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final SecurityValidator securityValidator;
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();

    private final String javacPath;
    private final String javaPath;
    private final String gppPath;

    public CodeExecutionService(ProblemRepository problemRepository,
                                SubmissionRepository submissionRepository,
                                SecurityValidator securityValidator) {
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.securityValidator = securityValidator;
        this.javacPath = resolveJavacPath();
        this.javaPath = resolveJavaPath();
        this.gppPath = resolveGppPath();
        log.info("Initialized Judge with javac: {}, java: {}, g++: {}", javacPath, javaPath, gppPath);
    }

    private String resolveJavacPath() {
        Path jdkBin = Paths.get(System.getProperty("java.home"), "bin");
        Path javacExe = jdkBin.resolve(isWindows() ? "javac.exe" : "javac");
        return Files.exists(javacExe) ? javacExe.toAbsolutePath().toString() : "javac";
    }

    private String resolveJavaPath() {
        Path jdkBin = Paths.get(System.getProperty("java.home"), "bin");
        Path javaExe = jdkBin.resolve(isWindows() ? "java.exe" : "java");
        return Files.exists(javaExe) ? javaExe.toAbsolutePath().toString() : "java";
    }

    private String resolveGppPath() {
        return isWindows() ? "g++.exe" : "g++";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Synchronous submission execution.
     */
    public ExecutionResult execute(ExecutionRequest request) {
        return executeInternal(request, null);
    }

    /**
     * Real-time Server-Sent Events (SSE) streaming execution.
     */
    public void executeStreaming(ExecutionRequest request, SseEmitter emitter) {
        ioExecutor.submit(() -> {
            try {
                executeInternal(request, caseResult -> {
                    try {
                        emitter.send(SseEmitter.event().name("case").data(caseResult));
                    } catch (IOException e) {
                        log.debug("SSE client disconnected: {}", e.getMessage());
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
    }

    private ExecutionResult executeInternal(ExecutionRequest request, Consumer<TestCaseResult> onCaseFinished) {
        String language = (request.getLanguage() != null ? request.getLanguage().toLowerCase().trim() : "java");
        boolean isCpp = language.contains("cpp") || language.contains("c++");

        // 1. Static Security Check
        SecurityValidator.SecurityCheckResult sec = securityValidator.validate(request.getCode(), language);
        if (!sec.passed()) {
            ExecutionResult secRes = ExecutionResult.securityViolation(sec.violationMessage());
            if (onCaseFinished != null) {
                try {
                    // Send error indication to SSE
                    onCaseFinished.accept(new TestCaseResult(1, false, Verdict.SECURITY_VIOLATION, 0, null, null, null, sec.violationMessage()));
                } catch (Exception ignored) {}
            }
            return secRes;
        }

        Optional<Problem> problemOpt = problemRepository.findById(request.getProblemId());
        if (problemOpt.isEmpty()) {
            ExecutionResult res = new ExecutionResult();
            res.setOverallVerdict(Verdict.RUNTIME_ERROR);
            res.setMessage("Problem not found: " + request.getProblemId());
            return res;
        }

        Problem problem = problemOpt.get();
        List<TestCase> allTests = problem.getTestCases();
        List<TestCase> testCasesToRun = new ArrayList<>();

        boolean isCustomRun = !request.isSubmit() && request.getCustomInput() != null && !request.getCustomInput().trim().isEmpty();

        if (isCustomRun) {
            testCasesToRun.add(new TestCase(request.getCustomInput().trim(), "", false));
        } else {
            for (TestCase tc : allTests) {
                if (request.isSubmit() || !tc.isHidden()) {
                    testCasesToRun.add(tc);
                }
            }
        }

        Path sandboxDir = null;
        try {
            // 2. Prepare Sandbox Directory
            sandboxDir = Files.createTempDirectory("oj_sandbox_");
            String codeToCompile = prepareHarness(request.getProblemId(), request.getCode(), isCpp);

            Path sourceFile;
            ProcessBuilder compilePb;
            String executableTarget;

            if (isCpp) {
                sourceFile = sandboxDir.resolve("solution.cpp");
                Files.writeString(sourceFile, codeToCompile, StandardCharsets.UTF_8);

                executableTarget = isWindows() ? "solution.exe" : "solution";
                compilePb = new ProcessBuilder(gppPath, "-O3", "-std=c++20", sourceFile.getFileName().toString(), "-o", executableTarget);
                compilePb.directory(sandboxDir.toFile());
                compilePb.redirectErrorStream(true);
            } else {
                String className = extractClassName(codeToCompile);
                executableTarget = className;
                sourceFile = sandboxDir.resolve(className + ".java");
                Files.writeString(sourceFile, codeToCompile, StandardCharsets.UTF_8);

                compilePb = new ProcessBuilder(javacPath, sourceFile.getFileName().toString());
                compilePb.directory(sandboxDir.toFile());
                compilePb.redirectErrorStream(true);
            }

            // 3. Compile User Code
            CompilationResult compilation = compileSource(sandboxDir, compilePb);
            if (!compilation.success) {
                ExecutionResult ceResult = ExecutionResult.compilationError(compilation.output);
                if (onCaseFinished != null) {
                    onCaseFinished.accept(new TestCaseResult(1, false, Verdict.COMPILATION_ERROR, 0, null, null, null, compilation.output));
                }
                return ceResult;
            }

            // 4. Run Test Cases
            List<TestCaseResult> caseResults = new ArrayList<>();
            Verdict overallVerdict = Verdict.ACCEPTED;
            int passedTests = 0;
            long totalExecutionTime = 0;
            String firstFailureMessage = null;

            for (int i = 0; i < testCasesToRun.size(); i++) {
                TestCase tc = testCasesToRun.get(i);
                TestCaseResult caseResult = runSingleTestCase(sandboxDir, tc, i + 1, executableTarget, isCpp);
                caseResults.add(caseResult);
                totalExecutionTime += caseResult.getExecutionTimeMs();

                if (onCaseFinished != null) {
                    onCaseFinished.accept(caseResult);
                }

                if (caseResult.getStatus() == Verdict.ACCEPTED) {
                    passedTests++;
                } else {
                    if (overallVerdict == Verdict.ACCEPTED) {
                        overallVerdict = caseResult.getStatus();
                        firstFailureMessage = String.format("%s on Test Case %d",
                                formatVerdictName(caseResult.getStatus()), i + 1);
                    }
                    if (caseResult.getStatus() == Verdict.TIME_LIMIT_EXCEEDED || request.isSubmit()) {
                        break;
                    }
                }
            }

            String summaryMessage;
            if (overallVerdict == Verdict.ACCEPTED) {
                summaryMessage = String.format("Accepted: Passed all %d/%d test cases in %d ms",
                        passedTests, testCasesToRun.size(), totalExecutionTime);
            } else {
                summaryMessage = String.format("%s (%d/%d tests passed)",
                        firstFailureMessage, passedTests, testCasesToRun.size());
            }

            ExecutionResult result = new ExecutionResult(
                    overallVerdict,
                    testCasesToRun.size(),
                    passedTests,
                    totalExecutionTime,
                    null,
                    summaryMessage,
                    caseResults
            );

            // Record submission and calculate percentile
            if (request.isSubmit() && overallVerdict == Verdict.ACCEPTED) {
                double percentile = submissionRepository.calculatePercentile(request.getProblemId(), language, totalExecutionTime);
                result.setPercentileBeaten(percentile);
            }

            if (request.isSubmit()) {
                submissionRepository.save(new SubmissionRecord(
                        request.getProblemId(),
                        language,
                        request.getCode(),
                        overallVerdict,
                        passedTests,
                        testCasesToRun.size(),
                        totalExecutionTime
                ));
            }

            return result;

        } catch (IOException e) {
            log.error("Internal sandbox I/O error", e);
            ExecutionResult res = new ExecutionResult();
            res.setOverallVerdict(Verdict.RUNTIME_ERROR);
            res.setMessage("Internal Sandbox Error: " + e.getMessage());
            return res;
        } finally {
            cleanUpSandbox(sandboxDir);
        }
    }

    /**
     * If user submitted pure LeetCode class/methods without main(), appends driver harness automatically.
     */
    private String prepareHarness(String problemId, String code, boolean isCpp) {
        if (code == null) return "";
        if (code.contains("main(") || code.contains("main (")) {
            return code; // User provided full driver with main()
        }

        // Auto-wrap based on problemId
        if (isCpp) {
            return appendCppHarness(problemId, code);
        } else {
            return appendJavaHarness(problemId, code);
        }
    }

    private String appendJavaHarness(String problemId, String code) {
        String driver = switch (problemId) {
            case "two-sum" -> """
class MainDriver {
    public static void main(String[] args) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();
        int target = sc.nextInt();
        Solution sol = new Solution();
        int[] res = sol.twoSum(nums, target);
        System.out.println(res[0] + " " + res[1]);
    }
}
""";
            case "valid-parentheses" -> """
class MainDriver {
    public static void main(String[] args) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        if (!sc.hasNext()) return;
        String s = sc.next();
        Solution sol = new Solution();
        System.out.println(sol.isValid(s));
    }
}
""";
            case "palindrome-number" -> """
class MainDriver {
    public static void main(String[] args) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        if (!sc.hasNextLong()) return;
        long x = sc.nextLong();
        Solution sol = new Solution();
        System.out.println(sol.isPalindrome((int) x));
    }
}
""";
            case "maximum-subarray" -> """
class MainDriver {
    public static void main(String[] args) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();
        Solution sol = new Solution();
        System.out.println(sol.maxSubArray(nums));
    }
}
""";
            case "climbing-stairs" -> """
class MainDriver {
    public static void main(String[] args) {
        java.util.Scanner sc = new java.util.Scanner(System.in);
        if (!sc.hasNextInt()) return;
        int n = sc.nextInt();
        Solution sol = new Solution();
        System.out.println(sol.climbStairs(n));
    }
}
""";
            default -> "";
        };

        return code + "\n\n" + driver;
    }

    private String appendCppHarness(String problemId, String code) {
        String driver = switch (problemId) {
            case "two-sum" -> """
int main() {
    int n;
    if (!(std::cin >> n)) return 0;
    std::vector<int> nums(n);
    for (int i = 0; i < n; i++) std::cin >> nums[i];
    int target;
    std::cin >> target;
    Solution sol;
    std::vector<int> res = sol.twoSum(nums, target);
    std::cout << res[0] << " " << res[1] << std::endl;
    return 0;
}
""";
            case "valid-parentheses" -> """
int main() {
    std::string s;
    if (!(std::cin >> s)) return 0;
    Solution sol;
    std::cout << (sol.isValid(s) ? "true" : "false") << std::endl;
    return 0;
}
""";
            case "palindrome-number" -> """
int main() {
    long long x;
    if (!(std::cin >> x)) return 0;
    Solution sol;
    std::cout << (sol.isPalindrome(x) ? "true" : "false") << std::endl;
    return 0;
}
""";
            case "maximum-subarray" -> """
int main() {
    int n;
    if (!(std::cin >> n)) return 0;
    std::vector<int> nums(n);
    for (int i = 0; i < n; i++) std::cin >> nums[i];
    Solution sol;
    std::cout << sol.maxSubArray(nums) << std::endl;
    return 0;
}
""";
            case "climbing-stairs" -> """
int main() {
    int n;
    if (!(std::cin >> n)) return 0;
    Solution sol;
    std::cout << sol.climbStairs(n) << std::endl;
    return 0;
}
""";
            default -> "";
        };

        return code + "\n\n" + driver;
    }

    private static class CompilationResult {
        final boolean success;
        final String output;

        CompilationResult(boolean success, String output) {
            this.success = success;
            this.output = output;
        }
    }

    private CompilationResult compileSource(Path sandboxDir, ProcessBuilder pb) {
        try {
            Process process = pb.start();
            ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

            CompletableFuture<Void> readTask = CompletableFuture.runAsync(() -> {
                try (InputStream is = process.getInputStream()) {
                    byte[] buf = new byte[1024];
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        if (outputBuffer.size() < MAX_OUTPUT_BYTES) {
                            outputBuffer.write(buf, 0, n);
                        }
                    }
                } catch (IOException ignored) {}
            }, ioExecutor);

            boolean finished = process.waitFor(7, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CompilationResult(false, "Compilation timed out after 7 seconds.");
            }

            readTask.get(500, TimeUnit.MILLISECONDS);
            int exitCode = process.exitValue();
            String output = outputBuffer.toString(StandardCharsets.UTF_8).trim();

            if (exitCode != 0) {
                return new CompilationResult(false, output.isEmpty() ? "Compilation failed with unknown error." : output);
            }

            return new CompilationResult(true, output);

        } catch (Exception e) {
            log.error("Compilation execution error", e);
            return new CompilationResult(false, "Compilation system error: " + e.getMessage());
        }
    }

    private TestCaseResult runSingleTestCase(Path sandboxDir, TestCase testCase, int testIndex, String executableTarget, boolean isCpp) {
        ProcessBuilder pb;
        if (isCpp) {
            Path exe = sandboxDir.resolve(executableTarget);
            pb = new ProcessBuilder(exe.toAbsolutePath().toString());
        } else {
            pb = new ProcessBuilder(
                    javaPath,
                    "-Xmx64M",
                    "-Dfile.encoding=UTF-8",
                    "-cp", ".",
                    executableTarget
            );
        }
        pb.directory(sandboxDir.toFile());

        long startTime = System.currentTimeMillis();
        Process process = null;

        try {
            final Process activeProcess = pb.start();
            process = activeProcess;

            ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrBuffer = new ByteArrayOutputStream();

            CompletableFuture<Void> stdoutDrainer = CompletableFuture.runAsync(() -> {
                try (InputStream is = activeProcess.getInputStream()) {
                    byte[] buf = new byte[1024];
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        if (stdoutBuffer.size() < MAX_OUTPUT_BYTES) {
                            stdoutBuffer.write(buf, 0, n);
                        }
                    }
                } catch (IOException ignored) {}
            }, ioExecutor);

            CompletableFuture<Void> stderrDrainer = CompletableFuture.runAsync(() -> {
                try (InputStream is = activeProcess.getErrorStream()) {
                    byte[] buf = new byte[1024];
                    int n;
                    while ((n = is.read(buf)) != -1) {
                        if (stderrBuffer.size() < MAX_OUTPUT_BYTES) {
                            stderrBuffer.write(buf, 0, n);
                        }
                    }
                } catch (IOException ignored) {}
            }, ioExecutor);

            try (OutputStream os = process.getOutputStream()) {
                if (testCase.getInput() != null) {
                    os.write(testCase.getInput().getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            } catch (IOException e) {
                log.debug("Process closed stdin pipe early: {}", e.getMessage());
            }

            boolean finishedInTime = process.waitFor(TIME_LIMIT_SECONDS, TimeUnit.SECONDS);
            long executionTimeMs = System.currentTimeMillis() - startTime;

            if (!finishedInTime) {
                process.destroyForcibly();
                process.waitFor(500, TimeUnit.MILLISECONDS);
                stdoutDrainer.cancel(true);
                stderrDrainer.cancel(true);

                return new TestCaseResult(
                        testIndex,
                        testCase.isHidden(),
                        Verdict.TIME_LIMIT_EXCEEDED,
                        TIME_LIMIT_SECONDS * 1000,
                        testCase.isHidden() ? "[Hidden]" : testCase.getInput(),
                        testCase.isHidden() ? "[Hidden]" : testCase.getExpectedOutput(),
                        "Time Limit Exceeded (> " + TIME_LIMIT_SECONDS + "s)",
                        "Execution exceeded the " + TIME_LIMIT_SECONDS + "-second time limit."
                );
            }

            try {
                stdoutDrainer.get(300, TimeUnit.MILLISECONDS);
                stderrDrainer.get(300, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {}

            int exitCode = process.exitValue();
            String stdout = stdoutBuffer.toString(StandardCharsets.UTF_8);
            String stderr = stderrBuffer.toString(StandardCharsets.UTF_8);

            if (exitCode != 0) {
                String errorMsg = stderr.trim();
                if (errorMsg.isEmpty()) {
                    errorMsg = "Process exited with non-zero exit code: " + exitCode;
                }
                return new TestCaseResult(
                        testIndex,
                        testCase.isHidden(),
                        Verdict.RUNTIME_ERROR,
                        executionTimeMs,
                        testCase.isHidden() ? "[Hidden]" : testCase.getInput(),
                        testCase.isHidden() ? "[Hidden]" : testCase.getExpectedOutput(),
                        stdout.trim(),
                        errorMsg
                );
            }

            String normalizedActual = normalizeOutput(stdout);
            String normalizedExpected = normalizeOutput(testCase.getExpectedOutput());

            Verdict verdict;
            if (testCase.getExpectedOutput() == null || testCase.getExpectedOutput().trim().isEmpty()) {
                verdict = Verdict.ACCEPTED;
            } else {
                boolean isMatch = normalizedActual.equals(normalizedExpected);
                verdict = isMatch ? Verdict.ACCEPTED : Verdict.WRONG_ANSWER;
            }

            return new TestCaseResult(
                    testIndex,
                    testCase.isHidden(),
                    verdict,
                    executionTimeMs,
                    testCase.isHidden() ? "[Hidden]" : testCase.getInput(),
                    testCase.isHidden() ? "[Hidden]" : testCase.getExpectedOutput(),
                    testCase.isHidden() ? "[Hidden]" : stdout.trim(),
                    stderr.trim()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) process.destroyForcibly();
            return new TestCaseResult(testIndex, testCase.isHidden(), Verdict.RUNTIME_ERROR,
                    System.currentTimeMillis() - startTime, null, null, null, "Execution thread interrupted");
        } catch (Exception e) {
            if (process != null) process.destroyForcibly();
            return new TestCaseResult(testIndex, testCase.isHidden(), Verdict.RUNTIME_ERROR,
                    System.currentTimeMillis() - startTime, null, null, null, "Execution error: " + e.getMessage());
        }
    }

    private String normalizeOutput(String raw) {
        if (raw == null) return "";
        String[] lines = raw.replace("\r\n", "\n").replace("\r", "\n").split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.stripTrailing();
            if (!trimmed.isEmpty()) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(trimmed);
            }
        }
        return sb.toString();
    }

    private String formatVerdictName(Verdict verdict) {
        return switch (verdict) {
            case ACCEPTED -> "Accepted";
            case WRONG_ANSWER -> "Wrong Answer";
            case TIME_LIMIT_EXCEEDED -> "Time Limit Exceeded";
            case RUNTIME_ERROR -> "Runtime Error";
            case COMPILATION_ERROR -> "Compilation Error";
            case SECURITY_VIOLATION -> "Security Violation";
        };
    }

    private String extractClassName(String code) {
        if (code == null) return "Solution";
        Matcher publicMatcher = PUBLIC_CLASS_PATTERN.matcher(code);
        if (publicMatcher.find()) {
            return publicMatcher.group(1);
        }
        Matcher anyMatcher = ANY_CLASS_PATTERN.matcher(code);
        if (anyMatcher.find()) {
            return anyMatcher.group(1);
        }
        return "Solution";
    }

    private void cleanUpSandbox(Path sandboxDir) {
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
