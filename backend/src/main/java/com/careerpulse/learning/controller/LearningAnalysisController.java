package com.careerpulse.learning.controller;

import com.careerpulse.learning.service.LearningContextService;
import com.careerpulse.learning.service.LearningService;
import com.careerpulse.user.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** 로그인 사용자의 관심 직무·지역·최신 비교 결과만 학습 분석에 사용한다. */
@RestController
@RequestMapping("/api/v1/learning")
public class LearningAnalysisController {
    private final LearningService learning;
    private final LearningContextService contexts;
    private final AuthService users;
    private final ObjectMapper mapper;
    private final RestClient ai;

    @Autowired
    public LearningAnalysisController(
            LearningService learning,
            LearningContextService contexts,
            AuthService users,
            ObjectMapper mapper,
            @Value("${ai.server.url}") String aiUrl
    ) {
        this(learning, contexts, users, mapper, createClient(aiUrl));
    }

    LearningAnalysisController(
            LearningService learning,
            LearningContextService contexts,
            AuthService users,
            ObjectMapper mapper,
            RestClient ai
    ) {
        this.learning = learning;
        this.contexts = contexts;
        this.users = users;
        this.mapper = mapper;
        this.ai = ai;
    }

    private static RestClient createClient(String aiUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(30000);
        return RestClient.builder().baseUrl(aiUrl).requestFactory(factory).build();
    }

    public record AnalyzeInput(@NotBlank String region) {}

    @PostMapping("/analysis/me")
    public JsonNode analyze(Authentication authentication, @Valid @RequestBody AnalyzeInput input) {
        String email = email(authentication);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        var selectedJobs = learning.getMyAvailableJobs(email, input.region());
        long userId = users.findByEmail(email).getUserId();
        var context = contexts.build(userId, selectedJobs);
        if (!selectedJobs.isEmpty() && context.jobs().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "선택된 공고의 요구사항 분석이 아직 완료되지 않았습니다. 공고 분석을 먼저 실행해 주세요."
            );
        }

        ObjectNode payload = mapper.createObjectNode();
        payload.put("referenceDate", today.toString());
        payload.set("interests", mapper.valueToTree(List.of("SELECTED")));
        payload.set("jobs", context.jobs());
        payload.set("assessments", context.assessments());
        try {
            JsonNode response = ai.post().uri("/api/v1/learning/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "분석 서버 응답이 올바르지 않습니다.");
            }
            ObjectNode result = (ObjectNode) response;
            result.put("source", "DATABASE");
            result.put("region", input.region().trim());
            result.put("skippedJobCount", context.skippedJobCount());
            return result;
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    ex.getStatusCode().value() == 422
                            ? HttpStatus.UNPROCESSABLE_ENTITY
                            : HttpStatus.BAD_GATEWAY,
                    ex.getStatusCode().value() == 422
                            ? "저장된 공고 요구사항과 비교 결과를 확인하세요."
                            : "학습 분석 서버 처리에 실패했습니다."
            );
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Python 학습 분석 서버 연결을 확인하세요."
            );
        }
    }

    private String email(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return authentication.getName();
    }
}
