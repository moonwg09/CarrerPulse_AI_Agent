package com.careerpulse.job.repository;

import com.careerpulse.job.entity.JobPosting;
import com.careerpulse.job.entity.JobPostingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JobPostingRepositoryTest {

    @Autowired
    private JobPostingRepository repository;

    @Test
    void 공고를_저장하고_출처와_외부ID로_조회한다() {
        String externalId = "TEST-" + UUID.randomUUID();
        JobPosting posting = new JobPosting("WORK24_TEST", externalId);
        posting.update(
                "테스트 회사",
                "백엔드 개발자",
                "백엔드",
                "서울",
                "Java와 Spring Boot 개발",
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                "https://example.com/jobs/" + externalId,
                JobPostingStatus.OPEN
        );

        repository.saveAndFlush(posting);

        JobPosting found = repository
                .findBySourceAndExternalJobId("WORK24_TEST", externalId)
                .orElseThrow();
        assertThat(found.getJobId()).isNotNull();
        assertThat(found.getTitle()).isEqualTo("백엔드 개발자");
        assertThat(found.getStatus()).isEqualTo(JobPostingStatus.OPEN);
    }

    @Test
    void 같은_공고를_갱신하면_기존_ID를_유지한다() {
        String externalId = "TEST-" + UUID.randomUUID();
        JobPosting posting = new JobPosting("WORK24_TEST", externalId);
        posting.update(
                "테스트 회사",
                "개발자",
                "개발",
                "전국",
                "기존 내용",
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                null,
                JobPostingStatus.OPEN
        );
        repository.saveAndFlush(posting);
        Long originalId = posting.getJobId();

        posting.update(
                "테스트 회사",
                "웹 개발자",
                "웹 개발",
                "서울",
                "수정된 내용",
                LocalDate.now(),
                LocalDate.now().plusDays(21),
                null,
                JobPostingStatus.OPEN
        );
        repository.saveAndFlush(posting);

        JobPosting found = repository
                .findBySourceAndExternalJobId("WORK24_TEST", externalId)
                .orElseThrow();
        assertThat(found.getJobId()).isEqualTo(originalId);
        assertThat(found.getTitle()).isEqualTo("웹 개발자");
        assertThat(found.getDescription()).isEqualTo("수정된 내용");
    }
}
