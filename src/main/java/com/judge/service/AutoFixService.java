package com.judge.service;

import com.judge.model.AutoFixRequest;
import com.judge.model.AutoFixResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Code Repair Engine for Java.
 * Analyzes compiler diagnostic messages and AST/syntax patterns to automatically
 * repair missing semicolons, missing imports, unclosed braces, incompatible types,
 * casing typos, and cleans up empty space lines.
 */
@Service
public class AutoFixService {

    private static final Logger log = LoggerFactory.getLogger(AutoFixService.class);

    // Common standard library classes mapping to their import package
    private static final Map<String, String> COMMON_IMPORTS = new HashMap<>();
    static {
        COMMON_IMPORTS.put("Scanner", "java.util.Scanner");
        COMMON_IMPORTS.put("ArrayList", "java.util.ArrayList");
        COMMON_IMPORTS.put("List", "java.util.List");
        COMMON_IMPORTS.put("Arrays", "java.util.Arrays");
        COMMON_IMPORTS.put("Map", "java.util.Map");
        COMMON_IMPORTS.put("HashMap", "java.util.HashMap");
        COMMON_IMPORTS.put("Set", "java.util.Set");
        COMMON_IMPORTS.put("HashSet", "java.util.HashSet");
        COMMON_IMPORTS.put("Queue", "java.util.Queue");
        COMMON_IMPORTS.put("LinkedList", "java.util.LinkedList");
        COMMON_IMPORTS.put("Collections", "java.util.Collections");
        COMMON_IMPORTS.put("Stack", "java.util.Stack");
        COMMON_IMPORTS.put("PriorityQueue", "java.util.PriorityQueue");
        COMMON_IMPORTS.put("Random", "java.util.Random");
        COMMON_IMPORTS.put("Pattern", "java.util.regex.Pattern");
        COMMON_IMPORTS.put("Matcher", "java.util.regex.Matcher");
        COMMON_IMPORTS.put("BigDecimal", "java.math.BigDecimal");
        COMMON_IMPORTS.put("BigInteger", "java.math.BigInteger");
    }

    private static final Pattern SEMICOLON_ERR_PATTERN = Pattern.compile("(?:.*\\.java:)?(\\d+):\\s*error:\\s*';'\\s*expected");
    private static final Pattern INCOMPATIBLE_TYPE_PATTERN = Pattern.compile(
            "(?:.*\\.java:)?(\\d+):\\s*error:\\s*incompatible types:\\s*(?:java\\.lang\\.)?([A-Za-z0-9_<>]+)\\s*cannot be converted to\\s*(?:java\\.lang\\.)?([A-Za-z0-9_<>]+)"
    );
    private static final Pattern SYMBOL_CLASS_PATTERN = Pattern.compile("symbol:\\s*class\\s+([A-Za-z0-9_]+)");
    private static final Pattern UNCLOSED_STR_PATTERN = Pattern.compile("(?:.*\\.java:)?(\\d+):\\s*error:\\s*unclosed string literal");
    private static final Pattern MISSING_RETURN_PATTERN = Pattern.compile("(?:.*\\.java:)?(\\d+):\\s*error:\\s*missing return statement");
    private static final Pattern PUBLIC_CLASS_ERR = Pattern.compile("class\\s+([A-Za-z0-9_]+)\\s+is public, should be declared in a file named");

