package com.careerpulse.analysis.repository;

import com.careerpulse.analysis.entity.*;
import com.careerpulse.analysis.service.AnalysisScoreCalculator;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AnalysisResultRepositoryTest {

    @Autowired
    private AnalysisResultRepository resultRepository;

    @Autowired
    private JobRequirementRepository requirementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User owner;
    private User otherUser;
    private Long jobId;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(user("owner"));
        otherUser = userRepository.save(user("other"));

        String externalId = "ANALYSIS-TEST-" + UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO job_postings (
                    source, external_job_id, company_name, title,
                    job_category, region, description, deadline, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "TEST",
                externalId,
                "테스트 회사",
                "백엔드 개발자",
                "백엔드",
                "서울",
                "Spring Boot 개발 경험",
                LocalDate.now().plusDays(30),
                "OPEN"
        );
        jobId = jdbcTemplate.queryForObject(
                "SELECT job_id FROM job_postings WHERE source = ? AND external_job_id = ?",
                Long.class,
                "TEST",
                externalId
        );
    }

    @Test
    void 비교결과와_요구조건별_판정을_함께_저장한다() {
        JobRequirement requirement = requirementRepository.save(new JobRequirement(
                jobId,
                "REQ-TEST-" + UUID.randomUUID(),
                RequirementCategory.REQUIRED,
                ComparisonKind.EXPERIENCE,
                "Spring Boot 개발 경험",
                "Spring Boot 개발 경험",
                "Spring Boot 개발 경험 필수"
        ));
        AnalysisItem item = new AnalysisItem(
                requirement,
                null,
                Judgment.EXPERIENCED,
                "사용자 문서에서 수행 근거를 확인했습니다.",
                false
        );
        AnalysisScore score = new AnalysisScoreCalculator().calculate(List.of(item));
        AnalysisResult result = new AnalysisResult(owner, jobId);
        result.complete(score, List.of(item));

        resultRepository.saveAndFlush(result);

        AnalysisResult found = resultRepository
                .findFirstByUser_UserIdAndJobIdOrderByCreatedAtDesc(
                        owner.getUserId(),
                        jobId
                )
                .orElseThrow();
        assertThat(found.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(found.getMatchRate()).isEqualByComparingTo("100.00");
        assertThat(found.getItems()).hasSize(1);
        assertThat(found.getItems().get(0).getJudgment()).isEqualTo(Judgment.EXPERIENCED);
    }

    @Test
    void 다른_사용자는_분석결과를_조회할_수_없다() {
        AnalysisResult result = resultRepository.saveAndFlush(new AnalysisResult(owner, jobId));

        assertThat(resultRepository.findByAnalysisIdAndUser_UserId(
                result.getAnalysisId(),
                owner.getUserId()
        )).isPresent();
        assertThat(resultRepository.findByAnalysisIdAndUser_UserId(
                result.getAnalysisId(),
                otherUser.getUserId()
        )).isEmpty();
    }

    private User user(String prefix) {
        return new User(
                prefix + "-" + UUID.randomUUID() + "@example.com",
                "Repository Test User",
                "NEW",
                null,
                "ANALYSIS_TEST_V1"
        );
    }
}
