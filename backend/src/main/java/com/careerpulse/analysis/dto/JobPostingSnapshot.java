package com.careerpulse.analysis.dto;

import java.time.LocalDate;

public record JobPostingSnapshot(
        Long jobId,
        String title,
        String companyName,
        String jobCategory,
        String region,
        String description,
        LocalDate deadline,
        String status
) {
    public String toAnalysisText() {
        return "공고명: " + nullSafe(title) + "\n"
                + "회사명: " + nullSafe(companyName) + "\n"
                + "직무: " + nullSafe(jobCategory) + "\n"
                + "지역: " + nullSafe(region) + "\n\n"
                + nullSafe(description);
    }

    public boolean isClosed(LocalDate today) {
        return "CLOSED".equalsIgnoreCase(status)
                || (deadline != null && deadline.isBefore(today));
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
