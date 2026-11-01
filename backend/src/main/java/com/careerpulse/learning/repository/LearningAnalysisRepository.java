package com.careerpulse.learning.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 승인된 공고 요구조건과 사용자의 최신 비교 결과를 학습 분석 입력으로 조회한다. */
@Repository
public class LearningAnalysisRepository {
    private final JdbcTemplate jdbc;

    public LearningAnalysisRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RequirementRow> findRequirements(List<Long> jobIds) {
        if (jobIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(jobIds.size(), "?"));
        return jdbc.query("""
            SELECT requirement_id,job_id,requirement_type,comparison_kind,
                   requirement_name,evaluation_rule
            FROM job_requirements
            WHERE job_id IN (%s)
            ORDER BY job_id,requirement_id
            """.formatted(placeholders), (rs, row) -> new RequirementRow(
                rs.getLong("requirement_id"),
                rs.getLong("job_id"),
                rs.getString("requirement_type"),
                rs.getString("comparison_kind"),
                rs.getString("requirement_name"),
                rs.getString("evaluation_rule")
            ), jobIds.toArray());
    }

    public List<AssessmentRow> findLatestAssessments(long userId, List<Long> jobIds) {
        if (jobIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(jobIds.size(), "?"));
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        parameters.addAll(jobIds);
        return jdbc.query("""
            WITH ranked AS (
                SELECT comparison_id,job_id,
                       ROW_NUMBER() OVER (
                           PARTITION BY job_id
                           ORDER BY COALESCE(analyzed_at,created_at) DESC,comparison_id DESC
                       ) AS ranking_no
                FROM comparison_results
                WHERE user_id=?
                  AND analysis_status='COMPLETED'
                  AND job_id IN (%s)
            )
            SELECT r.job_id,jr.requirement_id,jr.requirement_type,jr.comparison_kind,
                   jr.requirement_name,jr.evaluation_rule,ci.judgment,
                   ci.analysis_reason,ci.user_confirmed
            FROM ranked r
            JOIN comparison_items ci ON ci.comparison_id=r.comparison_id
            JOIN job_requirements jr ON jr.requirement_id=ci.requirement_id
            WHERE r.ranking_no=1
            ORDER BY r.job_id,jr.requirement_id
            """.formatted(placeholders), (rs, row) -> new AssessmentRow(
                rs.getLong("job_id"),
                rs.getLong("requirement_id"),
                rs.getString("requirement_type"),
                rs.getString("comparison_kind"),
                rs.getString("requirement_name"),
                rs.getString("evaluation_rule"),
                rs.getString("judgment"),
                rs.getString("analysis_reason"),
                rs.getBoolean("user_confirmed")
            ), parameters.toArray());
    }

    public record RequirementRow(
            long requirementId,
            long jobId,
            String requirementType,
            String comparisonKind,
            String requirementName,
            String evaluationRule
    ) {}

    public record AssessmentRow(
            long jobId,
            long requirementId,
            String requirementType,
            String comparisonKind,
            String requirementName,
            String evaluationRule,
            String judgment,
            String reason,
            boolean userConfirmed
    ) {}
}
