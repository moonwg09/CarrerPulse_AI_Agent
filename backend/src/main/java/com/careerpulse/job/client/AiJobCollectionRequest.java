package com.careerpulse.job.client;

public record AiJobCollectionRequest(
        String jobKeyword,
        String region,
        int limit
) {
}
