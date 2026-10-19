package com.careerpulse.learning.controller;

import com.careerpulse.learning.dto.LearningJobResponse;
import com.careerpulse.learning.service.LearningService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// 반환한 공고 목록을 JSON 응답으로 전달한다.
@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {

    private final LearningService learningService;

    // Spring이 LearningService 객체를 주입한다.
    public LearningController(
            LearningService learningService
    ) {
        this.learningService = learningService;
    }

    // GET /api/v1/learning/jobs?jobCategory=BACKEND
    @GetMapping("/jobs")
    public List<LearningJobResponse> getAvailableJobs(
            Authentication authentication,
            @RequestParam("jobCategory") String jobCategory
    ) {
        // 로그인하지 않은 요청은 거절한다.
        // 익명 인증 객체도 있을 수 있으므로 함께 확인한다.
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        // 빈 직무 분류는 잘못된 요청으로 처리한다.
        if (jobCategory.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "조회할 직무 분류가 필요합니다."
            );
        }

        // Service가 조회한 목록을 반환한다.
        return learningService.getAvailableJobs(jobCategory);
    }
    // 로그인 사용자의 관심 직무를 기준으로 공고를 조회한다.
// GET /api/v1/learning/jobs/me
    @GetMapping("/jobs/me")
    public List<LearningJobResponse> getMyAvailableJobs(
            Authentication authentication
    ) {
        // 로그인하지 않은 요청은 거절한다.
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        // 사용자 이메일은 요청 파라미터가 아니라 로그인 정보에서 가져온다.
        String email = authentication.getName();

        return learningService.getMyAvailableJobs(email);
    }
}