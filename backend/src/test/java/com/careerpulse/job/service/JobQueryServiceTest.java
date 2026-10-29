package com.careerpulse.job.service;

import com.careerpulse.job.dto.JobPostingResponse;
import com.careerpulse.job.dto.JobPostingSummaryResponse;
import com.careerpulse.job.entity.JobPosting;
import com.careerpulse.job.entity.JobPostingStatus;
import com.careerpulse.job.repository.JobPostingRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.entity.UserJob;
import com.careerpulse.user.repository.UserJobRepository;
import com.careerpulse.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobQueryServiceTest {

    @Test
    void searchesOpenJobsUsingInterestJobAndOptionalFilters() {
        JobPostingRepository jobRepository = mock(JobPostingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserJobRepository userJobRepository = mock(UserJobRepository.class);
        User user = user();
        ReflectionTestUtils.setField(user, "userId", 7L);
        JobPosting posting = posting();
        ReflectionTestUtils.setField(posting, "jobId", 10L);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(userJobRepository.findAllByUser_UserId(7L))
                .thenReturn(List.of(new UserJob(user, "BACKEND", "백엔드 개발자")));
        when(jobRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(posting)));

        List<JobPostingSummaryResponse> responses = new JobQueryService(
                jobRepository,
                userRepository,
                userJobRepository
        ).search("user@example.com", "서울", "Python");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().jobId()).isEqualTo(10L);
        assertThat(responses.getFirst().originalUrl())
                .isEqualTo("https://www.work24.go.kr/detail/K123");
        verify(jobRepository).findAll(
                any(Specification.class),
                (Pageable) org.mockito.ArgumentMatchers.argThat((Pageable pageable) ->
                        pageable.getPageSize() == 20
                                && pageable.getPageNumber() == 0
                )
        );
    }

    @Test
    void rejectsSearchWhenUserHasNoInterestJob() {
        JobPostingRepository jobRepository = mock(JobPostingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserJobRepository userJobRepository = mock(UserJobRepository.class);
        User user = user();

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(userJobRepository.findAllByUser_UserId(isNull()))
                .thenReturn(List.of());

        JobQueryService service = new JobQueryService(
                jobRepository,
                userRepository,
                userJobRepository
        );

        assertThatThrownBy(() -> service.search(
                "user@example.com",
                null,
                null
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("관심 직무를 먼저 등록해야 합니다.");
    }

    @Test
    void returnsJobPostingDetailWithOriginalUrl() {
        JobPostingRepository jobRepository = mock(JobPostingRepository.class);
        JobPosting posting = posting();
        ReflectionTestUtils.setField(posting, "jobId", 10L);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(posting));

        JobPostingResponse response = new JobQueryService(
                jobRepository,
                mock(UserRepository.class),
                mock(UserJobRepository.class)
        ).get(10L);

        assertThat(response.jobId()).isEqualTo(10L);
        assertThat(response.description()).isEqualTo("Python 백엔드 개발");
        assertThat(response.originalUrl())
                .isEqualTo("https://www.work24.go.kr/detail/K123");
    }

    private static User user() {
        return new User(
                "user@example.com",
                "테스트 사용자",
                "NEW",
                null,
                "DOCUMENT_ANALYSIS_V1"
        );
    }

    private static JobPosting posting() {
        JobPosting posting = new JobPosting("WORK24", "K123");
        posting.update(
                "테스트 회사",
                "Python 백엔드 개발자",
                "소프트웨어",
                "서울특별시 강남구",
                "Python 백엔드 개발",
                LocalDate.of(2026, 10, 28),
                LocalDate.of(2026, 11, 30),
                "https://www.work24.go.kr/detail/K123",
                JobPostingStatus.OPEN
        );
        return posting;
    }
}
