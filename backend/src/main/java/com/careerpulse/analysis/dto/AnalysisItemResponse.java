package com.careerpulse.analysis.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 요구사항 1건의 비교 결과.
 *
 * matchedSkills / missingSkills / userEvidence / jobEvidence 는
 * 저장해 둔 JSON 을 그대로 돌려준다(근거 원문 보존).
 */
public record AnalysisItemResponse(

        Long itemId,

        int itemOrder,

        String requirement,

        String requirementCategory,

        String matchStatus,

        String matchStatusLabel,

        String reason,

        JsonNode matchedSkills,

        JsonNode missingSkills,

        JsonNode userEvidence,

        JsonNode jobEvidence
) {
}
