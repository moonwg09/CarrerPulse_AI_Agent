package com.careerpulse.analysis.service;

import com.careerpulse.analysis.dto.AnalysisSummaryResponse;
import com.careerpulse.analysis.dto.RecommendationListResponse;
import com.careerpulse.analysis.entity.AnalysisStatus;
import com.careerpulse.analysis.entity.RecommendationStatus;
import com.careerpulse.analysis.repository.AnalysisContextRepository;
import com.careerpulse.analysis.repository.AnalysisResultRepository;
import com.careerpulse.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class AnalysisRecommendationService {

    private static final int RECOMMENDED_LIMIT = 10;
    private static final int GROWTH_LIMIT = 5;

    private final AnalysisResultRepository resultRepository;
    private final AnalysisContextRepository contextRepository;
    private final AnalysisService analysisService;

    public AnalysisRecommendationService(
            AnalysisResultRepository resultRepository,
            AnalysisContextRepository contextRepository,
            AnalysisService analysisService
    ) {
        this.resultRepository = resultRepository;
        this.contextRepository = contextRepository;
        this.analysisService = analysisService;
    }

    public RecommendationListResponse getRecommendations(
            String email,
            String statusParam,
            Integer limitParam
    ) {
        RecommendationStatus status = parseStatus(statusParam);
        User user = analysisService.findUser(email);
        LocalDate today = LocalDate.now();
        int limit = resolveLimit(status, limitParam);

        List<AnalysisSummaryResponse> items = resultRepository
                .findAllByUser_UserIdAndAnalysisStatus(
                        user.getUserId(),
                        AnalysisStatus.COMPLETED
                )
                .stream()
                .filter(result -> contextRepository.findJob(result.getJobId())
                        .filter(job -> !job.isClosed(today))
                        .isPresent())
                .map(analysisService::toSummary)
                .filter(summary -> status.name().equals(summary.recommendationStatus()))
                .sorted(comparatorFor(status))
                .limit(limit)
                .toList();

        return new RecommendationListResponse(
                status.name(),
                today,
                items.size(),
                items
        );
    }

    private RecommendationStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return RecommendationStatus.RECOMMENDED;
        }
        RecommendationStatus status = RecommendationStatus.from(value);
        if (status == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "status는 RECOMMENDED, GROWTH_CANDIDATE, EXCLUDED 중 하나여야 합니다."
            );
        }
        return status;
    }

    private int resolveLimit(RecommendationStatus status, Integer requested) {
        int maximum = status == RecommendationStatus.GROWTH_CANDIDATE
                ? GROWTH_LIMIT
                : RECOMMENDED_LIMIT;
        if (requested == null) {
            return maximum;
        }
        if (requested < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "limit은 1 이상이어야 합니다."
            );
        }
        return Math.min(requested, maximum);
    }

    private Comparator<AnalysisSummaryResponse> comparatorFor(RecommendationStatus status) {
        Comparator<AnalysisSummaryResponse> overall = Comparator.comparing(
                summary -> zero(summary.overallMatchRate()),
                Comparator.reverseOrder()
        );
        Comparator<AnalysisSummaryResponse> deadline = Comparator.comparing(
                summary -> summary.closeDate() == null ? LocalDate.MAX : summary.closeDate()
        );

        if (status != RecommendationStatus.GROWTH_CANDIDATE) {
            return overall.thenComparing(deadline);
        }

        return overall
                .thenComparing(
                        summary -> zero(summary.requiredMatchRate()),
                        Comparator.reverseOrder()
                )
                .thenComparing(
                        summary -> zero(summary.evaluationCoverage()),
                        Comparator.reverseOrder()
                )
                .thenComparing(deadline);
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
