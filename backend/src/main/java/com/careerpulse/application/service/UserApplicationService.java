// 이 클래스가 속한 패키지입니다.
package com.careerpulse.application.service;

// 조회 결과를 Controller에 전달할 DTO입니다.
import com.careerpulse.application.dto.UserApplicationResponse;

// 지원 내역을 DB에서 조회하는 Repository입니다.
import com.careerpulse.application.repository.UserApplicationRepository;

// 지원 내역 Entity입니다.
import com.careerpulse.application.entity.UserApplication;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Service 역할을 표시해 Spring이 관리하게 합니다.
import org.springframework.stereotype.Service;

// DB 작업의 트랜잭션을 관리합니다.
import org.springframework.transaction.annotation.Transactional;

// 면접일 입력·수정에 사용합니다.
import java.time.LocalDate;

// 조회 결과 목록에 사용합니다.
import java.util.List;

/**
 * 면접 지원 내역과 면접일 관련 처리를 담당합니다.
 */
@Service
@Transactional(readOnly = true)
public class UserApplicationService {

    // Repository에 DB 작업을 요청할 때 사용합니다.
    private final UserApplicationRepository repository;
    private final UserRepository userRepository;

    /**
     * 생성자 주입입니다.
     * Spring이 Repository 객체를 전달합니다.
     */
    public UserApplicationService(
            UserApplicationRepository repository,
            UserRepository userRepository
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    /**
     * 특정 지원 내역의 면접일을 입력하거나 수정합니다.
     */
    @Transactional
    public void updateInterviewDate(
            Long applicationId,
            String email,
            LocalDate interviewDate
    ) {
        // 해당 사용자 소유의 지원 내역을 찾습니다.
        UserApplication application = repository
                .findByApplicationIdAndUser_Email(applicationId, email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "지원 내역을 찾을 수 없습니다."
                        )
                );

        // Entity의 면접일 변경 메서드를 호출합니다.
        application.updateInterviewDate(interviewDate);
    }

    /**
     * 사용자의 지원 내역을 회사명·직무명과 함께 조회합니다.
     */
    public List<UserApplicationResponse> getApplicationsWithJobInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));

        // Repository의 조인 조회 결과를 응답 DTO 목록으로 바꿉니다.
        return repository.findViewsByUserId(user.getUserId())
                .stream()
                .map(view -> new UserApplicationResponse(
                        view.getApplicationId(),
                        view.getJobId(),
                        view.getCompanyName(),
                        view.getTitle(),
                        view.getApplicationStatus(),
                        view.getInterviewDate()
                ))
                .toList();
    }
}
