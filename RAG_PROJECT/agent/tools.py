"""Tool 정의와 시작 신호별 허용 목록 (SRS AGT-02).

Agent는 '어느 Tool을 부를지' 고르기만 한다. 허용 목록 밖의 Tool은
여기서 걸러지므로, 잘못 선택해도 실행되지 않는다.
"""
from typing import Dict, List

from prompt import build_judge_prompt, build_guide_prompt
from judge import call_gemini, call_gemini_text, decide_status
from retriever import search, build_query
from verify import generate_and_verify

TOOL_JOB_ANALYZE = "공고요구사항분석"
TOOL_JOB_FETCH = "공고수집"
TOOL_SEARCH = "근거검색"
TOOL_COMPARE = "경험비교"
TOOL_GUIDE = "작성방향생성"
TOOL_VERIFY = "근거검증"
TOOL_NOTIFY = "알림예약"

# SRS 상세 기준(AGT-01)의 시작 신호 세 가지
ALLOWED_TOOLS: Dict[str, List[str]] = {
    "user_request": [TOOL_JOB_ANALYZE, TOOL_SEARCH, TOOL_COMPARE, TOOL_GUIDE, TOOL_VERIFY],
    "schedule": [TOOL_JOB_FETCH, TOOL_JOB_ANALYZE, TOOL_COMPARE, TOOL_NOTIFY],
    "condition": [TOOL_SEARCH, TOOL_COMPARE, TOOL_NOTIFY],
}

DEFAULT_PLAN: Dict[str, List[str]] = {
    "user_request": [TOOL_JOB_ANALYZE, TOOL_SEARCH, TOOL_COMPARE, TOOL_GUIDE, TOOL_VERIFY],
    "schedule": [TOOL_JOB_FETCH, TOOL_JOB_ANALYZE, TOOL_COMPARE, TOOL_NOTIFY],
    "condition": [TOOL_SEARCH, TOOL_COMPARE, TOOL_NOTIFY],
}


def compare_requirement(store, requirement: dict, user_id: int,
                        user_confirmed_no: bool = False, api_key=None) -> dict:
    """요구 항목 하나에 대한 비교·판정 (RAG-01~03, CMP-02)."""
    hits = search(store, build_query(requirement), user_id=user_id)
    if not hits:                      # RAG-03: LLM을 호출하지 않는다
        return {"requirement_id": requirement["requirement_id"],
                "match_status": decide_status({}, user_confirmed_no),
                "match_score": 0.0, "cited": [], "missing_part": "",
                "analysis_reason": "확인 가능한 근거 없음", "hits": []}
    out = call_gemini(build_judge_prompt(requirement, hits), api_key)
    return {"requirement_id": requirement["requirement_id"],
            "match_status": decide_status(out, user_confirmed_no),
            "match_score": hits[0][0],
            "cited": out.get("cited", []),
            "missing_part": out.get("missing_part", ""),
            "analysis_reason": out.get("reason", ""),
            "hits": hits}


def generate_guide(requirement: dict, result: dict, api_key=None) -> dict:
    """작성 방향 생성 + 근거 검증 (GDE-08·09)."""
    if not result["hits"]:
        return {"status": "보류", "lines": [], "attempts": [],
                "reason": "근거가 없어 작성 방향을 생성하지 않았습니다."}
    return generate_and_verify(
        build_prompt_fn=lambda: build_guide_prompt(requirement, result, result["hits"]),
        call_fn=lambda p: call_gemini_text(p, api_key),
        hits=result["hits"])
