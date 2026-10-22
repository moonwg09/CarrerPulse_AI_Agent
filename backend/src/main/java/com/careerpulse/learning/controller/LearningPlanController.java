package com.careerpulse.learning.controller;

import com.careerpulse.learning.repository.LearningPlanRepository;
import com.careerpulse.user.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** PLN-04 생성/저장과 PLN-05 본인 최신 계획 조회를 제공한다. 기존 로그인/CSRF가 적용된다. */
@RestController
@RequestMapping("/api/v1/learning/plans/me")
public class LearningPlanController {
    private final LearningAnalysisController analysis;
    private final LearningPlanRepository plans;
    private final AuthService auth;
    private final ObjectMapper mapper;
    private final RestClient ai;
    @Autowired
    public LearningPlanController(LearningAnalysisController analysis, LearningPlanRepository plans,
            AuthService auth, ObjectMapper mapper, @Value("${ai.server.url}") String url) {
        this(analysis, plans, auth, mapper, createClient(url));
    }

    LearningPlanController(LearningAnalysisController analysis, LearningPlanRepository plans,
            AuthService auth, ObjectMapper mapper, RestClient ai) {
        this.analysis = analysis;
        this.plans = plans;
        this.auth = auth;
        this.mapper = mapper;
        this.ai = ai;
    }

    private static RestClient createClient(String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(30000);
        return RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public record PlanInput(@NotNull LocalDate startDate, @NotNull LocalDate endDate,
            @NotNull @DecimalMin("0.01") @DecimalMax("168") @Digits(integer=3, fraction=2) BigDecimal weeklyAvailableHours,
            @NotNull @Pattern(regexp="TEST|GEMINI") String mode,
            @NotNull @Pattern(regexp="TEST|DATABASE") String source, @NotNull JsonNode assessments) {}

    private String email(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return authentication.getName();
    }

    @GetMapping("/latest")
    public JsonNode latest(Authentication authentication) {
        return plans.latest(auth.findByEmail(email(authentication)).getUserId());
    }

    @PostMapping
    public JsonNode generate(Authentication authentication, @Valid @RequestBody PlanInput input) {
        String email = email(authentication);
        if (input.endDate().isBefore(input.startDate()) || ChronoUnit.DAYS.between(input.startDate(), input.endDate()) >= 730)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료일은 시작일 이후여야 하며 최대 기간은 730일입니다.");
        // 브라우저가 보낸 우선순위를 신뢰하지 않고 기존 PLN-01~03 진입점에서 본인 분석을 다시 계산한다.
        // 같은 JVM 내부 호출이며 별도 HTTP 요청이나 사용자 ID 파라미터를 만들지 않는다.
        JsonNode analyzed = analysis.analyze(authentication,
                new LearningAnalysisController.AnalyzeInput(input.source(), input.assessments()));
        var priorities = mapper.createArrayNode();
        for (var item : analyzed.path("preparation")) {
            ObjectNode target = mapper.createObjectNode();
            target.put("name", item.path("name").asText());
            target.put("preparationType", item.path("preparationType").asText());
            target.put("rank", item.path("rank").asInt());
            target.set("targets", item.path("targets"));
            priorities.add(target);
        }
        if (priorities.isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "계획을 생성할 준비 대상이 없습니다. 경험과 부족 세부 항목을 먼저 확인하세요.");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("startDate", input.startDate().toString());
        payload.put("endDate", input.endDate().toString());
        payload.put("weeklyAvailableHours", input.weeklyAvailableHours());
        payload.put("mode", input.mode());
        payload.set("priorities", priorities);
        try {
            JsonNode result = ai.post().uri("/api/v1/learning/plans/generate")
                    .contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(JsonNode.class);
            if (result == null || !result.isObject() || !result.path("weeks").isArray())
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "계획 생성 응답이 올바르지 않습니다.");
            ((ObjectNode) result).put("source", input.source());
            ((ObjectNode) result).put("referenceDate", analyzed.path("referenceDate").asText());
            ((ObjectNode) result).put("targetJobCount", analyzed.path("targetJobCount").asInt());
            return plans.save(auth.findByEmail(email).getUserId(), result);
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(ex.getStatusCode().value() == 422 ? HttpStatus.UNPROCESSABLE_ENTITY
                    : HttpStatus.SERVICE_UNAVAILABLE, "계획 생성에 실패했습니다. 입력값 또는 Python 서버의 Gemini 키·모델 설정을 확인하세요.");
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Python 계획 생성 서버에 연결할 수 없습니다.");
        }
    }
}
