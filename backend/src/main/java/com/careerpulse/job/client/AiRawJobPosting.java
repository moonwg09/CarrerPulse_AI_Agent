package com.careerpulse.job.client;

import java.time.LocalDate;

public record AiRawJobPosting(
        String jobId,
        String externalJobId,
        String title,
        String companyName,
        String region,
        LocalDate postedDate,
        LocalDate closeDate,
        String url,
        String content,
        String source
) {
}
