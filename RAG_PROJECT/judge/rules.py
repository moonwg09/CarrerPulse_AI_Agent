"""경험 상태 판정 규칙 (SRS CMP-02, RAG-03).

LLM이 아니라 이 함수가 최종 상태를 정한다.
"""
from typing import Dict, List

STATUS_HAVE = "경험 있음"
STATUS_PARTIAL = "일부만 있음"
STATUS_UNKNOWN = "기록 부족"
STATUS_NONE = "경험 없음"


def decide_status(llm_out: Dict, user_confirmed_no: bool = False) -> str:
    if user_confirmed_no:               # CMP-02: 사용자가 미수행을 확인한 경우에만
        return STATUS_NONE
    if not llm_out.get("has_evidence"):  # RAG-03: 근거 없음 != 경험 없음
        return STATUS_UNKNOWN
    if llm_out.get("partially_satisfied"):
        return STATUS_PARTIAL
    if llm_out.get("scope_specified"):
        return STATUS_HAVE
    return STATUS_UNKNOWN                # 근거는 있으나 수행 범위 불명


def match_rate(results: List[Dict]):
    """REC-02: '경험 있음'만 1개로 계산. 50%는 포함(이상)."""
    total = len(results)
    if total == 0:
        return 0, 0, 0.0, False
    matched = sum(1 for r in results if r["match_status"] == STATUS_HAVE)
    recommended = matched * 2 >= total   # 소수 오차를 피하려 정수로 비교
    return matched, total, matched / total, recommended
