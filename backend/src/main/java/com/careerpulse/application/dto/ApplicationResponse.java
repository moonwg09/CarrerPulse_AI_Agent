package com.careerpulse.application.dto;

import com.careerpulse.application.entity.ApplicationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ApplicationResponse(
        Long applicationId,
        Long jobId,
        String jobTitle,
        String companyName,
        ApplicationStatus status,
        LocalDate interviewDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
