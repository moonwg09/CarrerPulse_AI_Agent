package com.careerpulse.learning.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;

/** 승인된 학습계획 헤더와 활동 테이블만 사용해 계획을 저장하고 조회한다. */
@Repository
public class LearningPlanRepository {
    private final JdbcTemplate jdbc;

    public LearningPlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int invalidateActive(long userId) {
        return jdbc.update(
                "UPDATE learning_plans SET status='INVALIDATED' WHERE user_id=? AND status='ACTIVE'",
                userId
        );
    }

    @Transactional
    public JsonNode save(long userId, JsonNode result) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                INSERT INTO learning_plans(user_id,start_date,end_date,weekly_available_hours)
                VALUES (?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setString(2, result.path("startDate").asText());
            statement.setString(3, result.path("endDate").asText());
            statement.setBigDecimal(4, result.path("weeklyAvailableHours").decimalValue());
            return statement;
        }, key);

        if (key.getKey() == null) {
            throw new IllegalStateException("생성된 계획 ID가 없습니다.");
        }
        long planId = key.getKey().longValue();

        for (var week : result.path("weeks")) {
            int sequence = 0;
            for (var item : week.path("items")) {
                jdbc.update("""
                    INSERT INTO learning_plan_items(
                        plan_id,week_number,sequence_no,category,
                        task_name,description,estimated_hours
                    ) VALUES (?,?,?,?,?,?,?)
                    """,
                    planId,
                    week.path("weekNumber").asInt(),
                    ++sequence,
                    item.path("activityType").asText(),
                    item.path("taskName").asText(),
                    item.path("description").asText(),
                    item.path("estimatedHours").decimalValue()
                );
            }
        }

        ObjectNode response = result.deepCopy();
        response.put("planId", planId);
        return response;
    }

    @Transactional(readOnly = true)
    public JsonNode latest(long userId) {
        List<PlanRow> plans = jdbc.query("""
            SELECT plan_id,start_date,end_date,weekly_available_hours
            FROM learning_plans
            WHERE user_id=? AND status='ACTIVE'
            ORDER BY plan_id DESC
            LIMIT 1
            """, (rs, row) -> new PlanRow(
                rs.getLong("plan_id"),
                rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class),
                rs.getBigDecimal("weekly_available_hours")
            ), userId);

        if (plans.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "생성된 학습계획이 없습니다. 시작일·종료일·주당 학습 가능 시간을 입력해 계획을 생성하세요."
            );
        }

        PlanRow plan = plans.getFirst();
        List<ItemRow> items = jdbc.query("""
            SELECT week_number,sequence_no,category,task_name,description,estimated_hours
            FROM learning_plan_items
            WHERE plan_id=?
            ORDER BY week_number,sequence_no
            """, (rs, row) -> new ItemRow(
                rs.getInt("week_number"),
                rs.getInt("sequence_no"),
                rs.getString("category"),
                rs.getString("task_name"),
                rs.getString("description"),
                rs.getBigDecimal("estimated_hours")
            ), plan.planId());

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("planId", plan.planId());
        result.put("startDate", plan.startDate().toString());
        result.put("endDate", plan.endDate().toString());
        result.put("weeklyAvailableHours", plan.weeklyAvailableHours());
        result.put("estimateNotice", "예상 소요 시간은 추정치입니다.");

        var weeks = new LinkedHashMap<Integer, ObjectNode>();
        for (ItemRow item : items) {
            ObjectNode week = weeks.computeIfAbsent(
                item.weekNumber(),
                number -> createWeek(plan, number)
            );
            ObjectNode activity = JsonNodeFactory.instance.objectNode();
            activity.put("sequenceNo", item.sequenceNo());
            activity.put("activityType", item.category());
            activity.put("taskName", item.taskName());
            activity.put("description", item.description());
            activity.put("estimatedHours", item.estimatedHours());
            week.withArray("items").add(activity);
            BigDecimal total = week.path("totalEstimatedHours").decimalValue()
                .add(item.estimatedHours());
            week.put("totalEstimatedHours", total);
        }

        var weekArray = result.putArray("weeks");
        weeks.values().forEach(weekArray::add);
        return result;
    }

    private ObjectNode createWeek(PlanRow plan, int weekNumber) {
        LocalDate start = plan.startDate().plusDays((long) (weekNumber - 1) * 7);
        LocalDate end = start.plusDays(6);
        if (end.isAfter(plan.endDate())) {
            end = plan.endDate();
        }
        long days = end.toEpochDay() - start.toEpochDay() + 1;
        BigDecimal available = plan.weeklyAvailableHours()
            .multiply(BigDecimal.valueOf(days))
            .divide(BigDecimal.valueOf(7), 2, RoundingMode.DOWN);

        ObjectNode week = JsonNodeFactory.instance.objectNode();
        week.put("weekNumber", weekNumber);
        week.put("startDate", start.toString());
        week.put("endDate", end.toString());
        week.put("availableHours", available);
        week.put("totalEstimatedHours", BigDecimal.ZERO);
        week.putArray("items");
        return week;
    }

    private record PlanRow(
        long planId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal weeklyAvailableHours
    ) {}

    private record ItemRow(
        int weekNumber,
        int sequenceNo,
        String category,
        String taskName,
        String description,
        BigDecimal estimatedHours
    ) {}
}
