"""Tool 정의와 시작 신호별 허용 목록 (SRS AGT-02)."""
from typing import Dict, List

from rag.prompt import build_judge_prompt, build_guide_prompt
from rag.judge import call_gemini, call_gemini_text, decide_status
from rag.retriever import search, build_query
from rag.verify import generate_and_verify

TOOL_JOB_ANALYZE = "공고요구사항분석"
TOOL_JOB_FETCH = "공고수집"
TOOL_SEARCH = "근거검색"
TOOL_COMPARE = "경험비교"
TOOL_GUIDE = "작성방향생성"
TOOL_VERIFY = "근거검증"
TOOL_NOTIFY = "알림예약"

# SRS 상세 기준(AGT-01)의 시작 신호 세 가지에 대한 허용 목록.
# Agent가 잘못 골라도 이 목록 밖의 Tool은 실행 계획에서 제외된다.
ALLOWED_TOOLS: Dict[str, List[str]] = {
    "user_request": [TOOL_JOB_ANALYZE, TOOL_SEARCH, TOOL_COMPARE, TOOL_GUIDE, TOOL_VERIFY],
    "schedule": [TOOL_JOB_FETCH, TOOL_JOB_ANALYZE, TOOL_COMPARE, TOOL_NOTIFY],
    "condition": [TOOL_SEARCH, TOOL_COMPARE, TOOL_NOTIFY],
}

# 신호별 기본 호출 순서. 실제 실행 계획은 위 허용 목록과 교집합을 취한다.
DEFAULT_PLAN: Dict[str, List[str]] = {
    "user_request": [TOOL_JOB_ANALYZE, TOOL_SEARCH, TOOL_COMPARE, TOOL_GUIDE, TOOL_VERIFY],
    "schedule": [TOOL_JOB_FETCH, TOOL_JOB_ANALYZE, TOOL_COMPARE, TOOL_NOTIFY],
    "condition": [TOOL_SEARCH, TOOL_COMPARE, TOOL_NOTIFY],
}


def compare_requirement(store, requirement: dict, user_id: int,
                        user_confirmed_no: bool = False, api_key=None) -> dict:
    """요구 항목 하나를 사용자 서류와 비교해 상태를 판정한다 (RAG-01~03, CMP-02)."""
    hits = search(store, build_query(requirement), user_id=user_id)

    # RAG-03: 근거가 없으면 LLM을 호출하지 않는다. 호출해 봐야 근거 없는 추측만 나온다.
    if not hits:
        return {"requirement_id": requirement["requirement_id"],
                "match_status": decide_status({}, user_confirmed_no),
                "match_score": 0.0, "cited": [], "missing_part": "",
                "analysis_reason": "확인 가능한 근거 없음", "hits": []}

    # LLM은 사실만 정리하고, 상태는 decide_status가 규칙으로 정한다.
    out = call_gemini(build_judge_prompt(requirement, hits), api_key)
    return {"requirement_id": requirement["requirement_id"],
            "match_status": decide_status(out, user_confirmed_no),
            "match_score": hits[0][0],
            "cited": out.get("cited", []),
            "missing_part": out.get("missing_part", ""),
            "analysis_reason": out.get("reason", ""),
            "hits": hits}


def generate_guide(requirement: dict, result: dict, api_key=None) -> dict:
    """작성 방향을 생성하고 근거를 검증한다 (GDE-08·09)."""
    # 근거가 없으면 생성 자체를 하지 않는다. 확인되지 않은 경험을 안내할 수 없기 때문이다.
    if not result["hits"]:
        return {"status": "보류", "lines": [], "attempts": [],
                "reason": "근거가 없어 작성 방향을 생성하지 않았습니다."}
    return generate_and_verify(
        build_prompt_fn=lambda: build_guide_prompt(requirement, result, result["hits"]),
        call_fn=lambda p: call_gemini_text(p, api_key),
        hits=result["hits"])
