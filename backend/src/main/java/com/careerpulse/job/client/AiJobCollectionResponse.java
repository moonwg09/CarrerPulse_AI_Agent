package com.careerpulse.job.client;

import java.util.List;

public record AiJobCollectionResponse(
        String jobKeyword,
        String region,
        int requestedLimit,
        int collectedCount,
        List<AiRawJobPosting> jobs
) {
}
