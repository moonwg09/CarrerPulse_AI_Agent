package com.careerpulse.job.dto;

public record JobRequirementResponse(
        Long requirementId,
        String requirementType,
        String comparisonKind,
        String name,
        String description,
        String sourceQuote
) {
}
