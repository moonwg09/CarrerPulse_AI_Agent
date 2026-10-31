package com.careerpulse.application.repository;

import com.careerpulse.application.entity.ApplicationStatus;
import com.careerpulse.application.entity.UserApplication;
import com.careerpulse.job.entity.JobPosting;
import com.careerpulse.job.repository.JobPostingRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserApplicationRepositoryTest {
    @Autowired UserApplicationRepository applicationRepository;
    @Autowired UserRepository userRepository;
    @Autowired JobPostingRepository jobRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void 지원상태를_저장하고_사용자별로_조회한다() {
        User user = userRepository.save(new User(
                "application-" + UUID.randomUUID() + "@example.com",
                "지원 테스트",
                "NEW",
                null,
                "APPLICATION_TEST_V1"
        ));
        String externalId = "APP-TEST-" + UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO job_postings (
                    source, external_job_id, company_name, title,
                    job_category, region, description, deadline, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "TEST", externalId, "테스트 회사", "백엔드 개발자",
                "백엔드", "서울", "테스트 공고", LocalDate.now().plusDays(7), "OPEN"
        );
        JobPosting job = jobRepository.findBySourceAndExternalJobId("TEST", externalId).orElseThrow();

        UserApplication application = new UserApplication(user, job);
        application.update(ApplicationStatus.INTERVIEW, LocalDate.now().plusDays(3));
        applicationRepository.saveAndFlush(application);

        var found = applicationRepository.findAllByUser_UserIdOrderByUpdatedAtDesc(user.getUserId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(found.get(0).getInterviewDate()).isEqualTo(LocalDate.now().plusDays(3));
    }
}
