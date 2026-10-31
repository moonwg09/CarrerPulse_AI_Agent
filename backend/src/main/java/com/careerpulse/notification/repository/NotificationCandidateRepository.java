package com.careerpulse.notification.repository;

import com.careerpulse.notification.entity.NotificationType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class NotificationCandidateRepository {
    private final JdbcTemplate jdbc;

    public NotificationCandidateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Candidate> find(LocalDate today) {
        List<Candidate> candidates = new ArrayList<>();
        candidates.addAll(jdbc.query("""
                SELECT DISTINCT u.user_id,u.email,u.name,j.job_id,j.title,j.company_name,
                       j.region,j.deadline,j.original_url,r.match_rate
                  FROM comparison_results r
                  JOIN users u ON u.user_id=r.user_id
                  JOIN job_postings j ON j.job_id=r.job_id
                 WHERE r.analysis_status='COMPLETED'
                   AND r.match_rate>=50
                   AND u.account_status='ACTIVE'
                   AND j.status='OPEN'
                   AND (j.deadline IS NULL OR j.deadline>=?)
                   AND NOT EXISTS (
                       SELECT 1 FROM user_applications a
                        WHERE a.user_id=u.user_id AND a.job_id=j.job_id
                   )
                   AND NOT EXISTS (
                       SELECT 1 FROM notification_deliveries n
                        WHERE n.user_id=u.user_id AND n.job_id=j.job_id
                          AND n.notification_type='NEW_RECOMMENDATION'
                   )
                """, (rs, row) -> candidate(rs, NotificationType.NEW_RECOMMENDATION), today));
        candidates.addAll(jdbc.query("""
                SELECT u.user_id,u.email,u.name,j.job_id,j.title,j.company_name,
                       j.region,j.deadline,j.original_url,
                       (SELECT r.match_rate
                          FROM comparison_results r
                         WHERE r.user_id=u.user_id
                           AND r.job_id=j.job_id
                           AND r.analysis_status='COMPLETED'
                         ORDER BY r.analyzed_at DESC,r.comparison_id DESC
                         LIMIT 1) AS match_rate
                  FROM job_bookmarks b
                  JOIN users u ON u.user_id=b.user_id
                  JOIN job_postings j ON j.job_id=b.job_id
                 WHERE u.account_status='ACTIVE'
                   AND j.status='OPEN'
                   AND j.deadline=?
                   AND NOT EXISTS (
                       SELECT 1 FROM notification_deliveries n
                        WHERE n.user_id=u.user_id AND n.job_id=j.job_id
                          AND n.notification_type='DEADLINE_REMINDER'
                   )
                """, (rs, row) -> candidate(rs, NotificationType.DEADLINE_REMINDER), today.plusDays(3)));
        return candidates;
    }

    private Candidate candidate(java.sql.ResultSet rs, NotificationType type)
            throws java.sql.SQLException {
        return new Candidate(
                rs.getLong("user_id"),
                rs.getString("email"),
                rs.getString("name"),
                rs.getLong("job_id"),
                rs.getString("title"),
                rs.getString("company_name"),
                rs.getString("region"),
                rs.getBigDecimal("match_rate"),
                rs.getObject("deadline", LocalDate.class),
                rs.getString("original_url"),
                type
        );
    }

    public record Candidate(
            Long userId,
            String email,
            String userName,
            Long jobId,
            String title,
            String companyName,
            String region,
            BigDecimal matchRate,
            LocalDate deadline,
            String originalUrl,
            NotificationType type
    ) {}
}
