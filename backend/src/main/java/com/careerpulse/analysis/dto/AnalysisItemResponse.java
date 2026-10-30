package com.careerpulse.analysis.dto;

public record AnalysisItemResponse(
        Long itemId,
        Long requirementId,
        String requirement,
        String requirementCategory,
        String comparisonKind,
        String status,
        String statusLabel,
        String reason,
        Long evidenceChunkId,
        boolean userConfirmed
) {
}
