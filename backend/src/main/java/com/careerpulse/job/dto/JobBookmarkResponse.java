package com.careerpulse.job.dto;

import java.time.LocalDateTime;

public record JobBookmarkResponse(
        Long jobId,
        boolean bookmarked,
        LocalDateTime createdAt
) {}
