package com.careerpulse.analysis.service;

import com.careerpulse.analysis.dto.AiJobMatchResult;
import com.careerpulse.analysis.entity.*;
import com.careerpulse.analysis.repository.AnalysisContextRepository;
import com.careerpulse.analysis.repository.JobRequirementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisMappingTest {

    @Test
    void 전체와_세부_일치율을_비교항목에서_계산한다() {
        AnalysisScoreCalculator calculator = new AnalysisScoreCalculator();
        List<AnalysisItem> items = List.of(
                item(RequirementCategory.REQUIRED, ComparisonKind.EXPERIENCE, Judgment.EXPERIENCED),
                item(RequirementCategory.REQUIRED, ComparisonKind.EXPERIENCE, Judgment.PARTIAL),
                item(RequirementCategory.PREFERRED, ComparisonKind.EXPERIENCE, Judgment.INSUFFICIENT),
                item(RequirementCategory.REQUIRED, ComparisonKind.OBJECTIVE, Judgment.MET)
        );

        AnalysisScore score = calculator.calculate(items);

        assertThat(score.matchedCount()).isEqualTo(2);
        assertThat(score.comparableCount()).isEqualTo(3);
        assertThat(score.unknownCount()).isEqualTo(1);
        assertThat(score.overallMatchRate()).isEqualByComparingTo("66.67");
        assertThat(score.requiredMatchRate()).isEqualByComparingTo("66.67");
        assertThat(score.preferredMatchRate()).isNull();
        assertThat(score.objectiveMatchRate()).isEqualByComparingTo("100.00");
        assertThat(score.evaluationCoverage()).isEqualByComparingTo("75.00");
    }

    @Test
    void 경험없음_AI응답은_사용자확인전_기록부족으로_내린다() {
        AnalysisResultMapper mapper = mapper(true);
        AiJobMatchResult result = result("NO_EXPERIENCE", true);

        AnalysisResultMapper.MappedAnalysis mapped = mapper.map(7L, 3L, result);

        assertThat(mapped.items()).hasSize(1);
        assertThat(mapped.items().get(0).getJudgment()).isEqualTo(Judgment.INSUFFICIENT);
        assertThat(mapped.items().get(0).isUserConfirmed()).isFalse();
        assertThat(mapped.score().overallMatchRate()).isNull();
    }

    @Test
    void 검증가능한_근거가_없으면_경험있음을_기록부족으로_내린다() {
        AnalysisResultMapper mapper = mapper(false);

        assertThat(mapper.map(7L, 3L, result("EXPERIENCED", false))
                .items().get(0).getJudgment())
                .isEqualTo(Judgment.INSUFFICIENT);
    }

    @Test
    void 추천상태는_저장된_전체일치율로_결정한다() {
        assertThat(AnalysisService.recommendationStatus(new BigDecimal("50.00")))
                .isEqualTo("RECOMMENDED");
        assertThat(AnalysisService.recommendationStatus(new BigDecimal("49.99")))
                .isEqualTo("GROWTH_CANDIDATE");
        assertThat(AnalysisService.recommendationStatus(new BigDecimal("29.99")))
                .isEqualTo("EXCLUDED");
        assertThat(AnalysisService.recommendationStatus(null)).isNull();
    }

    @Test
    void 알수없는_필수우대_구분은_저장하지_않는다() {
        assertThatThrownBy(() -> RequirementCategory.from("UNCLEAR"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 같은_요구조건이_필수와_우대에_있으면_필수로_한번만_계산한다() {
        AnalysisResultMapper mapper = mapper(true);
        var evidence = List.<com.fasterxml.jackson.databind.JsonNode>of(
                new ObjectMapper().createObjectNode().put("documentId", 11)
        );
        AiJobMatchResult result = new AiJobMatchResult(
                null,
                null,
                List.of(
                        new AiJobMatchResult.RequirementMatch(
                                "Java 경험", "PREFERRED", "EXPERIENCED", "근거",
                                List.of(), List.of(), List.of(), evidence
                        ),
                        new AiJobMatchResult.RequirementMatch(
                                "Java 경험", "REQUIRED", "EXPERIENCED", "근거",
                                List.of(), List.of(), List.of(), evidence
                        )
                ),
                List.of(),
                null,
                null,
                List.of()
        );

        AnalysisResultMapper.MappedAnalysis mapped = mapper.map(7L, 3L, result);

        assertThat(mapped.items()).hasSize(1);
        assertThat(mapped.items().get(0).getRequirement().getRequirementType())
                .isEqualTo(RequirementCategory.REQUIRED);
        assertThat(mapped.score().matchedCount()).isEqualTo(1);
    }

    private AnalysisResultMapper mapper(boolean ownedDocument) {
        JobRequirementRepository requirementRepository = mock(JobRequirementRepository.class);
        AnalysisContextRepository contextRepository = mock(AnalysisContextRepository.class);
        when(requirementRepository.findByJobIdAndRequirementKey(any(), any()))
                .thenReturn(Optional.empty());
        when(requirementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contextRepository.isOwnedCompletedDocument(7L, 11L)).thenReturn(ownedDocument);
        return new AnalysisResultMapper(
                requirementRepository,
                contextRepository,
                new AnalysisScoreCalculator()
        );
    }

    private AiJobMatchResult result(String status, boolean includeEvidence) {
        List<com.fasterxml.jackson.databind.JsonNode> evidence = includeEvidence
                ? List.of(new ObjectMapper().createObjectNode().put("documentId", 11))
                : List.of();
        return new AiJobMatchResult(
                "백엔드 개발자",
                "A회사",
                List.of(new AiJobMatchResult.RequirementMatch(
                        "Spring 경험",
                        "REQUIRED",
                        status,
                        "판정 사유",
                        List.of("Spring"),
                        List.of(),
                        List.of(),
                        evidence
                )),
                List.of(),
                null,
                "RECOMMENDED",
                List.of()
        );
    }

    private AnalysisItem item(
            RequirementCategory category,
            ComparisonKind kind,
            Judgment judgment
    ) {
        JobRequirement requirement = new JobRequirement(
                1L,
                "REQ-" + category + "-" + kind + "-" + judgment,
                category,
                kind,
                "요구조건",
                "요구조건",
                null
        );
        return new AnalysisItem(requirement, null, judgment, null, false);
    }
}
