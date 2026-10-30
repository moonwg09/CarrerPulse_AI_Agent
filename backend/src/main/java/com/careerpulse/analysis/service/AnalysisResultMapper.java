package com.careerpulse.analysis.service;

import com.careerpulse.analysis.dto.AiJobMatchResult;
import com.careerpulse.analysis.entity.*;
import com.careerpulse.analysis.repository.AnalysisContextRepository;
import com.careerpulse.analysis.repository.JobRequirementRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class AnalysisResultMapper {

    private final JobRequirementRepository requirementRepository;
    private final AnalysisContextRepository contextRepository;
    private final AnalysisScoreCalculator scoreCalculator;
    private final ObjectMapper objectMapper;

    private static final Pattern CERTIFICATE_SEPARATOR = Pattern.compile(
            "\\s*(?:또는|OR|or|/|,|·)\\s*"
    );

    public AnalysisResultMapper(
            JobRequirementRepository requirementRepository,
            AnalysisContextRepository contextRepository,
            AnalysisScoreCalculator scoreCalculator,
            ObjectMapper objectMapper
    ) {
        this.requirementRepository = requirementRepository;
        this.contextRepository = contextRepository;
        this.scoreCalculator = scoreCalculator;
        this.objectMapper = objectMapper;
    }

    public MappedAnalysis map(Long userId, Long jobId, AiJobMatchResult result) {
        List<AnalysisItem> items = new ArrayList<>();

        for (AiJobMatchResult.RequirementMatch match
                : deduplicateExperience(result.requirementMatches())) {
            RequirementCategory category = RequirementCategory.from(match.requirementCategory());
            JobRequirement requirement = requirement(
                    jobId,
                    ComparisonKind.EXPERIENCE,
                    category,
                    match.requirement(),
                    firstJobEvidenceText(match.jobEvidence()),
                    experienceEvaluationRule(match)
            );

            EvidenceReference evidence = evidence(userId, match.userEvidence());
            Judgment judgment = experienceJudgment(match.status(), evidence.valid());
            String reason = reason(match.reason(), match.status(), evidence.valid());

            items.add(new AnalysisItem(
                    requirement,
                    evidence.chunkId(),
                    judgment,
                    reason,
                    false
            ));
        }

        for (AiJobMatchResult.ObjectiveConditionMatch match
                : deduplicateObjective(result.objectiveConditionMatches())) {
            RequirementCategory category = RequirementCategory.from(match.requirementCategory());
            JobRequirement requirement = requirement(
                    jobId,
                    ComparisonKind.OBJECTIVE,
                    category,
                    match.requirement(),
                    match.requirement(),
                    objectiveEvaluationRule(match)
            );

            EvidenceReference evidence = evidence(userId, match.userEvidence());
            Judgment judgment = objectiveJudgment(match.status(), evidence.valid());

            items.add(new AnalysisItem(
                    requirement,
                    evidence.chunkId(),
                    judgment,
                    evidence.valid()
                            ? match.reason()
                            : append(match.reason(), "검증 가능한 사용자 근거가 없습니다."),
                    false
            ));
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("AI 응답에 비교할 요구조건이 없습니다.");
        }

        return new MappedAnalysis(items, scoreCalculator.calculate(items));
    }

    private JobRequirement requirement(
            Long jobId,
            ComparisonKind kind,
            RequirementCategory category,
            String text,
            String sourceQuote,
            String evaluationRule
    ) {
        String normalized = text == null || text.isBlank() ? "미확인 요구조건" : text.trim();
        String key = requirementKey(kind, normalized);
        JobRequirement requirement = requirementRepository
                .findByJobIdAndRequirementKey(jobId, key)
                .orElseGet(() -> new JobRequirement(
                        jobId,
                        key,
                        category,
                        kind,
                        truncate(normalized, 255),
                        normalized,
                        sourceQuote,
                        evaluationRule
                ));
        requirement.apply(
                category,
                kind,
                truncate(normalized, 255),
                normalized,
                sourceQuote,
                evaluationRule
        );
        return requirementRepository.save(requirement);
    }

    private String experienceEvaluationRule(AiJobMatchResult.RequirementMatch match) {
        ObjectNode rule = objectMapper.createObjectNode();
        rule.put("version", 1);
        ArrayNode skills = rule.putArray("skills");
        var uniqueSkills = new LinkedHashMap<String, String>();
        for (String skill : safe(match.matchedSkills())) {
            addNormalized(uniqueSkills, skill);
        }
        for (String skill : safe(match.missingSkills())) {
            addNormalized(uniqueSkills, skill);
        }
        uniqueSkills.values().forEach(skills::add);
        rule.put("experience", match.requirement() == null ? "" : match.requirement().trim());
        return boundedRule(rule);
    }

    private String objectiveEvaluationRule(AiJobMatchResult.ObjectiveConditionMatch match) {
        if (!"CERTIFICATE".equalsIgnoreCase(match.conditionType())) {
            return null;
        }
        ObjectNode rule = objectMapper.createObjectNode();
        rule.put("version", 1);
        ArrayNode certificates = rule.putArray("certificates");
        var unique = new LinkedHashMap<String, String>();
        String requirement = match.requirement() == null ? "" : match.requirement().trim();
        for (String item : CERTIFICATE_SEPARATOR.split(requirement)) {
            String cleaned = item
                    .replaceAll("(?i)\\s*(보유자?|소지자?|우대|필수|자격증|면허)\\s*", " ")
                    .trim();
            addNormalized(unique, cleaned);
        }
        if (unique.isEmpty()) {
            addNormalized(unique, requirement);
        }
        unique.values().forEach(certificates::add);
        return boundedRule(rule);
    }

    private void addNormalized(Map<String, String> target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String cleaned = String.join(" ", value.trim().split("\\s+"));
        target.putIfAbsent(cleaned.toLowerCase(Locale.ROOT), cleaned);
    }

    private String boundedRule(ObjectNode rule) {
        String value = rule.toString();
        if (value.length() > 500) {
            throw new IllegalArgumentException("학습 요구사항 메타데이터가 저장 한도를 초과했습니다.");
        }
        return value;
    }

    private EvidenceReference evidence(Long userId, List<JsonNode> evidences) {
        for (JsonNode node : safe(evidences)) {
            Long chunkId = positiveLong(node, "evidenceChunkId");
            if (chunkId != null && contextRepository.isOwnedCompletedChunk(userId, chunkId)) {
                return new EvidenceReference(true, chunkId);
            }

            Long documentId = positiveLong(node, "documentId");
            if (documentId != null
                    && contextRepository.isOwnedCompletedDocument(userId, documentId)) {
                return new EvidenceReference(true, null);
            }
        }
        return new EvidenceReference(false, null);
    }

    private Judgment experienceJudgment(String status, boolean hasEvidence) {
        if (!hasEvidence) {
            return Judgment.INSUFFICIENT;
        }
        if ("EXPERIENCED".equalsIgnoreCase(status)) {
            return Judgment.EXPERIENCED;
        }
        if ("PARTIAL".equalsIgnoreCase(status)) {
            return Judgment.PARTIAL;
        }
        // 경험 없음은 사용자가 직접 확인하기 전까지 확정하지 않는다.
        return Judgment.INSUFFICIENT;
    }

    private Judgment objectiveJudgment(String status, boolean hasEvidence) {
        if (!hasEvidence) {
            return Judgment.UNKNOWN;
        }
        if ("SATISFIED".equalsIgnoreCase(status)) {
            return Judgment.MET;
        }
        if ("NOT_SATISFIED".equalsIgnoreCase(status)) {
            return Judgment.UNMET;
        }
        return Judgment.UNKNOWN;
    }

    private String reason(String original, String aiStatus, boolean hasEvidence) {
        if (!hasEvidence) {
            return append(original, "검증 가능한 사용자 근거가 없어 기록 부족으로 처리했습니다.");
        }
        if ("NO_EXPERIENCE".equalsIgnoreCase(aiStatus)) {
            return append(original, "사용자 확인 전에는 경험 없음으로 확정하지 않습니다.");
        }
        return original;
    }

    private String firstJobEvidenceText(List<JsonNode> evidences) {
        for (JsonNode node : safe(evidences)) {
            JsonNode text = node.get("text");
            if (text != null && text.isTextual() && !text.asText().isBlank()) {
                return text.asText();
            }
        }
        return null;
    }

    private static Long positiveLong(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToLong() || value.asLong() <= 0) {
            return null;
        }
        return value.asLong();
    }

    private static String requirementKey(ComparisonKind kind, String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    (kind + "|" + text.toLowerCase())
                            .getBytes(StandardCharsets.UTF_8)
            );
            return "REQ-" + HexFormat.of().formatHex(hash, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("요구조건 키를 만들 수 없습니다.", e);
        }
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String append(String first, String second) {
        if (first == null || first.isBlank()) {
            return second;
        }
        return first + " " + second;
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private List<AiJobMatchResult.RequirementMatch> deduplicateExperience(
            List<AiJobMatchResult.RequirementMatch> values
    ) {
        Map<String, AiJobMatchResult.RequirementMatch> unique = new LinkedHashMap<>();
        for (AiJobMatchResult.RequirementMatch value : safe(values)) {
            String key = normalizedKey(value.requirement());
            unique.merge(key, value, (existing, candidate) ->
                    isRequired(candidate.requirementCategory()) ? candidate : existing);
        }
        return List.copyOf(unique.values());
    }

    private List<AiJobMatchResult.ObjectiveConditionMatch> deduplicateObjective(
            List<AiJobMatchResult.ObjectiveConditionMatch> values
    ) {
        Map<String, AiJobMatchResult.ObjectiveConditionMatch> unique = new LinkedHashMap<>();
        for (AiJobMatchResult.ObjectiveConditionMatch value : safe(values)) {
            String key = normalizedKey(value.requirement());
            unique.merge(key, value, (existing, candidate) ->
                    isRequired(candidate.requirementCategory()) ? candidate : existing);
        }
        return List.copyOf(unique.values());
    }

    private static boolean isRequired(String value) {
        return "REQUIRED".equalsIgnoreCase(value);
    }

    private static String normalizedKey(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private record EvidenceReference(boolean valid, Long chunkId) {
    }

    public record MappedAnalysis(List<AnalysisItem> items, AnalysisScore score) {
    }
}
