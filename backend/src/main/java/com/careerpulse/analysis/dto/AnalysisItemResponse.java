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
        Long evidenceDocumentId,
        String evidenceDocumentType,
        String evidenceText,
        String evidenceSourceLocation,
        boolean userConfirmed
) {
}