    public AutoFixResponse autoFix(AutoFixRequest request) {
        String code = request.getCode() != null ? request.getCode() : "";
        String stderr = request.getStderr() != null ? request.getStderr() : "";

        List<String> fixes = new ArrayList<>();
        String currentCode = code;

        // 1. Check for missing imports based on "cannot find symbol: class X"
        Matcher symMatcher = SYMBOL_CLASS_PATTERN.matcher(stderr);
        Set<String> addedImports = new HashSet<>();
        while (symMatcher.find()) {
            String className = symMatcher.group(1);
            if (COMMON_IMPORTS.containsKey(className)) {
                String fullImport = COMMON_IMPORTS.get(className);
                if (!currentCode.contains("import " + fullImport) && !addedImports.contains(fullImport)) {
                    currentCode = "import " + fullImport + ";\n" + currentCode;
                    addedImports.add(fullImport);
                    fixes.add("Added 'import " + fullImport + ";'");
                }
            }
        }

        // 2. Check for missing semicolons
        List<String> lines = new ArrayList<>(Arrays.asList(currentCode.split("\n", -1)));
        Matcher semiMatcher = SEMICOLON_ERR_PATTERN.matcher(stderr);
        Set<Integer> semiLinesFixed = new HashSet<>();
        while (semiMatcher.find()) {
            try {
                int lineNum = Integer.parseInt(semiMatcher.group(1));
                int idx = lineNum - 1;
                // If index is valid and not yet fixed
                if (idx >= 0 && idx < lines.size() && !semiLinesFixed.contains(idx)) {
                    String lineContent = lines.get(idx).trim();
                    if (!lineContent.isEmpty() && !lineContent.endsWith(";") && !lineContent.endsWith("{") && !lineContent.endsWith("}")) {
                        lines.set(idx, lines.get(idx) + ";");
                        semiLinesFixed.add(idx);
                        fixes.add("Added missing semicolon ';' on line " + lineNum);
                    } else if (idx > 0) {
                        // Might be previous line missing the semicolon
                        String prevContent = lines.get(idx - 1).trim();
                        if (!prevContent.isEmpty() && !prevContent.endsWith(";") && !prevContent.endsWith("{") && !prevContent.endsWith("}")) {
                            lines.set(idx - 1, lines.get(idx - 1) + ";");
                            semiLinesFixed.add(idx - 1);
                            fixes.add("Added missing semicolon ';' on line " + (lineNum - 1));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error parsing semicolon line: {}", e.getMessage());
            }
        }

        // 3. Check for incompatible types
        Matcher incompMatcher = INCOMPATIBLE_TYPE_PATTERN.matcher(stderr);
        while (incompMatcher.find()) {
            try {
                int lineNum = Integer.parseInt(incompMatcher.group(1));
                String actualType = incompMatcher.group(2); // e.g. String
                String declaredType = incompMatcher.group(3); // e.g. int
                int idx = lineNum - 1;
                if (idx >= 0 && idx < lines.size()) {
                    String line = lines.get(idx);
                    // Match pattern: declaredType varName =
                    Pattern declPat = Pattern.compile("\\b" + Pattern.quote(declaredType) + "\\b(\\s+[A-Za-z0-9_$]+\\s*=)");
                    Matcher dm = declPat.matcher(line);
                    if (dm.find()) {
                        String replaced = dm.replaceFirst(Matcher.quoteReplacement(actualType) + "$1");
                        lines.set(idx, replaced);
                        fixes.add("Changed variable type from '" + declaredType + "' to '" + actualType + "' on line " + lineNum);
                    }
                }
            } catch (Exception e) {
                log.warn("Error parsing incompatible types: {}", e.getMessage());
            }
        }

        // 4. Check for unclosed string literals
        Matcher unclosedStrMatcher = UNCLOSED_STR_PATTERN.matcher(stderr);
        while (unclosedStrMatcher.find()) {
            try {
                int lineNum = Integer.parseInt(unclosedStrMatcher.group(1));
                int idx = lineNum - 1;
                if (idx >= 0 && idx < lines.size()) {
                    String line = lines.get(idx);
                    int quoteCount = countOccurrences(line, '"');
                    if (quoteCount % 2 != 0) {
                        if (line.endsWith(";")) {
                            lines.set(idx, line.substring(0, line.length() - 1) + "\";");
                        } else {
                            lines.set(idx, line + "\"");
                        }
                        fixes.add("Closed unclosed string literal on line " + lineNum);
                    }
                }
            } catch (Exception e) {
                log.warn("Error parsing unclosed string: {}", e.getMessage());
            }
        }

        // 5. Check for missing return statement
        Matcher missingRetMatcher = MISSING_RETURN_PATTERN.matcher(stderr);
        while (missingRetMatcher.find()) {
            try {
                int lineNum = Integer.parseInt(missingRetMatcher.group(1));
                int idx = lineNum - 1;
                if (idx >= 0 && idx < lines.size()) {
                    // Insert a sensible default return before the method closing brace
                    lines.add(idx, "        return 0; // Auto-generated fallback return");
                    fixes.add("Added default 'return 0;' on line " + lineNum);
                }
            } catch (Exception e) {
                log.warn("Error parsing missing return: {}", e.getMessage());
            }
        }

        // Reassemble code after line-level fixes
        currentCode = String.join("\n", lines);

        // 6. Typo fixes: system.out -> System.out, printline / printLn -> println
        if (currentCode.contains("system.out.")) {
            currentCode = currentCode.replace("system.out.", "System.out.");
            fixes.add("Corrected 'system.out' casing to 'System.out'");
        }
        if (currentCode.contains(".printLn(")) {
            currentCode = currentCode.replace(".printLn(", ".println(");
            fixes.add("Corrected '.printLn()' typo to '.println()'");
        }
        if (currentCode.contains(".printline(")) {
            currentCode = currentCode.replace(".printline(", ".println(");
            fixes.add("Corrected '.printline()' typo to '.println()'");
        }
        if (currentCode.contains("math.max(") || currentCode.contains("math.min(")) {
            currentCode = currentCode.replace("math.max(", "Math.max(").replace("math.min(", "Math.min(");
            fixes.add("Corrected 'math' casing to 'Math'");
        }

        // 7. Check for missing closing braces (reached end of file while parsing)
        if (stderr.contains("reached end of file while parsing") || countOccurrences(currentCode, '{') > countOccurrences(currentCode, '}')) {
            int openBraces = countOccurrences(currentCode, '{');
            int closeBraces = countOccurrences(currentCode, '}');
            if (openBraces > closeBraces) {
                int diff = openBraces - closeBraces;
                StringBuilder sb = new StringBuilder(currentCode);
                for (int i = 0; i < diff; i++) {
                    sb.append("\n}");
                }
                currentCode = sb.toString();
                fixes.add("Appended " + diff + " missing closing brace(s) '}'");
            }
        }

        // 8. Public class clash check
        Matcher pubClassMatcher = PUBLIC_CLASS_ERR.matcher(stderr);
        if (pubClassMatcher.find()) {
            String offendingClass = pubClassMatcher.group(1);
            currentCode = currentCode.replaceFirst("public\\s+class\\s+" + Pattern.quote(offendingClass), "class " + offendingClass);
            fixes.add("Removed conflicting 'public' modifier from class '" + offendingClass + "'");
        }

        // 9. Format and eliminate empty blank space lines in between code
        String compactFormattedCode = formatAndCompact(currentCode);

        boolean wasFixed = !fixes.isEmpty();
        String message = wasFixed
                ? "Auto-fixed " + fixes.size() + " issue(s) successfully!"
                : "No standard syntax issues could be automatically resolved. Code was formatted.";

        return new AutoFixResponse(wasFixed, compactFormattedCode, fixes, message);
    }

    /**
     * Formats Java code and removes all blank lines between codes.
     */
    public String formatAndCompact(String code) {
        if (code == null || code.isBlank()) return "";
        String[] rawLines = code.split("\n");
        List<String> formatted = new ArrayList<>();
        int indent = 0;

        for (String raw : rawLines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                // Eliminate empty lines between codes
                continue;
            }

            // Count leading closing braces on this line
            int leadingCloses = 0;
            for (char c : line.toCharArray()) {
                if (c == '}' || c == ']' || c == ')') {
                    leadingCloses++;
                } else {
                    break;
                }
            }

            int currentIndent = Math.max(0, indent - leadingCloses);
            formatted.add("    ".repeat(currentIndent) + line);

            // Compute delta of braces, excluding strings
            String stripped = line.replaceAll("\"[^\"]*\"", "");
            int opens = countOccurrences(stripped, '{');
            int closes = countOccurrences(stripped, '}');
            indent = Math.max(0, indent + opens - closes);
        }

        return String.join("\n", formatted);
    }

    private int countOccurrences(String str, char ch) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == ch) count++;
        }
        return count;
    }
}
