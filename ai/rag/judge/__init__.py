"""판정 패키지의 공개 창구.

LLM 호출(llm_client)과 상태 결정 규칙(rules)을 한곳으로 모아 내보낸다.
역할이 나뉘어 있는 것이 핵심이다 — LLM은 사실만 정리하고,
4단계 상태는 rules의 decide_status가 규칙으로 정한다 (SRS CMP-02).
"""
from .llm_client import call_gemini, call_gemini_text
from .rules import (
    decide_status,
    match_rate,
    STATUS_HAVE,
    STATUS_PARTIAL,
    STATUS_UNKNOWN,
    STATUS_NONE,
)

__all__ = [
    "call_gemini",
    "call_gemini_text",
    "decide_status",
    "match_rate",
    "STATUS_HAVE",
    "STATUS_PARTIAL",
    "STATUS_UNKNOWN",
    "STATUS_NONE",
]