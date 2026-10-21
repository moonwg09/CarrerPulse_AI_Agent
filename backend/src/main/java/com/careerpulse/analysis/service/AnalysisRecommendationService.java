package com.careerpulse.analysis.service;

import com.careerpulse.analysis.dto.AnalysisSummaryResponse;
import com.careerpulse.analysis.dto.RecommendationListResponse;
import com.careerpulse.analysis.entity.AnalysisResult;
import com.careerpulse.analysis.entity.RecommendationStatus;
import com.careerpulse.analysis.repository.AnalysisResultRepository;
import com.careerpulse.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * [추천조회]
 *
 * 저장된 비교 결과를 기준으로 추천 공고 / 성장 후보 공고를 조회한다.
 * 제외·정렬 규칙은 ai/services/recommendation_service.py 와 같게 맞췄다.
 *
 *  - 마감일이 지난 공고 제외
 *  - 추천 공고      : 전체 일치율 내림차순 -> 마감일 가까운 순, 최대 10개
 *  - 성장 후보 공고 : 전체 일치율 -> 필수 충족률 -> 평가 가능 비율
 *                     내림차순 -> 마감일 가까운 순, 최대 5개
 *
 * 공고 중복 제거는 (user_id, job_key) 유니크 제약으로 저장 단계에서 이미 끝난다.
 */
@Service
public class AnalysisRecommendationService {

    private static final int RECOMMENDED_DEFAULT_LIMIT = 10;
    private static final int GROWTH_DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 50;

    private final AnalysisResultRepository analysisResultRepository;
    private final AnalysisService analysisService;

    public AnalysisRecommendationService(
            AnalysisResultRepository analysisResultRepository,
            AnalysisService analysisService
    ) {
        this.analysisResultRepository = analysisResultRepository;
        this.analysisService = analysisService;
    }

    @Transactional(readOnly = true)
    public RecommendationListResponse getRecommendations(
            String email,
            String statusParam,
            Integer limitParam
    ) {

        RecommendationStatus status = parseStatus(statusParam);

        User user = analysisService.findUser(email);

        LocalDate today = LocalDate.now();

        List<AnalysisResult> found = analysisResultRepository
                .findAllByUser_UserIdAndRecommendationStatusAndCloseDateGreaterThanEqual(
                        user.getUserId(),
                        status,
                        today
                );

        int limit = resolveLimit(status, limitParam);

        List<AnalysisSummaryResponse> items = found.stream()
                .sorted(comparatorFor(status))
                .limit(limit)
                .map(analysisService::toSummary)
                .toList();

        return new RecommendationListResponse(
                status.name(),
                today,
                items.size(),
                items
        );
    }

    private RecommendationStatus parseStatus(String statusParam) {

        if (statusParam == null || statusParam.isBlank()) {
            return RecommendationStatus.RECOMMENDED;
        }

        RecommendationStatus status =
                RecommendationStatus.from(statusParam);

        if (status == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "status 는 RECOMMENDED, GROWTH_CANDIDATE, "
                            + "EXCLUDED 중 하나여야 합니다."
            );
        }

        return status;
    }

    private int resolveLimit(
            RecommendationStatus status,
            Integer limitParam
    ) {

        if (limitParam == null) {
            return status == RecommendationStatus.GROWTH_CANDIDATE
                    ? GROWTH_DEFAULT_LIMIT
                    : RECOMMENDED_DEFAULT_LIMIT;
        }

        if (limitParam < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "limit 은 1 이상이어야 합니다."
            );
        }

        return Math.min(limitParam, MAX_LIMIT);
    }

    private Comparator<AnalysisResult> comparatorFor(
            RecommendationStatus status
    ) {

        Comparator<AnalysisResult> byOverall =
                Comparator.comparing(
                        AnalysisRecommendationService::overall
                ).reversed();

        if (status != RecommendationStatus.GROWTH_CANDIDATE) {

            return byOverall.thenComparing(
                    AnalysisRecommendationService::closeDate
            );
        }

        return byOverall
                .thenComparing(
                        Comparator.comparing(
                                AnalysisRecommendationService::required
                        ).reversed()
                )
                .thenComparing(
                        Comparator.comparing(
                                AnalysisRecommendationService::coverage
                        ).reversed()
                )
                .thenComparing(
                        AnalysisRecommendationService::closeDate
                );
    }

    private static BigDecimal overall(AnalysisResult result) {
        return orZero(result.getOverallMatchRate());
    }

    private static BigDecimal required(AnalysisResult result) {
        return orZero(result.getRequiredMatchRate());
    }

    private static BigDecimal coverage(AnalysisResult result) {
        return orZero(result.getEvaluationCoverage());
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static LocalDate closeDate(AnalysisResult result) {
        return result.getCloseDate() == null
                ? LocalDate.MAX
                : result.getCloseDate();
    }
}
