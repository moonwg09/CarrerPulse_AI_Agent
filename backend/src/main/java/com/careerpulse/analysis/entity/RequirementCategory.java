package com.careerpulse.analysis.entity;

/**
 * 공고 요구사항의 구분. ai/schemas/job_schema.py 의 RequirementCategory 와 값을 맞춘다.
 */
public enum RequirementCategory {

    // 필수
    REQUIRED,

    // 우대
    PREFERRED,

    // 공고에 명시되지 않아 판단 불가
    UNCLEAR;

    public static RequirementCategory from(String value) {

        if (value == null) {
            return UNCLEAR;
        }

        try {
            return RequirementCategory.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            return UNCLEAR;
        }
    }
}
