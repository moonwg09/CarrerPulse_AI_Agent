package com.careerpulse.learning.controller;

import com.careerpulse.learning.service.LearningService;
import com.careerpulse.user.service.UserService;
import com.careerpulse.user.dto.UserJobResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 로그인한 본인의 관심 직무와 공고를 Python 분석기에 전달한다.
 * 사용자 ID/관심 직무/분석 기준일은 클라이언트가 지정하지 못하게 서버에서 결정한다.
 * 경험 상태는 현재 단계에서는 본인이 확인한 입력이며 DB 문서 분석 결과로 가장하지 않는다.
 * 기존 SecurityConfig의 로그인 및 CSRF 보호를 그대로 적용한다.
 */
@RestController
@RequestMapping("/api/v1/learning")
public class LearningAnalysisController {
    private final LearningService learning;
    private final UserService users;
    private final ObjectMapper mapper;
    private final RestClient ai;

    @Autowired
    public LearningAnalysisController(LearningService learning, UserService users, ObjectMapper mapper,
            @Value("${ai.server.url}") String aiUrl) {
        this(learning, users, mapper, createClient(aiUrl));
    }

    // 테스트에서는 실제 네트워크 대신 Spring의 MockRestServiceServer를 주입한다.
    LearningAnalysisController(LearningService learning, UserService users, ObjectMapper mapper, RestClient ai) {
        this.learning = learning;
        this.users = users;
        this.mapper = mapper;
        this.ai = ai;
    }

    private static RestClient createClient(String aiUrl) {
        // 외부 분석 서버 장애가 전체 웹 요청을 무한히 점유하지 않도록 제한한다.
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(30000);
        return RestClient.builder().baseUrl(aiUrl).requestFactory(factory).build();
    }

    public record AnalyzeInput(String source, JsonNode assessments) {}

    @PostMapping("/analysis/me")
    public JsonNode analyze(Authentication authentication, @RequestBody AnalyzeInput input) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (!"TEST".equals(input.source()) && !"DATABASE".equals(input.source())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "source는 TEST 또는 DATABASE입니다.");
        }
        if (input.assessments() == null || !input.assessments().isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assessments 배열이 필요합니다.");
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        ObjectNode payload = mapper.createObjectNode();
        payload.put("referenceDate", today.toString());
        payload.set("assessments", input.assessments());
        var interests = users.getMyJobs(authentication.getName()).stream()
                .map(UserJobResponse::jobCode).filter(x -> x != null && !x.isBlank())
                .map(String::trim).distinct().toList();
        payload.set("interests", mapper.valueToTree(interests));
        ArrayNode jobs = mapper.createArrayNode();
        try {
            if ("TEST".equals(input.source())) {
                // 명시적으로 선택한 자체 데이터이다. 실제 DB에 삽입하거나 기존 공고를 수정하지 않는다.
                try (var stream = new ClassPathResource("learning/sample-jobs.json").getInputStream()) {
                    jobs = (ArrayNode) mapper.readTree(stream);
                }
                for (JsonNode job : jobs) {
                    int offset = job.path("postedOffsetDays").asInt();
                    ObjectNode node = (ObjectNode) job;
                    node.remove("postedOffsetDays");
                    node.put("postedDate", today.minusDays(offset).toString());
                    // 마감 공고의 상대 날짜도 매 실행 시 적용해 시간이 흘러도 테스트 의도를 유지한다.
                    if (node.has("deadlineOffsetDays")) {
                        node.put("deadline", today.plusDays(node.get("deadlineOffsetDays").asInt()).toString());
                        node.remove("deadlineOffsetDays");
                    }
                }
            } else {
                for (var job : learning.getMyAvailableJobs(authentication.getName())) {
                    // 크롤링/분석 어댑터의 계약: description에 구조화된 requirements를 저장한다.
                    // 일반 본문을 빈 요구 조건으로 간주하면 잘못된 성공이 되므로 명확히 거절한다.
                    JsonNode description;
                    try {
                        description = mapper.readTree(job.description() == null ? "" : job.description());
                    } catch (IOException ex) {
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                                "공고 " + job.jobId() + "의 요구 역량 구조화가 필요합니다. 자체 데이터로 먼저 테스트하세요.");
                    }
                    if (description == null || !description.path("requirements").isArray() || job.postedDate() == null) {
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                                "공고 " + job.jobId() + "의 requirements 배열과 게시일이 필요합니다.");
                    }
                    ObjectNode node = mapper.createObjectNode();
                    node.put("jobId", job.jobId().toString());
                    node.put("jobCategory", job.jobCategory());
                    node.put("postedDate", job.postedDate().toString());
                    if (job.collectedAt() != null) node.put("collectedAt", job.collectedAt().toString());
                    if (job.deadline() != null) node.put("deadline", job.deadline().toString());
                    node.set("requirements", description.get("requirements"));
                    jobs.add(node);
                }
            }
            payload.set("jobs", jobs);
            JsonNode response = ai.post().uri("/api/v1/learning/analyze")
                    .contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(JsonNode.class);
            if (response == null || !response.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "분석 서버 응답이 올바르지 않습니다.");
            }
            ((ObjectNode) response).put("source", input.source());
            return response;
        } catch (RestClientResponseException ex) {
            // 검증 오류와 서버 장애를 구분하되, 내부 서버/응답 전문은 사용자에게 노출하지 않는다.
            throw new ResponseStatusException(ex.getStatusCode().value() == 422
                    ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.BAD_GATEWAY,
                    ex.getStatusCode().value() == 422 ? "경험 상태와 근거, 부족 세부 항목 입력을 확인하세요."
                            : "학습 분석 서버 처리에 실패했습니다.");
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Python 학습 분석 서버 연결을 확인하세요.");
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "자체 테스트 데이터를 읽지 못했습니다.");
        }
    }
}
