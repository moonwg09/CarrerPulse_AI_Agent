package com.careerpulse.analysis.client;

import com.careerpulse.analysis.dto.AiJobMatchResult;
import com.careerpulse.analysis.dto.JobPostingSnapshot;
import com.careerpulse.analysis.dto.GuidanceResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class AiAnalysisClient {

    public static final String ANALYZE_INDEXED_JOB_PATH =
            "/api/v1/career/analyze-indexed-job";
    public static final String GUIDANCE_PATH = "/api/v1/career/guidance";

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    public AiAnalysisClient(RestClient aiRestClient, ObjectMapper objectMapper) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
    }

    public AiJobMatchResult analyzeJob(
            Long userId,
            JobPostingSnapshot job,
            List<Long> documentIds
    ) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("userId", userId);
        body.put("jobId", job.jobId());
        body.put("jobText", job.toAnalysisText());

        ArrayNode ids = body.putArray("documentIds");
        documentIds.forEach(ids::add);

        try {
            String rawJson = aiRestClient.post()
                    .uri(ANALYZE_INDEXED_JOB_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI 서버가 빈 응답을 반환했습니다."
                );
            }

            return objectMapper.readValue(rawJson, AiJobMatchResult.class);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 분석 서버에 연결할 수 없습니다.",
                    e
            );
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 서버 응답 형식을 해석할 수 없습니다.",
                    e
            );
        }
    }

    public String ping() {
        try {
            return aiRestClient.get().uri("/").retrieve().body(String.class);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 분석 서버에 연결할 수 없습니다.",
                    e
            );
        }
    }

    public GuidanceResponse generateGuidance(
            Long userId,
            List<Long> documentIds,
            String requirement,
            String requirementType,
            String status,
            String missingPart
    ) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("userId", userId);
        body.put("requirement", requirement);
        body.put("requirementType", requirementType);
        body.put("status", status);
        body.put("missingPart", missingPart == null ? "" : missingPart);
        ArrayNode ids = body.putArray("documentIds");
        documentIds.forEach(ids::add);

        try {
            GuidanceResponse response = aiRestClient.post()
                    .uri(GUIDANCE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GuidanceResponse.class);
            if (response == null || response.lines() == null || response.lines().isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI 서버가 검증된 작성 방향을 반환하지 않았습니다."
                );
            }
            return response;
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 분석 서버에 연결할 수 없습니다.",
                    e
            );
        }
    }
}
