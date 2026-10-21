package com.careerpulse.analysis.entity;

/**
 * 요구사항 1건에 대한 최종 판정 상태.
 *
 * AI(FastAPI)는 사실만 돌려주고, 저장 가능한 4가지 상태로 바꾸는 책임은
 * 백엔드(이 enum + from 메서드)가 가진다.
 * 알 수 없는 값이 오면 임의로 '경험 있음' 쪽으로 올리지 않고
 * INSUFFICIENT_EVIDENCE(기록 부족)로 내린다.
 */
public enum MatchStatus {

    // 경험 있음
    EXPERIENCED,

    // 일부만 있음
    PARTIAL,

    // 기록 부족
    INSUFFICIENT_EVIDENCE,

    // 경험 없음
    NO_EXPERIENCE;

    public static MatchStatus from(String value) {

        if (value == null) {
            return INSUFFICIENT_EVIDENCE;
        }

        try {
            return MatchStatus.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            return INSUFFICIENT_EVIDENCE;
        }
    }
}
