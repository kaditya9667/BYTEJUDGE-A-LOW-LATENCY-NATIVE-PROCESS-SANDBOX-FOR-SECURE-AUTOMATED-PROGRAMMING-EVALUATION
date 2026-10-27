package com.judge.controller;

import com.judge.model.Problem;
import com.judge.model.TestCase;
import com.judge.repository.ProblemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for retrieving available problems and their public details.
 */
@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemRepository problemRepository;

    public ProblemController(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    /**
     * Lists all problems with metadata for the problem selector.
     */
    @GetMapping
    public List<Map<String, Object>> getProblemList() {
        return problemRepository.findAll().stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "title", p.getTitle(),
                        "difficulty", p.getDifficulty(),
                        "totalTests", p.getTestCases().size()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Returns full problem statement, examples, constraints, starter code,
     * and non-hidden test cases (hidden test cases are omitted for integrity).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Problem> getProblemDetails(@PathVariable String id) {
        return problemRepository.findById(id)
                .map(p -> {
                    // Clone or filter test cases so hidden ones are not leaked in the problem statement
                    List<TestCase> publicTests = p.getTestCases().stream()
                            .filter(tc -> !tc.isHidden())
                            .collect(Collectors.toList());

                    Problem sanitized = new Problem(
                            p.getId(),
                            p.getTitle(),
                            p.getDifficulty(),
                            p.getDescription(),
                            p.getExamples(),
                            p.getConstraints(),
                            p.getStarterCode(),
                            p.getStarterCodeCpp(),
                            publicTests
                    );
                    return ResponseEntity.ok(sanitized);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
