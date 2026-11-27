package com.careerpulse.analysis.dto;

import java.util.List;

public record GuidanceResponse(
        String status,
        List<String> lines,
        int evidenceCount
) {
}
