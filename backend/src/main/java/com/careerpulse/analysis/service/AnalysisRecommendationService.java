package com.careerpulse.analysis.service;

import com.careerpulse.analysis.dto.AnalysisSummaryResponse;
import com.careerpulse.analysis.dto.RecommendationListResponse;
import com.careerpulse.analysis.entity.AnalysisStatus;
import com.careerpulse.analysis.entity.RecommendationStatus;
import com.careerpulse.analysis.repository.AnalysisContextRepository;
import com.careerpulse.analysis.repository.AnalysisResultRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AnalysisRecommendationService {

    private static final int RECOMMENDED_LIMIT = 10;
    private static final int GROWTH_LIMIT = 5;

    private final AnalysisResultRepository resultRepository;
    private final AnalysisContextRepository contextRepository;
    private final AnalysisService analysisService;
    private final UserJobRepository userJobRepository;

    public AnalysisRecommendationService(
            AnalysisResultRepository resultRepository,
            AnalysisContextRepository contextRepository,
            AnalysisService analysisService,
            UserJobRepository userJobRepository
    ) {
        this.resultRepository = resultRepository;
        this.contextRepository = contextRepository;
        this.analysisService = analysisService;
        this.userJobRepository = userJobRepository;
    }

    public RecommendationListResponse getRecommendations(
            String email,
            String statusParam,
            Integer limitParam,
            String region
    ) {
        RecommendationStatus status = parseStatus(statusParam);
        User user = analysisService.findUser(email);
        String normalizedRegion = requireRegion(region);
        List<String> interestKeywords = userJobRepository
                .findAllByUser_UserId(user.getUserId())
                .stream()
                .map(userJob -> normalizeInterest(userJob.getJobName()))
                .toList();
        LocalDate today = LocalDate.now();
        int limit = resolveLimit(status, limitParam);

        Set<Long> seenJobIds = new HashSet<>();
        List<AnalysisSummaryResponse> items = resultRepository
                .findAllByUser_UserIdAndAnalysisStatusOrderByAnalyzedAtDesc(
                        user.getUserId(),
                        AnalysisStatus.COMPLETED
                )
                .stream()
                .filter(result -> seenJobIds.add(result.getJobId()))
                .filter(result -> contextRepository.findJob(result.getJobId())
                        .filter(job -> !job.isClosed(today))
                        .filter(job -> matchesRegion(job.region(), normalizedRegion))
                        .filter(job -> matchesInterest(
                                job.title(),
                                job.jobCategory(),
                                job.description(),
                                interestKeywords
                        ))
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

    private String requireRegion(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "추천 공고를 조회할 지역을 선택해야 합니다."
            );
        }
        return value.trim();
    }

    static boolean matchesRegion(String jobRegion, String selectedRegion) {
        return "전국".equals(selectedRegion)
                || (jobRegion != null && jobRegion.contains(selectedRegion));
    }

    static boolean matchesInterest(
            String title,
            String category,
            String description,
            List<String> interestKeywords
    ) {
        String searchable = String.join(" ",
                value(title), value(category), value(description)
        ).toLowerCase(Locale.ROOT);
        return interestKeywords.stream()
                .filter(keyword -> !keyword.isBlank())
                .anyMatch(searchable::contains);
    }

    private static String normalizeInterest(String name) {
        if (name == null) {
            return "";
        }
        return name.trim()
                .replaceFirst("\\s*개발자$", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
