package com.careerpulse.job.service;

import com.careerpulse.job.dto.JobCollectionRequest;
import com.careerpulse.user.repository.UserJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
@ConditionalOnProperty(
        name = "job.collection.enabled",
        havingValue = "true"
)
public class JobCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobCollectionScheduler.class);
    private static final int COLLECTION_LIMIT = 10;
    private static final int MAX_ATTEMPTS = 3;
    private static final String NATIONWIDE_REGION = "전국";

    private final UserJobRepository userJobRepository;
    private final JobCollectionService jobCollectionService;
    private final long retryDelayMillis;
    private final Sleeper sleeper;

    public JobCollectionScheduler(
            UserJobRepository userJobRepository,
            JobCollectionService jobCollectionService,
            @Value("${job.collection.retry-delay-ms}") long retryDelayMillis
    ) {
        this(
                userJobRepository,
                jobCollectionService,
                retryDelayMillis,
                Thread::sleep
        );
    }

    JobCollectionScheduler(
            UserJobRepository userJobRepository,
            JobCollectionService jobCollectionService,
            long retryDelayMillis,
            Sleeper sleeper
    ) {
        if (retryDelayMillis <= 0) {
            throw new IllegalArgumentException(
                    "JOB_COLLECTION_RETRY_DELAY_MS는 1 이상이어야 합니다."
            );
        }
        this.userJobRepository = userJobRepository;
        this.jobCollectionService = jobCollectionService;
        this.retryDelayMillis = retryDelayMillis;
        this.sleeper = sleeper;
    }

    @Scheduled(
            cron = "${job.collection.cron}",
            zone = "Asia/Seoul"
    )
    public void collectNationwideJobs() {
        List<String> jobNames = userJobRepository.findDistinctJobNames();
        if (jobNames.isEmpty()) {
            log.info("등록된 관심 직무가 없어 정기 공고 수집을 건너뜁니다.");
            return;
        }

        for (String jobName : jobNames) {
            collectWithRetry(jobName);
        }
    }

    private void collectWithRetry(String jobName) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                jobCollectionService.collectAndStore(
                        new JobCollectionRequest(jobName, NATIONWIDE_REGION, COLLECTION_LIMIT)
                );
                return;
            } catch (ResponseStatusException exception) {
                log.warn(
                        "관심 직무 공고 수집 실패: jobName={}, attempt={}/{}",
                        jobName,
                        attempt,
                        MAX_ATTEMPTS,
                        exception
                );
                if (attempt == MAX_ATTEMPTS || !waitBeforeRetry(jobName)) {
                    return;
                }
            }
        }
    }

    private boolean waitBeforeRetry(String jobName) {
        try {
            sleeper.sleep(retryDelayMillis);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("정기 공고 수집 재시도 대기가 중단되었습니다: jobName={}", jobName);
            return false;
        }
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
