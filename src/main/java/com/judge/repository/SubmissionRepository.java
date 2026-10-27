package com.judge.repository;

import com.judge.model.SubmissionRecord;
import com.judge.model.Verdict;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory store for submissions and runtime percentile calculations.
 */
@Repository
public class SubmissionRepository {

    private final List<SubmissionRecord> submissions = new CopyOnWriteArrayList<>();

    public SubmissionRepository() {
        // Pre-populate some historical benchmarks for realistic percentile graphs
        seedBenchmarks();
    }

    private void seedBenchmarks() {
        submissions.add(new SubmissionRecord("two-sum", "java", "// benchmark", Verdict.ACCEPTED, 5, 5, 1));
        submissions.add(new SubmissionRecord("two-sum", "java", "// benchmark", Verdict.ACCEPTED, 5, 5, 2));
        submissions.add(new SubmissionRecord("two-sum", "java", "// benchmark", Verdict.ACCEPTED, 5, 5, 3));
        submissions.add(new SubmissionRecord("two-sum", "java", "// benchmark", Verdict.ACCEPTED, 5, 5, 5));
        submissions.add(new SubmissionRecord("two-sum", "java", "// benchmark", Verdict.ACCEPTED, 5, 5, 8));
        submissions.add(new SubmissionRecord("two-sum", "cpp", "// benchmark", Verdict.ACCEPTED, 5, 5, 1));
        submissions.add(new SubmissionRecord("two-sum", "cpp", "// benchmark", Verdict.ACCEPTED, 5, 5, 2));
    }

    public SubmissionRecord save(SubmissionRecord record) {
        submissions.add(0, record); // newest first
        return record;
    }

    public List<SubmissionRecord> findByProblemId(String problemId) {
        return submissions.stream()
                .filter(s -> problemId == null || problemId.equalsIgnoreCase(s.getProblemId()))
                .collect(Collectors.toList());
    }

    /**
     * Calculates the percentage of submissions beaten by runtimeMs.
     * Higher beats % means faster solution.
     */
    public double calculatePercentile(String problemId, String language, long runtimeMs) {
        List<SubmissionRecord> accepted = submissions.stream()
                .filter(s -> s.getVerdict() == Verdict.ACCEPTED)
                .filter(s -> problemId == null || problemId.equalsIgnoreCase(s.getProblemId()))
                .filter(s -> language == null || language.equalsIgnoreCase(s.getLanguage()))
                .toList();

        if (accepted.isEmpty()) return 100.0;

        long slowerOrEqual = accepted.stream().filter(s -> s.getRuntimeMs() >= runtimeMs).count();
        double percentile = ((double) slowerOrEqual / accepted.size()) * 100.0;
        return Math.min(99.9, Math.max(1.0, Math.round(percentile * 10.0) / 10.0));
    }

    /**
     * Returns distribution buckets for rendering the LeetCode runtime histogram.
     */
    public Map<String, Object> getRuntimeDistribution(String problemId, String language) {
        List<Long> runtimes = submissions.stream()
                .filter(s -> s.getVerdict() == Verdict.ACCEPTED)
                .filter(s -> problemId == null || problemId.equalsIgnoreCase(s.getProblemId()))
                .filter(s -> language == null || language.equalsIgnoreCase(s.getLanguage()))
                .map(SubmissionRecord::getRuntimeMs)
                .sorted()
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("sampleSize", runtimes.size());
        result.put("runtimes", runtimes);
        return result;
    }
}
