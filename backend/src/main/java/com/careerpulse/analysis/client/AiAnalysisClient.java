package com.careerpulse.analysis.client;

import com.careerpulse.analysis.dto.AiJobMatchResult;
import com.careerpulse.analysis.dto.AnalysisCompareRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * [fast api 호출]
 *
 * 백엔드에서 AI 서버(FastAPI)를 호출하는 지점을 이 클래스 하나로 모은다.
 * 호출 대상:
 *   GET  /                           - 연결 확인
 *   POST /api/v1/career/analyze-job  - 서류 x 공고 비교 (Career Agent)
 */
@Component
public class AiAnalysisClient {

    private static final String ANALYZE_JOB_PATH =
            "/api/v1/career/analyze-job";

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    public AiAnalysisClient(
            RestClient aiRestClient,
            ObjectMapper objectMapper
    ) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
    }

    /**
     * AI 응답을 원본 JSON과 파싱 결과로 함께 들고 다니기 위한 묶음.
     * rawJson 은 근거 원문을 손실 없이 저장하는 데 쓴다.
     */
    public record AiCallResult(
            String rawJson,
            AiJobMatchResult result
    ) {
    }

    /**
     * AI 서버 연결 확인.
     */
    public String ping() {

        try {
            return aiRestClient.get()
                    .uri("/")
                    .retrieve()
                    .body(String.class);

        } catch (ResourceAccessException e) {
            throw aiUnavailable();
        }
    }

    /**
     * 서류 분석 결과와 공고 본문을 AI 서버로 보내 비교 결과를 받는다.
     */
    public AiCallResult analyzeJob(
            AnalysisCompareRequest request
    ) {

        ObjectNode body = objectMapper.createObjectNode();

        body.put("jobText", request.jobText());

        body.set("userAnalysis", request.userAnalysis());

        // FastAPI 쪽 embeddedChunks 는 필수 필드이므로
        // 값이 없으면 빈 배열을 보낸다.
        if (request.embeddedChunks() != null
                && request.embeddedChunks().isArray()) {

            body.set("embeddedChunks", request.embeddedChunks());
        } else {
            body.set("embeddedChunks", objectMapper.createArrayNode());
        }

        String rawJson;

        try {
            rawJson = aiRestClient.post()
                    .uri(ANALYZE_JOB_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (req, response) -> {
                                throw toResponseStatusException(
                                        response.getStatusCode(),
                                        readBody(response)
                                );
                            }
                    )
                    .body(String.class);

        } catch (ResourceAccessException e) {
            throw aiUnavailable();
        }

        if (rawJson == null || rawJson.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 서버가 빈 응답을 반환했습니다."
            );
        }

        AiJobMatchResult parsed;

        try {
            parsed = objectMapper.readValue(
                    rawJson,
                    AiJobMatchResult.class
            );

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 서버 응답 형식을 해석할 수 없습니다."
            );
        }

        return new AiCallResult(rawJson, parsed);
    }

    private ResponseStatusException aiUnavailable() {

        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI 분석 서버에 연결할 수 없습니다. "
                        + "AI 서버 실행 상태를 확인해 주세요."
        );
    }

    private String readBody(
            org.springframework.http.client.ClientHttpResponse response
    ) {

        try {
            return new String(
                    response.getBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {
            return "";
        }
    }

    /**
     * FastAPI 의 {"detail": "..."} 를 그대로 사용자에게 전달한다.
     */
    private ResponseStatusException toResponseStatusException(
            HttpStatusCode statusCode,
            String responseBody
    ) {

        String detail = extractDetail(responseBody);

        int status = statusCode.value();

        if (status == 400 || status == 429 || status == 503) {

            return new ResponseStatusException(
                    statusCode,
                    detail
            );
        }

        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "AI 분석 중 오류가 발생했습니다: " + detail
        );
    }

    private String extractDetail(String responseBody) {

        if (responseBody == null || responseBody.isBlank()) {
            return "알 수 없는 오류";
        }

        try {
            JsonNode node = objectMapper.readTree(responseBody);

            JsonNode detail = node.get("detail");

            if (detail != null && !detail.isNull()) {
                return detail.isTextual()
                        ? detail.asText()
                        : detail.toString();
            }

        } catch (IOException ignored) {
            // JSON 이 아니면 본문을 그대로 쓴다.
        }

        return responseBody.length() > 300
                ? responseBody.substring(0, 300)
                : responseBody;
    }
}
