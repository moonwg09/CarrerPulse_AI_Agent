package com.careerpulse.job.dto;

import java.util.List;

public record JobCollectionResponse(
        String jobKeyword,
        String region,
        int requestedLimit,
        int collectedCount,
        List<JobPostingResponse> jobs
) {
}
