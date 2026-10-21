package com.careerpulse.analysis.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * [서류와 공고비교] 요청.
 *
 * userAnalysis / embeddedChunks 는 문서 도메인(FastAPI
 * /api/v1/documents/analyze, RAG embedding)의 결과를 그대로 전달한다.
 * 백엔드는 내용을 해석하지 않고 통과시키므로 JsonNode 로 받는다.
 */
public record AnalysisCompareRequest(

        @NotBlank(message = "공고 식별 키(jobKey)는 필수입니다.")
        @Size(max = 100, message = "jobKey는 100자 이하여야 합니다.")
        String jobKey,

        @NotBlank(message = "공고 본문(jobText)은 필수입니다.")
        String jobText,

        @Size(max = 200)
        String jobTitle,

        @Size(max = 200)
        String companyName,

        @NotNull(message = "공고 마감일(closeDate)은 필수입니다.")
        LocalDate closeDate,

        @NotNull(message = "사용자 문서 분석 결과(userAnalysis)는 필수입니다.")
        JsonNode userAnalysis,

        JsonNode embeddedChunks
) {
}
