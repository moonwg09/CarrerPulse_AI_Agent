package com.careerpulse.review.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class CompanyReviewController {

    private final JdbcTemplate jdbcTemplate;

    public CompanyReviewController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    @GetMapping("/api/company-reviews")
    public List<Map<String, Object>> getCompanyReviews() {
        String sql = """
                SELECT
                    review_id,
                    user_id,
                    company_name,
                    job_name,
                    joined_year_month,
                    preparation_tip,
                    created_at,
                    updated_at
                    FROM company_reviews
                    ORDER BY created_at DESC, review_id DESC
                """;
        return jdbcTemplate.queryForList(sql);
    }
}
