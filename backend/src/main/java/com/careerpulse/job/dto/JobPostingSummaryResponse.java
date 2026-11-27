package com.careerpulse.job.dto;

import com.careerpulse.job.entity.JobPostingStatus;

import java.time.LocalDate;

public record JobPostingSummaryResponse(
        Long jobId,
        String companyName,
        String title,
        String jobCategory,
        String region,
        LocalDate postedDate,
        LocalDate deadline,
        String originalUrl,
        JobPostingStatus status
) {
}
