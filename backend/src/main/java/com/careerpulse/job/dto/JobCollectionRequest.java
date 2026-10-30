package com.careerpulse.job.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record JobCollectionRequest(
        @NotBlank String jobKeyword,
        @Min(1) @Max(10) int limit
) {
}
