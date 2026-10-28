package com.careerpulse.job.service;

import com.careerpulse.job.client.AiJobCollectionResponse;
import com.careerpulse.job.client.AiRawJobPosting;
import com.careerpulse.job.dto.JobPostingResponse;
import com.careerpulse.job.entity.JobPosting;
import com.careerpulse.job.entity.JobPostingStatus;
import com.careerpulse.job.repository.JobPostingRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobPostingServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-28T00:00:00Z"),
            ZoneId.of("UTC")
    );

    @Test
    void savesNewJobPostingBySourceAndExternalId() {
        JobPostingRepository repository = mock(JobPostingRepository.class);
        when(repository.findBySourceAndExternalJobId("WORK24", "K123"))
                .thenReturn(Optional.empty());
        when(repository.save(any(JobPosting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobPostingService service = new JobPostingService(repository, CLOCK);
        List<JobPostingResponse> responses = service.upsertAll(collection(job(
                "K123",
                "백엔드 개발자",
                LocalDate.of(2026, 11, 30)
        )));

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().externalJobId()).isEqualTo("K123");
        assertThat(responses.getFirst().jobCategory()).isEqualTo("소프트웨어");
        assertThat(responses.getFirst().status()).isEqualTo(JobPostingStatus.OPEN);
        verify(repository).flush();
    }

    @Test
    void updatesExistingJobWithoutCreatingDuplicate() {
        JobPostingRepository repository = mock(JobPostingRepository.class);
        JobPosting existing = new JobPosting("WORK24", "K123");
        existing.update(
                "기존 회사",
                "기존 제목",
                "소프트웨어",
                "서울",
                "기존 내용",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 20),
                "https://example.com/old",
                JobPostingStatus.OPEN
        );
        when(repository.findBySourceAndExternalJobId("WORK24", "K123"))
                .thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        JobPostingService service = new JobPostingService(repository, CLOCK);
        List<JobPostingResponse> responses = service.upsertAll(collection(job(
                "K123",
                "변경된 백엔드 개발자",
                LocalDate.of(2026, 10, 20)
        )));

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().title()).isEqualTo("변경된 백엔드 개발자");
        assertThat(responses.getFirst().status()).isEqualTo(JobPostingStatus.CLOSED);
        verify(repository).save(existing);
    }

    private static AiJobCollectionResponse collection(AiRawJobPosting job) {
        return new AiJobCollectionResponse(
                "백엔드 개발자",
                "서울",
                1,
                1,
                List.of(job)
        );
    }

    private static AiRawJobPosting job(
            String externalJobId,
            String title,
            LocalDate deadline
    ) {
        return new AiRawJobPosting(
                "work24_" + externalJobId,
                externalJobId,
                title,
                "테스트 회사",
                "서울특별시 강남구",
                LocalDate.of(2026, 10, 28),
                deadline,
                "https://www.work24.go.kr/detail/" + externalJobId,
                "Python 백엔드 개발",
                "WORK24"
        );
    }
}
