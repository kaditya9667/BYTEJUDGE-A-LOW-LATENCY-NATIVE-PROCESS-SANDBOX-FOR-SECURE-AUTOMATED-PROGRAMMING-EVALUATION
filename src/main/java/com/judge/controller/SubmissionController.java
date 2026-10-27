package com.judge.controller;

import com.judge.model.ExecutionRequest;
import com.judge.model.ExecutionResult;
import com.judge.model.SubmissionRecord;
import com.judge.repository.SubmissionRepository;
import com.judge.service.CodeExecutionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * REST controller for executing user code against problem test cases,
 * streaming progress via Server-Sent Events, and tracking submission history.
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final CodeExecutionService executionService;
    private final SubmissionRepository submissionRepository;

    public SubmissionController(CodeExecutionService executionService, SubmissionRepository submissionRepository) {
        this.executionService = executionService;
        this.submissionRepository = submissionRepository;
    }

    /**
     * Executes code submission synchronously.
     */
    @PostMapping("/execute")
    public ResponseEntity<ExecutionResult> executeCode(@RequestBody ExecutionRequest request) {
        if (request.getProblemId() == null || request.getProblemId().isBlank()) {
            return ResponseEntity.badRequest().body(ExecutionResult.compilationError("Problem ID must be provided."));
        }
        ExecutionResult result = executionService.execute(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Executes code submission with live real-time Server-Sent Events (SSE) streaming.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCodeExecution(@RequestBody ExecutionRequest request) {
        SseEmitter emitter = new SseEmitter(45000L); // 45-second timeout
        executionService.executeStreaming(request, emitter);
        return emitter;
    }

    /**
     * Returns past submissions for a problem.
     */
    @GetMapping("/history")
    public ResponseEntity<List<SubmissionRecord>> getSubmissionHistory(@RequestParam(required = false) String problemId) {
        List<SubmissionRecord> history = submissionRepository.findByProblemId(problemId);
        return ResponseEntity.ok(history);
    }

    /**
     * Returns runtime distribution data for LeetCode-style percentile charts.
     */
    @GetMapping("/distribution")
    public ResponseEntity<Map<String, Object>> getRuntimeDistribution(
            @RequestParam(required = false) String problemId,
            @RequestParam(defaultValue = "java") String language) {
        Map<String, Object> dist = submissionRepository.getRuntimeDistribution(problemId, language);
        return ResponseEntity.ok(dist);
    }
}
