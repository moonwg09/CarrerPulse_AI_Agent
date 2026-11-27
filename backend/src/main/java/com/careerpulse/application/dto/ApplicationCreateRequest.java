package com.careerpulse.application.dto;

import jakarta.validation.constraints.NotNull;

public record ApplicationCreateRequest(@NotNull Long jobId) {
}
