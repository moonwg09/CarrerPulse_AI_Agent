package com.careerpulse.learning.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LearningPlanRepository.class)
class LearningPlanRepositoryIntegrationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    LearningPlanRepository repository;

    long userId;

    @BeforeEach
    void createUser() {
        String email = "learning-test-" + UUID.randomUUID() + "@example.com";
        jdbc.update("""
            INSERT INTO users(email,name,career_type,consented,account_status)
            VALUES (?, '학습계획 테스트', 'NEW', FALSE, 'ACTIVE')
            """, email);
        userId = jdbc.queryForObject(
            "SELECT user_id FROM users WHERE email=?",
            Long.class,
            email
        );
    }

    private JsonNode plan(String taskName) throws Exception {
        return mapper.readTree("""
            {
              "startDate":"2026-10-20",
              "endDate":"2026-10-29",
              "weeklyAvailableHours":8,
              "mode":"TEST",
              "weeks":[
                {
                  "weekNumber":1,
                  "items":[{
                    "activityType":"TECH_STUDY",
                    "taskName":"%s",
                    "description":"학습 결과물을 작성합니다.",
                    "estimatedHours":2
                  }]
                },
                {
                  "weekNumber":2,
                  "items":[{
                    "activityType":"PROJECT",
                    "taskName":"미니 프로젝트",
                    "description":"API를 구현합니다.",
                    "estimatedHours":3
                  }]
                }
              ],
              "unscheduled":[],
              "allItemsScheduled":true
            }
            """.formatted(taskName));
    }

    @Test
    void savesAndRebuildsLatestPlanFromApprovedTables() throws Exception {
        JsonNode saved = repository.save(userId, plan("SQL 실행 계획 분석"));
        long planId = saved.path("planId").asLong();

        assertTrue(planId > 0);
        assertEquals(1, jdbc.queryForObject(
            "SELECT COUNT(*) FROM learning_plans WHERE plan_id=?",
            Integer.class,
            planId
        ));
        assertEquals(2, jdbc.queryForObject(
            "SELECT COUNT(*) FROM learning_plan_items WHERE plan_id=?",
            Integer.class,
            planId
        ));

        JsonNode latest = repository.latest(userId);
        assertEquals(planId, latest.path("planId").asLong());
        assertEquals("2026-10-20", latest.path("startDate").asText());
        assertEquals(8.0, latest.path("weeklyAvailableHours").asDouble());
        assertEquals(2, latest.path("weeks").size());
        assertEquals("SQL 실행 계획 분석",
            latest.path("weeks").get(0).path("items").get(0).path("taskName").asText());
        assertEquals(3.42,
            latest.path("weeks").get(1).path("availableHours").asDouble());
        assertEquals(1,
            latest.path("weeks").get(1).path("items").get(0).path("sequenceNo").asInt());
    }

    @Test
    void returnsOnlyCurrentUsersLatestActivePlan() throws Exception {
        long firstId = repository.save(userId, plan("첫 번째 계획")).path("planId").asLong();
        long secondId = repository.save(userId, plan("두 번째 계획")).path("planId").asLong();

        assertTrue(secondId > firstId);
        assertEquals(secondId, repository.latest(userId).path("planId").asLong());
        assertEquals(404, assertThrows(
            ResponseStatusException.class,
            () -> repository.latest(Long.MAX_VALUE)
        ).getStatusCode().value());
    }
}
