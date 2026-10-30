package com.careerpulse.analysis.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AnalysisCompareRequest(
        @NotNull(message = "공고 ID(jobId)는 필수입니다.")
        @Positive(message = "jobId는 1 이상이어야 합니다.")
        Long jobId
) {
}
