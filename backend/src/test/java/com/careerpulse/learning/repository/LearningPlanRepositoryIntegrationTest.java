package com.careerpulse.learning.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

/** 명시적으로 켠 경우에만 실제 MySQL을 사용한다. 모든 테스트 데이터는 롤백한다. */
@EnabledIfEnvironmentVariable(named="LEARNING_DB_TEST", matches="true")
class LearningPlanRepositoryIntegrationTest {
    final ObjectMapper mapper = new ObjectMapper();
    JdbcTemplate jdbc;
    LearningPlanRepository repository;
    TransactionTemplate transaction;
    long userId;

    @BeforeEach
    void setup() {
        var source = new DriverManagerDataSource("jdbc:mysql://localhost:3306/careerpulse_mvp_dev",
                System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
        jdbc = new JdbcTemplate(source);
        repository = new LearningPlanRepository(jdbc, mapper);
        transaction = new TransactionTemplate(new JdbcTransactionManager(source));
        Long existing = jdbc.queryForObject("SELECT MIN(user_id) FROM users", Long.class);
        Assumptions.assumeTrue(existing != null, "검증에 사용할 기존 사용자 필요");
        userId = existing;
    }

    JsonNode plan() throws Exception {
        return mapper.readTree("""
            {"startDate":"2026-10-20","endDate":"2026-11-02","weeklyAvailableHours":8,
             "mode":"TEST","weeks":[{"weekNumber":1,"items":[{"activityType":"TECH_STUDY",
             "taskName":"가상 검증 활동","description":"롤백할 테스트","estimatedHours":2}]}],
             "unscheduled":[],"allItemsScheduled":true}
            """);
    }

    @Test
    void saveReloadAndOwnershipWithRollback() throws Exception {
        JsonNode data = plan();
        transaction.executeWithoutResult(status -> {
            try {
                var saved = repository.save(userId, data);
                var loaded = repository.latest(userId);
                // MySQL JSON 정규화 후 ID의 IntNode/LongNode 표현 차이는 내용 변경이 아니다.
                assertTrue(saved.equals((left, right) -> left.isNumber() && right.isNumber()
                        ? left.decimalValue().compareTo(right.decimalValue()) : left.equals(right) ? 0 : 1, loaded));
                assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM learning_plan_items WHERE plan_id=?",
                        Integer.class, saved.path("planId").asLong()));
                assertEquals(404, assertThrows(ResponseStatusException.class,
                        () -> repository.latest(Long.MAX_VALUE)).getStatusCode().value());
            } finally { status.setRollbackOnly(); }
        });
    }

    @Test
    void activityFailureRollsBackHeader() throws Exception {
        var broken = plan();
        ((com.fasterxml.jackson.databind.node.ObjectNode) broken.path("weeks").get(0).path("items").get(0))
                .put("estimatedHours", -1);
        Integer before = jdbc.queryForObject("SELECT COUNT(*) FROM learning_plans", Integer.class);
        assertThrows(DataAccessException.class,
                () -> transaction.executeWithoutResult(status -> repository.save(userId, broken)));
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM learning_plans", Integer.class));
    }
}
