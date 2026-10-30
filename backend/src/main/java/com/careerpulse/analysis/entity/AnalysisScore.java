package com.careerpulse.analysis.entity;

import java.math.BigDecimal;

public record AnalysisScore(
        int matchedCount,
        int comparableCount,
        int unknownCount,
        BigDecimal overallMatchRate,
        BigDecimal requiredMatchRate,
        BigDecimal preferredMatchRate,
        BigDecimal objectiveMatchRate,
        BigDecimal evaluationCoverage
) {
}
