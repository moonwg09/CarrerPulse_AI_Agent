package com.careerpulse.application.repository;

import com.careerpulse.application.dto.UserApplicationView;
import com.careerpulse.application.entity.UserApplication;
import com.careerpulse.user.entity.User;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:careerpulse-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserApplicationRepositoryTest {

    @Autowired
    private UserApplicationRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createJobPostingsTableForNativeJoin() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS job_postings (
                    job_id BIGINT PRIMARY KEY,
                    source VARCHAR(30) NOT NULL,
                    external_job_id VARCHAR(100),
                    company_name VARCHAR(255) NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                )
                """);
    }

    @Test
    void findsApplicationOnlyForItsOwner() {
        User user = createUser("owner@example.test");
        UserApplication application = createApplication(user, 1001L, LocalDate.of(2026, 10, 5));

        assertThat(repository.findByApplicationIdAndUser_UserId(
                application.getApplicationId(), user.getUserId()
        )).isPresent();

        assertThat(repository.findByApplicationIdAndUser_UserId(
                application.getApplicationId(), -1L
        )).isEmpty();
    }

    @Test
    void findsDatedApplicationsInDateOrderAndOmitsApplicationsWithoutDates() {
        User user = createUser("dates@example.test");
        createApplication(user, 1002L, LocalDate.of(2026, 10, 6));
        createApplication(user, 1003L, null);
        createApplication(user, 1004L, LocalDate.of(2026, 10, 5));

        List<UserApplication> results = repository
                .findByUser_UserIdAndInterviewDateIsNotNullOrderByInterviewDateAsc(user.getUserId());

        assertThat(results)
                .extracting(UserApplication::getInterviewDate)
                .containsExactly(
                        LocalDate.of(2026, 10, 5),
                        LocalDate.of(2026, 10, 6)
                );
    }

    @Test
    void joinsApplicationWithCompanyAndJobTitle() {
        User user = createUser("join@example.test");
        jdbcTemplate.update(
                "INSERT INTO job_postings (job_id, source, company_name, title) VALUES (?, ?, ?, ?)",
                2001L, "MOCK", "가상기업", "백엔드 개발자"
        );
        createApplication(user, 2001L, LocalDate.of(2026, 10, 7));

        List<UserApplicationView> results = repository.findViewsByUserId(user.getUserId());

        System.out.println("조회된 면접 일정:");
        results.forEach(view -> System.out.printf(
                "지원 ID=%d, 회사=%s, 직무=%s, 상태=%s, 면접일=%s%n",
                view.getApplicationId(), view.getCompanyName(), view.getTitle(),
                view.getApplicationStatus(), view.getInterviewDate()
        ));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCompanyName()).isEqualTo("가상기업");
        assertThat(results.get(0).getTitle()).isEqualTo("백엔드 개발자");
        assertThat(results.get(0).getInterviewDate()).isEqualTo(LocalDate.of(2026, 10, 7));
    }

    private User createUser(String email) {
        User user = new User(email, "테스트 사용자", "NEW", null, "v1.0");
        entityManager.persistAndFlush(user);
        return user;
    }

    private UserApplication createApplication(User user, Long jobId, LocalDate interviewDate) {
        UserApplication application = new UserApplication(user, jobId);
        if (interviewDate != null) {
            application.updateInterviewDate(interviewDate);
        }
        entityManager.persistAndFlush(application);
        return application;
    }
}
