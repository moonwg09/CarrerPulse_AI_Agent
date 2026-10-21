"""경험 상태 판정 규칙 (SRS CMP-02, RAG-03)."""
from typing import Dict, List

STATUS_HAVE = "경험 있음"
STATUS_PARTIAL = "일부만 있음"
STATUS_UNKNOWN = "기록 부족"
STATUS_NONE = "경험 없음"


def decide_status(llm_out: Dict, user_confirmed_no: bool = False) -> str:
    """LLM이 정리한 사실을 4단계 상태로 매핑한다.

    SRS가 "최종 상태는 정해진 판정 규칙으로 결정한다"고 명시했으므로,
    상태를 정하는 곳은 LLM이 아니라 이 함수 한 곳이다.
    """
    # '경험 없음'은 사용자가 직접 미수행을 확인한 경우에만 부여한다(CMP-02·CMP-03).
    if user_confirmed_no:
        return STATUS_NONE

    # 근거를 찾지 못한 것은 '경험이 없다'가 아니라 '확인할 수 없다'이다(RAG-03).
    if not llm_out.get("has_evidence"):
        return STATUS_UNKNOWN

    # 근거가 있을 때는 요구 범위를 얼마나 충족했는지로 나눈다.
    # 범위가 적혀 있지 않으면 경험이 있어도 '기록 부족'이다(서류 보완 대상).
    if llm_out.get("partially_satisfied"):
        return STATUS_PARTIAL
    if llm_out.get("scope_specified"):
        return STATUS_HAVE
    return STATUS_UNKNOWN


def match_rate(results: List[Dict]):
    """자격요건 일치율을 계산한다 (SRS REC-02). 반환: (일치 수, 전체, 비율, 추천 여부)"""
    # '경험 있음'만 1개로 세고 나머지는 0개다. 추천 기준 50%는 '이상'이므로 포함한다.
    # 비율을 소수로 비교하면 49.999% 같은 오차가 생길 수 있어 정수로 비교한다.
    total = len(results)
    if total == 0:
        return 0, 0, 0.0, False
    matched = sum(1 for r in results if r["match_status"] == STATUS_HAVE)
    return matched, total, matched / total, matched * 2 >= total
