package com.careerpulse.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * FastAPI /api/v1/career/analyze-job 응답(JobMatchResult) 매핑용 DTO.
 *
 * status / requirementCategory / recommendationStatus 는 문자열로 받는다.
 * AI 가 예상 밖의 값을 돌려줘도 역직렬화에서 터지지 않게 하고,
 * 실제 enum 변환은 백엔드 규칙(MatchStatus.from 등)이 담당한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiJobMatchResult(

        String jobTitle,

        String companyName,

        List<RequirementMatch> requirementMatches,

        List<JsonNode> objectiveConditionMatches,

        MatchScore score,

        String recommendationStatus,

        List<String> warnings
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RequirementMatch(

            String requirement,

            String requirementCategory,

            String status,

            String reason,

            List<String> matchedSkills,

            List<String> missingSkills,

            // 근거 원문. 가공하지 않고 저장하기 위해 JsonNode 로 둔다.
            List<JsonNode> jobEvidence,

            List<JsonNode> userEvidence
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MatchScore(

            Double overallMatchRate,

            Double requiredMatchRate,

            Double preferredMatchRate,

            Double objectiveMatchRate,

            Double evaluationCoverage
    ) {
    }
}
