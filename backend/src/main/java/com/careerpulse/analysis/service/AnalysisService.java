package com.careerpulse.analysis.service;

import com.careerpulse.analysis.client.AiAnalysisClient;
import com.careerpulse.analysis.dto.*;
import com.careerpulse.analysis.entity.*;
import com.careerpulse.analysis.repository.AnalysisResultRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * [서류와 공고비교] + [결과저장]
 *
 * 1. 로그인 사용자 확인
 * 2. AiAnalysisClient 로 FastAPI 호출
 * 3. 받은 결과를 analysis_results / analysis_items 에 저장
 * 4. 저장된 내용을 응답으로 반환
 *
 * 판정 상태(EXPERIENCED / PARTIAL / INSUFFICIENT_EVIDENCE / NO_EXPERIENCE)는
 * AI 문자열을 그대로 믿지 않고 MatchStatus.from 규칙을 통과시켜 정한다.
 */
@Service
public class AnalysisService {

    private static final int REQUIREMENT_MAX_LENGTH = 500;
    private static final int REASON_MAX_LENGTH = 1000;

    private final UserRepository userRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final AiAnalysisClient aiAnalysisClient;
    private final ObjectMapper objectMapper;

    public AnalysisService(
            UserRepository userRepository,
            AnalysisResultRepository analysisResultRepository,
            AiAnalysisClient aiAnalysisClient,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.aiAnalysisClient = aiAnalysisClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 서류와 공고를 비교하고 결과를 저장한다.
     */
    @Transactional
    public AnalysisDetailResponse compare(
            String email,
            AnalysisCompareRequest request
    ) {

        User user = findUser(email);

        // [fast api 호출]
        AiAnalysisClient.AiCallResult called =
                aiAnalysisClient.analyzeJob(request);

        AiJobMatchResult aiResult = called.result();

        // [결과저장] 같은 공고를 다시 비교하면 기존 결과를 갱신한다.
        AnalysisResult entity = analysisResultRepository
                .findByUser_UserIdAndJobKey(
                        user.getUserId(),
                        request.jobKey()
                )
                .orElseGet(() -> new AnalysisResult(
                        user,
                        request.jobKey()
                ));

        entity.apply(
                firstNonBlank(aiResult.jobTitle(), request.jobTitle()),
                firstNonBlank(aiResult.companyName(), request.companyName()),
                request.closeDate(),
                RecommendationStatus.from(aiResult.recommendationStatus()),
                rate(aiResult.score() == null
                        ? null : aiResult.score().overallMatchRate()),
                rate(aiResult.score() == null
                        ? null : aiResult.score().requiredMatchRate()),
                rate(aiResult.score() == null
                        ? null : aiResult.score().preferredMatchRate()),
                rate(aiResult.score() == null
                        ? null : aiResult.score().objectiveMatchRate()),
                rate(aiResult.score() == null
                        ? null : aiResult.score().evaluationCoverage()),
                toJson(aiResult.warnings()),
                called.rawJson()
        );

        List<AiJobMatchResult.RequirementMatch> matches =
                aiResult.requirementMatches() == null
                        ? List.of()
                        : aiResult.requirementMatches();

        int order = 0;

        for (AiJobMatchResult.RequirementMatch match : matches) {

            entity.addItem(new AnalysisItem(
                    order++,
                    truncate(
                            blankToDash(match.requirement()),
                            REQUIREMENT_MAX_LENGTH
                    ),
                    RequirementCategory.from(match.requirementCategory()),
                    MatchStatus.from(match.status()),
                    truncate(match.reason(), REASON_MAX_LENGTH),
                    toJson(match.matchedSkills()),
                    toJson(match.missingSkills()),
                    toJson(match.userEvidence()),
                    toJson(match.jobEvidence())
            ));
        }

        AnalysisResult saved = analysisResultRepository.save(entity);

        return toDetail(saved);
    }

    /**
     * 내 분석 결과 목록 (최근 비교 순).
     */
    @Transactional(readOnly = true)
    public List<AnalysisSummaryResponse> getMyAnalyses(String email) {

        User user = findUser(email);

        return analysisResultRepository
                .findAllByUser_UserIdOrderByUpdatedAtDesc(user.getUserId())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * 분석 결과 상세 (요구사항별 판정 + 근거 원문).
     */
    @Transactional(readOnly = true)
    public AnalysisDetailResponse getDetail(
            String email,
            Long analysisId
    ) {

        return toDetail(findMyResult(email, analysisId));
    }

    /**
     * AI 응답 원본 JSON.
     */
    @Transactional(readOnly = true)
    public JsonNode getRawResult(
            String email,
            Long analysisId
    ) {

        AnalysisResult result = findMyResult(email, analysisId);

        return readJson(result.getRawResult());
    }

    /**
     * AI 서버 연결 확인.
     */
    public String pingAiServer() {
        return aiAnalysisClient.ping();
    }

    AnalysisResult findMyResult(String email, Long analysisId) {

        User user = findUser(email);

        return analysisResultRepository
                .findByAnalysisIdAndUser_UserId(
                        analysisId,
                        user.getUserId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "분석 결과를 찾을 수 없습니다."
                ));
    }

    User findUser(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));
    }

    AnalysisSummaryResponse toSummary(AnalysisResult result) {

        return new AnalysisSummaryResponse(
                result.getAnalysisId(),
                result.getJobKey(),
                result.getJobTitle(),
                result.getCompanyName(),
                result.getCloseDate(),
                result.getRecommendationStatus() == null
                        ? null
                        : result.getRecommendationStatus().name(),
                result.getOverallMatchRate(),
                result.getRequiredMatchRate(),
                result.getPreferredMatchRate(),
                result.getObjectiveMatchRate(),
                result.getEvaluationCoverage(),
                result.getItems().size(),
                result.getCreatedAt(),
                result.getUpdatedAt()
        );
    }

    AnalysisDetailResponse toDetail(AnalysisResult result) {

        List<AnalysisItemResponse> items = new ArrayList<>();

        long experienced = 0;
        long partial = 0;
        long insufficient = 0;
        long noExperience = 0;

        for (AnalysisItem item : result.getItems()) {

            items.add(new AnalysisItemResponse(
                    item.getItemId(),
                    item.getItemOrder(),
                    item.getRequirement(),
                    item.getRequirementCategory().name(),
                    item.getMatchStatus().name(),
                    statusLabel(item.getMatchStatus()),
                    item.getReason(),
                    readJson(item.getMatchedSkills()),
                    readJson(item.getMissingSkills()),
                    readJson(item.getUserEvidence()),
                    readJson(item.getJobEvidence())
            ));

            switch (item.getMatchStatus()) {
                case EXPERIENCED -> experienced++;
                case PARTIAL -> partial++;
                case INSUFFICIENT_EVIDENCE -> insufficient++;
                case NO_EXPERIENCE -> noExperience++;
            }
        }

        JsonNode raw = readJson(result.getRawResult());

        JsonNode objectiveConditions = raw == null
                ? null
                : raw.get("objectiveConditionMatches");

        return new AnalysisDetailResponse(
                toSummary(result),
                items,
                objectiveConditions,
                readJson(result.getWarnings()),
                new AnalysisDetailResponse.AnalysisStatusCount(
                        experienced,
                        partial,
                        insufficient,
                        noExperience
                )
        );
    }

    static String statusLabel(MatchStatus status) {

        return switch (status) {
            case EXPERIENCED -> "경험 있음";
            case PARTIAL -> "일부만 있음";
            case INSUFFICIENT_EVIDENCE -> "기록 부족";
            case NO_EXPERIENCE -> "경험 없음";
        };
    }

    static BigDecimal rate(Double value) {

        if (value == null) {
            return null;
        }

        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    static String firstNonBlank(String first, String second) {

        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return null;
    }

    static String blankToDash(String value) {

        return (value == null || value.isBlank())
                ? "-"
                : value;
    }

    static String truncate(String value, int maxLength) {

        if (value == null) {
            return null;
        }

        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }

    String toJson(Object value) {

        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);

        } catch (JsonProcessingException e) {
            return null;
        }
    }

    JsonNode readJson(String json) {

        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(json);

        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
