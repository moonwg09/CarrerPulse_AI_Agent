// 이 Controller가 속한 패키지입니다.
package com.careerpulse.application.controller;

// 면접일 수정 요청의 데이터 형식입니다.
import com.careerpulse.application.dto.InterviewDateRequest;

// 면접 지원 조회 결과의 데이터 형식입니다.
import com.careerpulse.application.dto.UserApplicationResponse;

// 조회·수정 처리를 담당하는 Service입니다.
import com.careerpulse.application.service.UserApplicationService;
import org.springframework.security.core.Authentication;

// 요청 본문의 값을 검증하는 데 사용합니다.
import jakarta.validation.Valid;

// 수정 요청이 성공했을 때 HTTP 응답을 만드는 데 사용합니다.
import org.springframework.http.ResponseEntity;

// API 요청 경로, HTTP 메서드, 경로 변수 등을 처리하는 어노테이션입니다.
import org.springframework.web.bind.annotation.*;

// 조회 결과 여러 건을 담을 목록 타입입니다.
import java.util.List;

/**
 * 사용자 지원 내역 조회와 면접일 수정을 처리하는 Controller입니다.
 *
 * Controller는 HTTP 요청을 받고, 실제 처리는 Service에 맡깁니다.
 */
@RestController
// 이 Controller의 공통 주소입니다.
// 사용자 ID는 URL에서 받지 않고 로그인 정보에서 확인합니다.
// 예: /api/v1/users/me/applications
@RequestMapping("/api/v1/users/me/applications")
public class UserApplicationController {

    // 업무 처리를 맡길 Service 객체입니다.
    private final UserApplicationService service;

    /**
     * 생성자 주입입니다.
     * Spring이 UserApplicationService 객체를 자동으로 전달합니다.
     */
    public UserApplicationController(UserApplicationService service) {
        this.service = service;
    }

    /**
     * 특정 사용자의 지원 내역 목록을 조회합니다.
     *
     * GET /api/v1/users/me/applications
     */
    @GetMapping
    public List<UserApplicationResponse> getApplications(
            // 로그인한 사용자의 인증 정보입니다. getName()은 로그인 이메일입니다.
            Authentication authentication
    ) {
        // 조회 작업을 Service에 맡기고, 결과 목록을 응답으로 돌려줍니다.
        return service.getApplicationsWithJobInfo(authentication.getName());
    }

    /**
     * 특정 지원 내역의 면접일을 수정합니다.
     *
     * PUT /api/v1/users/me/applications/{applicationId}/interview
     */
    @PutMapping("/{applicationId}/interview")
    public ResponseEntity<Void> updateInterviewDate(
            // URL의 {applicationId} 값을 받습니다.
            @PathVariable Long applicationId,

            // 로그인한 사용자의 인증 정보입니다.
            Authentication authentication,

            // JSON 요청 본문을 InterviewDateRequest로 변환하고 검증합니다.
            // interviewDate가 null이면 요청 검증에서 거부됩니다.
            @Valid @RequestBody InterviewDateRequest request
    ) {
        // 사용자 ID, 지원 내역 ID, 새 면접일을 Service에 전달합니다.
        service.updateInterviewDate(
                applicationId,
                authentication.getName(),
                request.interviewDate()
        );

        // 수정이 성공했고 응답 본문은 없다는 HTTP 204 응답입니다.
        return ResponseEntity.noContent().build();
    }
}
