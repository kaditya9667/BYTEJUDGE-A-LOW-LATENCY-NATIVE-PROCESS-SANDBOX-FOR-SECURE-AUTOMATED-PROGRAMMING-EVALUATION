package com.judge.controller;

import com.judge.model.CompilerRequest;
import com.judge.model.CompilerResponse;
import com.judge.service.OnlineCompilerService;
import com.judge.service.AutoFixService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for interactive Programiz-style online code compilation
 * and code compaction.
 */
@RestController
@RequestMapping("/api/compiler")
public class CompilerController {

    private final OnlineCompilerService compilerService;
    private final AutoFixService autoFixService;

    public CompilerController(OnlineCompilerService compilerService, AutoFixService autoFixService) {
        this.compilerService = compilerService;
        this.autoFixService = autoFixService;
    }

    /**
     * Executes arbitrary Java code with optional standard input (stdin).
     */
    @PostMapping("/run")
    public ResponseEntity<CompilerResponse> runCode(@RequestBody CompilerRequest request) {
        CompilerResponse response = compilerService.execute(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Formats and removes all empty/blank lines between codes.
     */
    @PostMapping("/format")
    public ResponseEntity<Map<String, String>> formatCode(@RequestBody Map<String, String> body) {
        String code = body.getOrDefault("code", "");
        String formatted = autoFixService.formatAndCompact(code);
        return ResponseEntity.ok(Map.of("formattedCode", formatted));
    }
}
