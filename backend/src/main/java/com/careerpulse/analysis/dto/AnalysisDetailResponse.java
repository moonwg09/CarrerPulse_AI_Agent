package com.careerpulse.analysis.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 분석 결과 상세. 요약 + 요구사항별 판정 + 객관 조건 + 경고.
 *
 * AI 응답 원본 전체는 크기가 커질 수 있으므로 여기 담지 않고
 * GET /api/v1/analysis/{analysisId}/raw 로 따로 내려준다.
 */
public record AnalysisDetailResponse(

        AnalysisSummaryResponse summary,

        List<AnalysisItemResponse> items,

        JsonNode objectiveConditionMatches,

        JsonNode warnings,

        AnalysisStatusCount statusCount
) {

    /**
     * 판정 상태별 건수. 화면 상단 요약 배지에 쓴다.
     */
    public record AnalysisStatusCount(

            long experienced,

            long partial,

            long insufficientEvidence,

            long noExperience
    ) {
    }
}
