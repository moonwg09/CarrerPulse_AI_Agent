package com.careerpulse.analysis.entity;

/**
 * 공고 요구사항의 구분. ai/schemas/job_schema.py 의 RequirementCategory 와 값을 맞춘다.
 */
public enum RequirementCategory {

    // 필수
    REQUIRED,

    // 우대
    PREFERRED;

    public static RequirementCategory from(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("요구조건의 필수/우대 구분이 없습니다.");
        }

        try {
            return RequirementCategory.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "지원하지 않는 요구조건 구분입니다: " + value,
                    e
            );
        }
    }
}
