package com.careerpulse.review.dto;

import com.careerpulse.review.entity.CompanyReview;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record CompanyReviewResponse(
        @JsonProperty("review_id") Long reviewId,
        @JsonProperty("company_name") String companyName,
        @JsonProperty("job_name") String jobName,
        @JsonProperty("joined_year_month") String joinedYearMonth,
        @JsonProperty("preparation_tip") String preparationTip,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {

    public static CompanyReviewResponse from(CompanyReview review) {
        return new CompanyReviewResponse(
                review.getReviewId(),
                review.getCompanyName(),
                review.getJobName(),
                review.getJoinedYearMonth(),
                review.getPreparationTip(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}