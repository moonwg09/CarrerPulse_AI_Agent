package com.careerpulse.analysis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 분석 결과 1건의 요약. 목록 조회와 추천조회에서 공통으로 쓴다.
 */
public record AnalysisSummaryResponse(

        Long analysisId,

        String jobKey,

        String jobTitle,

        String companyName,

        LocalDate closeDate,

        String recommendationStatus,

        BigDecimal overallMatchRate,

        BigDecimal requiredMatchRate,

        BigDecimal preferredMatchRate,

        BigDecimal objectiveMatchRate,

        BigDecimal evaluationCoverage,

        int requirementCount,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}
