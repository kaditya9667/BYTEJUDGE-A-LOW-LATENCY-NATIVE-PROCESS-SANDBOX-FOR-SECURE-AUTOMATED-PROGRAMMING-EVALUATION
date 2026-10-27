package com.judge.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Static security analyzer to prevent Remote Code Execution (RCE),
 * file system traversal, socket network communication, and fork bombs.
 */
@Service
public class SecurityValidator {

    public record SecurityCheckResult(boolean passed, String violationMessage) {}

    // Java forbidden patterns
    private static final List<Rule> JAVA_RULES = List.of(
            new Rule(Pattern.compile("\\bSystem\\.exit\\s*\\("), "Use of System.exit() is disallowed in the sandbox."),
            new Rule(Pattern.compile("\\bRuntime\\.getRuntime\\s*\\("), "Direct access to OS Runtime is disallowed."),
            new Rule(Pattern.compile("\\bProcessBuilder\\b"), "ProcessBuilder execution is disallowed."),
            new Rule(Pattern.compile("\\bjava\\.lang\\.reflect\\b"), "Reflection is disallowed in the sandbox."),
            new Rule(Pattern.compile("\\bjava\\.net\\b|\\bSocket\\b|\\bServerSocket\\b|\\bHttpURLConnection\\b"), "Network socket communication is disallowed."),
            new Rule(Pattern.compile("\\bjava\\.io\\.File\\b|\\bjava\\.nio\\.file\\b|\\bFileOutputStream\\b|\\bFileInputStream\\b|\\bRandomAccessFile\\b"), "Direct file system disk access is disallowed.")
    );

    // C++ forbidden patterns
    private static final List<Rule> CPP_RULES = List.of(
            new Rule(Pattern.compile("\\bsystem\\s*\\("), "Calling system() is disallowed in C++ sandbox."),
            new Rule(Pattern.compile("\\b(fork|execvp|execl|popen)\\s*\\("), "Process spawning functions are disallowed."),
            new Rule(Pattern.compile("#\\s*include\\s*<\\s*(windows\\.h|winsock2\\.h|sys/socket\\.h|arpa/inet\\.h)\\s*>"), "Platform/network OS headers are disallowed."),
            new Rule(Pattern.compile("\\b(remove|rename)\\s*\\("), "Direct file modification functions are disallowed.")
    );

    private record Rule(Pattern pattern, String reason) {}

    public SecurityCheckResult validate(String code, String language) {
        if (code == null || code.isBlank()) {
            return new SecurityCheckResult(true, null);
        }

        String lang = (language == null ? "java" : language).toLowerCase().trim();
        List<Rule> rules = lang.contains("cpp") || lang.contains("c++") ? CPP_RULES : JAVA_RULES;

        List<String> violations = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.pattern().matcher(code).find()) {
                violations.add(rule.reason());
            }
        }

        if (!violations.isEmpty()) {
            return new SecurityCheckResult(false, "Security Policy Violation: " + String.join(" ", violations));
        }

        return new SecurityCheckResult(true, null);
    }
}
