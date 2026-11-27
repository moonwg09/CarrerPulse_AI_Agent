package com.careerpulse.job.service;

import com.careerpulse.job.dto.JobCollectionRequest;
import com.careerpulse.user.repository.UserJobRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobCollectionSchedulerTest {

    @Test
    void collectsDistinctInterestJobsNationwide() throws InterruptedException {
        UserJobRepository userJobRepository = mock(UserJobRepository.class);
        JobCollectionService collectionService = mock(JobCollectionService.class);
        JobCollectionScheduler.Sleeper sleeper = mock(
                JobCollectionScheduler.Sleeper.class
        );
        when(userJobRepository.findDistinctJobNames())
                .thenReturn(List.of("백엔드 개발자", "프론트엔드 개발자"));

        new JobCollectionScheduler(
                userJobRepository,
                collectionService,
                1,
                sleeper
        ).collectNationwideJobs();

        ArgumentCaptor<JobCollectionRequest> requestCaptor =
                ArgumentCaptor.forClass(JobCollectionRequest.class);
        verify(collectionService, times(2))
                .collectAndStore(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(JobCollectionRequest::jobKeyword)
                .containsExactly("백엔드 개발자", "프론트엔드 개발자");
        assertThat(requestCaptor.getAllValues())
                .extracting(JobCollectionRequest::limit)
                .containsOnly(10);
        verify(sleeper, never()).sleep(anyLong());
    }

    @Test
    void retriesFailedCollectionUpToThreeAttempts() throws InterruptedException {
        UserJobRepository userJobRepository = mock(UserJobRepository.class);
        JobCollectionService collectionService = mock(JobCollectionService.class);
        JobCollectionScheduler.Sleeper sleeper = mock(
                JobCollectionScheduler.Sleeper.class
        );
        when(userJobRepository.findDistinctJobNames())
                .thenReturn(List.of("백엔드 개발자"));
        when(collectionService.collectAndStore(any(JobCollectionRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY))
                .thenReturn(null);

        new JobCollectionScheduler(
                userJobRepository,
                collectionService,
                100,
                sleeper
        ).collectNationwideJobs();

        verify(collectionService, times(3))
                .collectAndStore(any(JobCollectionRequest.class));
        verify(sleeper, times(2)).sleep(100);
    }

    @Test
    void skipsCollectionWhenNoInterestJobIsRegistered() throws InterruptedException {
        UserJobRepository userJobRepository = mock(UserJobRepository.class);
        JobCollectionService collectionService = mock(JobCollectionService.class);
        JobCollectionScheduler.Sleeper sleeper = mock(
                JobCollectionScheduler.Sleeper.class
        );
        when(userJobRepository.findDistinctJobNames()).thenReturn(List.of());

        new JobCollectionScheduler(
                userJobRepository,
                collectionService,
                1,
                sleeper
        ).collectNationwideJobs();

        verify(collectionService, never())
                .collectAndStore(any(JobCollectionRequest.class));
        verify(sleeper, never()).sleep(anyLong());
    }
}
