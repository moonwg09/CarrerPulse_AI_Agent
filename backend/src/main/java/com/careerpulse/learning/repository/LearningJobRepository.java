package com.careerpulse.learning.repository;

import com.careerpulse.learning.dto.LearningJobResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

// 공고 조회를 담당하는 객체로 spring에 등록한다.
@Repository
public class LearningJobRepository {

    private final JdbcTemplate jdbcTemplate;

    // Spring이 DB 연결 설정을 사용하는 JdbcTemplate을 주입한다.
    public LearningJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 지정한 직무 분류의 모집 중 공고를 최신순 최대 10개 조회한다.
    // jobCategory에는 DB에 저장된 직무 분류 값을 전달해야 한다.
    public List<LearningJobResponse> findLatestOpenJobs(
            String jobCategory,
            LocalDate referenceDate
    ) {
        String sql = """
                SELECT job_id,
                company_name,
                title,
                job_category,
                description,
                posted_date,
                deadline
        FROM job_postings
        WHERE job_category = ?
          AND status = 'OPEN'
          AND (deadline IS NULL OR deadline >= ?)
        ORDER BY posted_date DESC,
                 collected_at DESC,
                 job_id DESC
        LIMIT 10
        """;

        //? 자리에 값을 바인딩한다.
        // 사용자 입력을 SQL 문자열에 직접 이어 붙이지 않는다.
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new LearningJobResponse(
                            // 조회 결과를 DTO로 바꾸는 부분
                        rs.getLong("job_id"),
                        rs.getString("company_name"),
                        rs.getString("title"),
                        rs.getString("job_category"),
                        rs.getString("description"),
                        rs.getObject("posted_date", LocalDate.class),
                        rs.getObject("deadline", LocalDate.class)
                ),
                jobCategory,                            // 첫 번째 ?
                java.sql.Date.valueOf(referenceDate)    // 두 번째 ? - java의 LocalDate를 DB의 DATE컬럼과 비교할 때 사용할 java.sql.Date로 변환하는 코드
        );
    }
    // 여러 직무에 해당하는 공고를 합쳐 최신순 최대 10개 조회한다.
    public List<LearningJobResponse> findLatestOpenJobsForCategories(
            List<String> jobCategories,
            LocalDate referenceDate
    ) {
        // 관심 직무가 없으면 전체 공고를 조회하지 않고 빈 목록을 반환한다.
        if (jobCategories.isEmpty()) {
            return List.of();
        }

        // 직무 개수만큼 SQL의 ?를 만든다.
        // 예: 직무가 2개이면 "?, ?"
        String placeholders = String.join(
                ", ",
                Collections.nCopies(jobCategories.size(), "?")
        );

        String sql = """
            SELECT job_id,
                   company_name,
                   title,
                   job_category,
                   description,
                   posted_date,
                   deadline
            FROM job_postings
            WHERE job_category IN (%s)
              AND status = 'OPEN'
              AND (deadline IS NULL OR deadline >= ?)
            ORDER BY posted_date DESC,
                     collected_at DESC,
                     job_id DESC
            LIMIT 10
            """.formatted(placeholders);

        // 직무 값들을 먼저 넣고 마지막에 기준일을 넣는다.
        // 실제 입력값은 SQL 문자열에 붙이지 않고 바인딩한다.
        List<Object> parameters = new ArrayList<>(jobCategories);
        parameters.add(java.sql.Date.valueOf(referenceDate));

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new LearningJobResponse(
                        rs.getLong("job_id"),
                        rs.getString("company_name"),
                        rs.getString("title"),
                        rs.getString("job_category"),
                        rs.getString("description"),
                        rs.getObject("posted_date", LocalDate.class),
                        rs.getObject("deadline", LocalDate.class)
                ),
                parameters.toArray()
        );
    }
}
