package com.careerpulse.analysis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnalysisSummaryResponse(
        Long analysisId,
        Long jobId,
        String jobTitle,
        String companyName,
        LocalDate closeDate,
        String analysisStatus,
        String recommendationStatus,
        BigDecimal overallMatchRate,
        BigDecimal requiredMatchRate,
        BigDecimal preferredMatchRate,
        BigDecimal objectiveMatchRate,
        BigDecimal evaluationCoverage,
        int matchedCount,
        int comparableCount,
        int unknownCount,
        int requirementCount,
        LocalDateTime analyzedAt,
        LocalDateTime createdAt
) {
}
