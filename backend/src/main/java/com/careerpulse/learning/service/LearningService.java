package com.careerpulse.learning.service;

import com.careerpulse.learning.dto.LearningJobResponse;
import com.careerpulse.learning.repository.LearningJobRepository;
import com.careerpulse.user.dto.UserJobResponse;
import com.careerpulse.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class LearningService {

    private final LearningJobRepository learningJobRepository;
    private final UserService userService;

    // 공고 조회 Repository와 기존 사용자 Service를 주입받는다.
    public LearningService(
            LearningJobRepository learningJobRepository,
            UserService userService
    ) {
        this.learningJobRepository = learningJobRepository;
        this.userService = userService;
    }

    // 기존 기능: 직접 지정한 직무 분류로 공고 조회.
    @Transactional(readOnly = true)
    public List<LearningJobResponse> getAvailableJobs(
            String jobCategory
    ) {
        if (jobCategory == null || jobCategory.isBlank()) {
            throw new IllegalArgumentException(
                    "조회할 직무 분류가 필요합니다."
            );
        }

        LocalDate referenceDate =
                LocalDate.now(ZoneId.of("Asia/Seoul"));

        return learningJobRepository.findLatestOpenJobs(
                jobCategory.trim(),
                referenceDate
        );
    }

    // 새 기능: 로그인 사용자의 관심 직무를 기준으로 공고 조회.
    @Transactional(readOnly = true)
    public List<LearningJobResponse> getMyAvailableJobs(
            String email
    ) {
        // 기존 서비스를 이용해 해당 사용자의 관심 직무만 가져온다.
        List<UserJobResponse> myJobs =
                userService.getMyJobs(email);

        // 개발용 연결 기준:
        // 관심 직무의 jobCode와 공고의 job_category가 같다고 본다.
        // 실제 수집 데이터 연결 시 팀의 직무 매핑 기준에 맞춰야 한다.
        List<String> jobCategories = myJobs.stream()
                .map(UserJobResponse::jobCode)
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        // 관심 직무가 없으면 전체 공고를 대신 조회하지 않는다.
        if (jobCategories.isEmpty()) {
            return List.of();
        }

        LocalDate referenceDate =
                LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 여러 관심 직무의 공고를 합쳐 최신순 최대 10개 조회한다.
        return learningJobRepository.findLatestOpenJobsForCategories(
                jobCategories,
                referenceDate
        );
    }
}

