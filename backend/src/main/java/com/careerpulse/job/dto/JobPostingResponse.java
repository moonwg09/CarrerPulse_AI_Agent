package com.careerpulse.job.dto;

import com.careerpulse.job.entity.JobPostingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record JobPostingResponse(
        Long jobId,
        String source,
        String externalJobId,
        String companyName,
        String title,
        String jobCategory,
        String region,
        String description,
        LocalDate postedDate,
        LocalDate deadline,
        String originalUrl,
        JobPostingStatus status,
        LocalDateTime collectedAt,
        LocalDateTime updatedAt,
        List<JobRequirementResponse> requirements
) {
}
