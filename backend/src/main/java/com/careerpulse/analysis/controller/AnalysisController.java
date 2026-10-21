package com.careerpulse.analysis.controller;

import com.careerpulse.analysis.dto.AnalysisCompareRequest;
import com.careerpulse.analysis.dto.AnalysisDetailResponse;
import com.careerpulse.analysis.dto.AnalysisSummaryResponse;
import com.careerpulse.analysis.dto.RecommendationListResponse;
import com.careerpulse.analysis.service.AnalysisRecommendationService;
import com.careerpulse.analysis.service.AnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * analysis 기능 API.
 *
 *  POST /api/v1/analysis/compare              [서류와 공고비교] + [fast api 호출] + [결과저장]
 *  GET  /api/v1/analysis                      저장된 분석 결과 목록
 *  GET  /api/v1/analysis/{id}                 분석 결과 상세 (요구사항별 판정 + 근거)
 *  GET  /api/v1/analysis/{id}/raw             AI 응답 원본 JSON
 *  GET  /api/v1/analysis/recommendations      [추천조회]
 *  GET  /api/v1/analysis/ai-health            AI 서버 연결 확인
 *
 * 모든 엔드포인트는 로그인이 필요하다(SecurityConfig 의 anyRequest().authenticated()).
 * POST 는 CSRF 토큰이 필요하므로 프론트에서 /api/v1/auth/csrf 를 먼저 호출한다.
 */
@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnalysisRecommendationService recommendationService;

    public AnalysisController(
            AnalysisService analysisService,
            AnalysisRecommendationService recommendationService
    ) {
        this.analysisService = analysisService;
        this.recommendationService = recommendationService;
    }

    @PostMapping("/compare")
    public ResponseEntity<AnalysisDetailResponse> compare(
            Authentication authentication,

            @Valid
            @RequestBody
            AnalysisCompareRequest request
    ) {

        String email = authentication.getName();

        AnalysisDetailResponse response =
                analysisService.compare(email, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<AnalysisSummaryResponse>> getMyAnalyses(
            Authentication authentication
    ) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                analysisService.getMyAnalyses(email)
        );
    }

    @GetMapping("/recommendations")
    public ResponseEntity<RecommendationListResponse> getRecommendations(
            Authentication authentication,

            @RequestParam(name = "status", required = false)
            String status,

            @RequestParam(name = "limit", required = false)
            Integer limit
    ) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                recommendationService.getRecommendations(
                        email,
                        status,
                        limit
                )
        );
    }

    @GetMapping("/ai-health")
    public ResponseEntity<Map<String, String>> aiHealth() {

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "response", String.valueOf(
                        analysisService.pingAiServer()
                )
        ));
    }

    @GetMapping("/{analysisId}")
    public ResponseEntity<AnalysisDetailResponse> getDetail(
            Authentication authentication,

            @PathVariable Long analysisId
    ) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                analysisService.getDetail(email, analysisId)
        );
    }

    @GetMapping("/{analysisId}/raw")
    public ResponseEntity<JsonNode> getRawResult(
            Authentication authentication,

            @PathVariable Long analysisId
    ) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                analysisService.getRawResult(email, analysisId)
        );
    }
}
