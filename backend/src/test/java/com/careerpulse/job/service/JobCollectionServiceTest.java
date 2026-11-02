package com.careerpulse.job.service;

import com.careerpulse.job.client.AiJobCollectionRequest;
import com.careerpulse.job.client.AiJobCollectionResponse;
import com.careerpulse.job.client.AiRawJobPosting;
import com.careerpulse.job.client.JobCollectionClient;
import com.careerpulse.job.dto.JobCollectionRequest;
import com.careerpulse.job.dto.JobCollectionResponse;
import com.careerpulse.job.dto.JobPostingResponse;
import com.careerpulse.job.entity.JobPostingStatus;
import com.careerpulse.job.repository.JobPostingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobCollectionServiceTest {

    @Test
    void collectsFromFastApiAndStoresResults() {
        JobCollectionClient client = mock(JobCollectionClient.class);
        JobPostingService postingService = mock(JobPostingService.class);
        JobPostingRepository postingRepository = mock(JobPostingRepository.class);
        AiRawJobPosting rawJob = rawJob();
        AiJobCollectionResponse aiResponse = new AiJobCollectionResponse(
                "백엔드 개발자",
                "서울",
                1,
                1,
                List.of(rawJob)
        );
        JobPostingResponse savedJob = savedJob();
        when(client.collect(org.mockito.ArgumentMatchers.any()))
                .thenReturn(aiResponse);
        when(postingService.upsertAll(aiResponse)).thenReturn(List.of(savedJob));

        JobCollectionResponse response = new JobCollectionService(
                client,
                postingService,
                postingRepository
        ).collectAndStore(new JobCollectionRequest("백엔드 개발자", "서울", 1));

        ArgumentCaptor<AiJobCollectionRequest> requestCaptor =
                ArgumentCaptor.forClass(AiJobCollectionRequest.class);
        verify(client).collect(requestCaptor.capture());
        assertThat(requestCaptor.getValue().jobKeyword()).isEqualTo("백엔드 개발자");
        assertThat(requestCaptor.getValue().region()).isEqualTo("서울");
        assertThat(requestCaptor.getValue().limit()).isEqualTo(1);
        assertThat(response.collectedCount()).isEqualTo(1);
        assertThat(response.jobs()).containsExactly(savedJob);
    }

    private static AiRawJobPosting rawJob() {
        return new AiRawJobPosting(
                "work24_K123",
                "K123",
                "백엔드 개발자",
                "테스트 회사",
                "서울특별시 강남구",
                LocalDate.of(2026, 10, 28),
                LocalDate.of(2026, 11, 30),
                "https://www.work24.go.kr/detail/K123",
                "Python 백엔드 개발",
                "WORK24"
        );
    }

    private static JobPostingResponse savedJob() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 28, 12, 0);
        return new JobPostingResponse(
                1L,
                "WORK24",
                "K123",
                "테스트 회사",
                "백엔드 개발자",
                "소프트웨어",
                "서울특별시 강남구",
                "Python 백엔드 개발",
                LocalDate.of(2026, 10, 28),
                LocalDate.of(2026, 11, 30),
                "https://www.work24.go.kr/detail/K123",
                JobPostingStatus.OPEN,
                now,
                now,
                List.of()
        );
    }
}
