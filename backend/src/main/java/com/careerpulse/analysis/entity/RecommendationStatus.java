package com.careerpulse.analysis.entity;

/**
 * 공고 추천 구분. ai/schemas/match_schema.py 의 RecommendationStatus 와 값을 맞춘다.
 */
public enum RecommendationStatus {

    // 추천 공고
    RECOMMENDED,

    // 성장 후보 공고
    GROWTH_CANDIDATE,

    // 추천 제외
    EXCLUDED;

    public static RecommendationStatus from(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return RecommendationStatus.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
