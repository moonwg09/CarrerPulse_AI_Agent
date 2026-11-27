package com.careerpulse.application.dto;

import com.careerpulse.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ApplicationUpdateRequest(
        @NotNull ApplicationStatus status,
        LocalDate interviewDate
) {
}
