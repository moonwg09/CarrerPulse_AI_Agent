package com.careerpulse.learning.service;

import com.careerpulse.learning.dto.LearningJobResponse;
import com.careerpulse.learning.repository.LearningAnalysisRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 저장된 요구조건과 최신 사용자 비교 결과를 Python 학습 분석 계약으로 변환한다. */
@Service
public class LearningContextService {
    private final LearningAnalysisRepository repository;
    private final ObjectMapper mapper;

    public LearningContextService(LearningAnalysisRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Context build(long userId, List<LearningJobResponse> selectedJobs) {
        List<Long> jobIds = selectedJobs.stream().map(LearningJobResponse::jobId).toList();
        var requirements = repository.findRequirements(jobIds);
        var assessments = repository.findLatestAssessments(userId, jobIds);

        Map<Long, List<LearningAnalysisRepository.RequirementRow>> byJob = new LinkedHashMap<>();
        requirements.forEach(row -> byJob.computeIfAbsent(row.jobId(), ignored -> new ArrayList<>()).add(row));

        ArrayNode jobs = mapper.createArrayNode();
        int skipped = 0;
        for (LearningJobResponse job : selectedJobs) {
            ArrayNode items = mapper.createArrayNode();
            Map<String, ObjectNode> unique = new LinkedHashMap<>();
            for (var row : byJob.getOrDefault(job.jobId(), List.of())) {
                for (LearningItem item : learningItems(row.evaluationRule())) {
                    String key = item.type() + "|" + item.name().toLowerCase(Locale.ROOT);
                    ObjectNode value = mapper.createObjectNode();
                    value.put("name", item.name());
                    value.put("type", item.type());
                    value.put("category", row.requirementType());
                    ObjectNode existing = unique.get(key);
                    if (existing == null || "REQUIRED".equals(row.requirementType())) {
                        unique.put(key, value);
                    }
                }
            }
            unique.values().forEach(items::add);
            LocalDate postedDate = job.postedDate() != null
                    ? job.postedDate()
                    : job.collectedAt() == null ? null : job.collectedAt().toLocalDate();
            if (items.isEmpty() || postedDate == null) {
                skipped++;
                continue;
            }
            ObjectNode node = mapper.createObjectNode();
            node.put("jobId", job.jobId().toString());
            node.put("jobCategory", "SELECTED");
            node.put("postedDate", postedDate.toString());
            if (job.collectedAt() != null) node.put("collectedAt", job.collectedAt().toString());
            if (job.deadline() != null) node.put("deadline", job.deadline().toString());
            node.set("requirements", items);
            jobs.add(node);
        }

        Map<String, AssessmentAccumulator> accumulated = new LinkedHashMap<>();
        for (var row : assessments) {
            for (LearningItem item : learningItems(row.evaluationRule())) {
                String key = item.type() + "|" + item.name().toLowerCase(Locale.ROOT);
                accumulated.computeIfAbsent(
                        key,
                        ignored -> new AssessmentAccumulator(item.name(), item.type())
                ).add(status(row), row.reason());
            }
        }

        ArrayNode assessmentArray = mapper.createArrayNode();
        accumulated.values().forEach(value -> assessmentArray.add(value.toJson(mapper)));
        Set<Long> comparedJobIds = assessments.stream()
                .map(LearningAnalysisRepository.AssessmentRow::jobId)
                .collect(Collectors.toSet());
        List<Long> needsAnalysisJobIds = selectedJobs.stream()
                .map(LearningJobResponse::jobId)
                .filter(jobId -> !comparedJobIds.contains(jobId)
                        || !byJob.containsKey(jobId)
                        || byJob.get(jobId).stream().noneMatch(
                                row -> row.evaluationRule() != null && !row.evaluationRule().isBlank()
                        ))
                .distinct()
                .toList();
        return new Context(jobs, assessmentArray, skipped, needsAnalysisJobIds);
    }

    private List<LearningItem> learningItems(String rawRule) {
        if (rawRule == null || rawRule.isBlank()) return List.of();
        try {
            JsonNode rule = mapper.readTree(rawRule);
            List<LearningItem> values = new ArrayList<>();
            add(values, rule.path("skills"), "SKILL");
            add(values, rule.path("certificates"), "CERTIFICATE");
            String experience = rule.path("experience").asText("").trim();
            if (!experience.isEmpty()) values.add(new LearningItem(experience, "EXPERIENCE"));
            return values;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void add(List<LearningItem> target, JsonNode values, String type) {
        if (!values.isArray()) return;
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        values.forEach(value -> {
            String name = value.asText("").trim();
            if (!name.isEmpty()) unique.add(name);
        });
        unique.forEach(name -> target.add(new LearningItem(name, type)));
    }

    private String status(LearningAnalysisRepository.AssessmentRow row) {
        return switch (row.judgment()) {
            case "EXPERIENCED", "MET" -> "EXPERIENCED";
            case "PARTIAL" -> "PARTIAL";
            case "NO_EXPERIENCE" -> row.userConfirmed() ? "NO_EXPERIENCE" : "UNKNOWN";
            case "UNMET" -> "NO_EXPERIENCE";
            default -> "UNKNOWN";
        };
    }

    public record Context(
            ArrayNode jobs,
            ArrayNode assessments,
            int skippedJobCount,
            List<Long> needsAnalysisJobIds
    ) {}
    private record LearningItem(String name, String type) {}

    private static class AssessmentAccumulator {
        private final String name;
        private final String type;
        private final LinkedHashSet<String> statuses = new LinkedHashSet<>();
        private final LinkedHashSet<String> reasons = new LinkedHashSet<>();

        AssessmentAccumulator(String name, String type) {
            this.name = name;
            this.type = type;
        }

        void add(String status, String reason) {
            statuses.add(status);
            if (reason != null && !reason.isBlank()) reasons.add(reason.trim());
        }

        ObjectNode toJson(ObjectMapper mapper) {
            String status;
            if (statuses.contains("UNKNOWN")) status = "UNKNOWN";
            else if (statuses.contains("PARTIAL")
                    || (statuses.contains("EXPERIENCED") && statuses.contains("NO_EXPERIENCE"))) {
                status = "PARTIAL";
            } else if (statuses.contains("NO_EXPERIENCE")) status = "NO_EXPERIENCE";
            else status = "EXPERIENCED";

            ObjectNode node = mapper.createObjectNode();
            node.put("name", name);
            node.put("type", type);
            node.put("status", status);
            node.put("evidence", status.equals("UNKNOWN")
                    ? ""
                    : reasons.isEmpty() ? "저장된 최신 비교 결과" : String.join(" ", reasons));
            ArrayNode details = node.putArray("missingDetails");
            if (status.equals("NO_EXPERIENCE")) details.add(name);
            return node;
        }
    }
}
