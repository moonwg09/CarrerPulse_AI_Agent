package com.careerpulse.analysis.service;

import com.careerpulse.analysis.client.AiAnalysisClient;
import com.careerpulse.analysis.dto.*;
import com.careerpulse.analysis.entity.*;
import com.careerpulse.analysis.repository.AnalysisContextRepository;
import com.careerpulse.analysis.repository.AnalysisResultRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AnalysisService {

    private final UserRepository userRepository;
    private final AnalysisResultRepository resultRepository;
    private final AnalysisContextRepository contextRepository;
    private final AiAnalysisClient aiAnalysisClient;
    private final AnalysisResultMapper resultMapper;
    private final AnalysisScoreCalculator scoreCalculator;

    public AnalysisService(
            UserRepository userRepository,
            AnalysisResultRepository resultRepository,
            AnalysisContextRepository contextRepository,
            AiAnalysisClient aiAnalysisClient,
            AnalysisResultMapper resultMapper,
            AnalysisScoreCalculator scoreCalculator
    ) {
        this.userRepository = userRepository;
        this.resultRepository = resultRepository;
        this.contextRepository = contextRepository;
        this.aiAnalysisClient = aiAnalysisClient;
        this.resultMapper = resultMapper;
        this.scoreCalculator = scoreCalculator;
    }

    public AnalysisDetailResponse compare(String email, AnalysisCompareRequest request) {
        User user = findUser(email);
        JobPostingSnapshot job = findJob(request.jobId());
        List<Long> documentIds = contextRepository.findCompletedDocumentIds(user.getUserId());

        if (documentIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "분석이 완료된 사용자 문서가 없습니다. 문서를 먼저 등록해 주세요."
            );
        }

        AnalysisResult entity = resultRepository
                .findFirstByUser_UserIdAndJobIdOrderByCreatedAtDesc(
                        user.getUserId(),
                        job.jobId()
                )
                .orElseGet(() -> new AnalysisResult(user, job.jobId()));
        entity.resetPending();
        entity = resultRepository.saveAndFlush(entity);

        try {
            AiJobMatchResult aiResult = aiAnalysisClient.analyzeJob(
                    user.getUserId(),
                    job,
                    documentIds
            );
            AnalysisResultMapper.MappedAnalysis mapped = resultMapper.map(
                    user.getUserId(),
                    job.jobId(),
                    aiResult
            );
            entity.complete(mapped.score(), mapped.items());
            return toDetail(resultRepository.saveAndFlush(entity));
        } catch (RuntimeException e) {
            entity.fail();
            resultRepository.saveAndFlush(entity);
            throw e;
        }
    }

    public List<AnalysisSummaryResponse> getMyAnalyses(String email) {
        User user = findUser(email);
        return resultRepository
                .findAllByUser_UserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public AnalysisDetailResponse getDetail(String email, Long analysisId) {
        return toDetail(findMyResult(email, analysisId));
    }

    public AnalysisDetailResponse confirmNoExperience(
            String email,
            Long analysisId,
            Long itemId
    ) {
        AnalysisResult result = findMyResult(email, analysisId);
        AnalysisItem item = result.getItems().stream()
                .filter(candidate -> candidate.getItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "비교 항목을 찾을 수 없습니다."
                ));

        if (item.getRequirement().getComparisonKind() != ComparisonKind.EXPERIENCE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "경험 요구조건만 경험 없음으로 확인할 수 있습니다."
            );
        }

        item.confirmNoExperience();
        AnalysisScore score = scoreCalculator.calculate(result.getItems());
        result.refreshScore(score);
        return toDetail(resultRepository.saveAndFlush(result));
    }

    public String pingAiServer() {
        return aiAnalysisClient.ping();
    }

    AnalysisResult findMyResult(String email, Long analysisId) {
        User user = findUser(email);
        return resultRepository
                .findByAnalysisIdAndUser_UserId(analysisId, user.getUserId())
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
        JobPostingSnapshot job = findJob(result.getJobId());
        AnalysisScore score = scoreCalculator.calculate(result.getItems());
        BigDecimal overall = result.getAnalysisStatus() == AnalysisStatus.COMPLETED
                ? result.getMatchRate()
                : null;

        return new AnalysisSummaryResponse(
                result.getAnalysisId(),
                result.getJobId(),
                job.title(),
                job.companyName(),
                job.deadline(),
                result.getAnalysisStatus().name(),
                recommendationStatus(overall),
                overall,
                score.requiredMatchRate(),
                score.preferredMatchRate(),
                score.objectiveMatchRate(),
                score.evaluationCoverage(),
                result.getMatchedCount(),
                result.getComparableCount(),
                result.getUnknownCount(),
                result.getItems().size(),
                result.getAnalyzedAt(),
                result.getCreatedAt()
        );
    }

    AnalysisDetailResponse toDetail(AnalysisResult result) {
        List<AnalysisItemResponse> items = result.getItems().stream()
                .map(this::toItem)
                .toList();

        return new AnalysisDetailResponse(
                toSummary(result),
                items,
                new AnalysisDetailResponse.AnalysisStatusCount(
                        count(result, Judgment.EXPERIENCED),
                        count(result, Judgment.PARTIAL),
                        count(result, Judgment.INSUFFICIENT),
                        count(result, Judgment.NO_EXPERIENCE),
                        count(result, Judgment.MET),
                        count(result, Judgment.UNMET),
                        count(result, Judgment.UNKNOWN)
                )
        );
    }

    private AnalysisItemResponse toItem(AnalysisItem item) {
        JobRequirement requirement = item.getRequirement();
        return new AnalysisItemResponse(
                item.getItemId(),
                requirement.getRequirementId(),
                requirement.getRequirementName(),
                requirement.getRequirementType().name(),
                requirement.getComparisonKind().name(),
                item.getJudgment().name(),
                statusLabel(item.getJudgment()),
                item.getAnalysisReason(),
                item.getEvidenceChunkId(),
                item.isUserConfirmed()
        );
    }

    private JobPostingSnapshot findJob(Long jobId) {
        return contextRepository.findJob(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "채용공고를 찾을 수 없습니다."
                ));
    }

    static String recommendationStatus(BigDecimal rate) {
        if (rate == null) {
            return null;
        }
        if (rate.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return RecommendationStatus.RECOMMENDED.name();
        }
        if (rate.compareTo(BigDecimal.valueOf(30)) >= 0) {
            return RecommendationStatus.GROWTH_CANDIDATE.name();
        }
        return RecommendationStatus.EXCLUDED.name();
    }

    static String statusLabel(Judgment judgment) {
        return switch (judgment) {
            case EXPERIENCED -> "경험 있음";
            case PARTIAL -> "일부만 있음";
            case INSUFFICIENT -> "기록 부족";
            case NO_EXPERIENCE -> "경험 없음";
            case MET -> "충족";
            case UNMET -> "미충족";
            case UNKNOWN -> "판단 불가";
        };
    }

    private long count(AnalysisResult result, Judgment judgment) {
        return result.getItems().stream()
                .filter(item -> item.getJudgment() == judgment)
                .count();
    }
}
