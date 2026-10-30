package com.careerpulse.analysis.dto;

import java.util.List;

public record AnalysisDetailResponse(
        AnalysisSummaryResponse summary,
        List<AnalysisItemResponse> items,
        AnalysisStatusCount statusCount
) {
    public record AnalysisStatusCount(
            long experienced,
            long partial,
            long insufficientEvidence,
            long noExperience,
            long met,
            long unmet,
            long unknown
    ) {
    }
}
