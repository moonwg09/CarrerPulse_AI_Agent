package com.careerpulse.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_reviews")
public class CompanyReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "joined_year_month", columnDefinition = "CHAR(7)")
    private String joinedYearMonth;

    @Column(name = "preparation_tip", columnDefinition = "TEXT")
    private String preparationTip;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime updatedAt;

    protected CompanyReview() {
    }
    public Long getReviewId() {
        return reviewId;
    }

    public  String getCompanyName() {
        return companyName;
    }

    public String getJobName() {
        return jobName;
    }

    public String getJoinedYearMonth() {
        return joinedYearMonth;
    }

    public String getPreparationTip() {
        return preparationTip;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
