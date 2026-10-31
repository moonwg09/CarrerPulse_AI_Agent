package com.careerpulse.analysis.dto;

public record AiExperienceRejudgment(
        String status,
        String reason,
        String evidenceId,
        Long documentId
) {
}
