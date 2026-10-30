package com.careerpulse.analysis.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * [추천조회] 응답.
 *
 * 정렬/제외 규칙은 ai/services/recommendation_service.py 와 동일하게
 * 저장된 분석 결과 기준으로 백엔드에서 다시 적용한다.
 */
public record RecommendationListResponse(

        String recommendationStatus,

        LocalDate baseDate,

        int totalCount,

        List<AnalysisSummaryResponse> items
) {
}
