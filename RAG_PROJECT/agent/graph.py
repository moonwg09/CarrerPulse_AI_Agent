"""Agent 실행 흐름 (SRS AGT-01~05).

시작 신호 → 사전 검증 → Tool 선택 → 실행 → 결과 전달
langgraph가 없으면 동일한 순서의 대체 실행을 사용한다.
"""
from typing import Any, Dict, List, TypedDict

from .tools import ALLOWED_TOOLS, DEFAULT_PLAN, compare_requirement, generate_guide
from . import tools as T


class AgentState(TypedDict, total=False):
    trigger_type: str          # user_request | schedule | condition
    run_key: str               # AGT-01: 중복 실행 방지 키
    user_id: int
    job_posting_id: int
    requirements: List[dict]
    store: Any
    api_key: str
    allowed_tools: List[str]
    plan: List[str]
    step_results: Dict[str, Any]
    needs_user_confirm: bool   # AGT-04
    errors: List[str]          # AGT-05
    stopped_reason: str


EXECUTED_RUN_KEYS = set()      # 운영에서는 DB의 처리 이력 테이블로 대체


def node_precheck(state: AgentState) -> AgentState:
    """AGT-03: 중복 신호, 필수 입력값, 실행 조건 확인."""
    if state.get("run_key") in EXECUTED_RUN_KEYS:
        return {**state, "stopped_reason": "중복 신호 - 이미 실행된 작업입니다."}
    if not state.get("user_id"):
        return {**state, "stopped_reason": "필수 입력값 누락: user_id"}
    if state["trigger_type"] not in ALLOWED_TOOLS:
        return {**state, "stopped_reason": f'알 수 없는 시작 신호: {state["trigger_type"]}'}
    if state["trigger_type"] == "user_request" and not state.get("job_posting_id"):
        return {**state, "stopped_reason": "필수 입력값 누락: job_posting_id"}
    return {**state, "allowed_tools": ALLOWED_TOOLS[state["trigger_type"]]}


def node_plan(state: AgentState) -> AgentState:
    """AGT-02: 허용 목록 안에서만 호출 순서를 정한다."""
    if state.get("stopped_reason"):
        return state
    wanted = DEFAULT_PLAN[state["trigger_type"]]
    return {**state, "plan": [t for t in wanted if t in state["allowed_tools"]]}


def node_run(state: AgentState) -> AgentState:
    """AGT-04·05: 결과 전달, 실패는 다음 단계로 넘기지 않는다."""
    if state.get("stopped_reason"):
        return state
    results: Dict[str, Any] = {}
    errors: List[str] = []
    comparisons: List[dict] = []

    for tool in state["plan"]:
        try:
            if tool == T.TOOL_COMPARE:
                comparisons = [compare_requirement(state["store"], r, state["user_id"],
                                                   api_key=state.get("api_key"))
                               for r in state.get("requirements", [])]
                results[tool] = [{k: v for k, v in c.items() if k != "hits"}
                                 for c in comparisons]
            elif tool == T.TOOL_GUIDE:
                # 보완이 필요한 항목을 우선하되, 없으면 근거가 있는 항목으로 안내한다
                target = next((c for c in comparisons
                               if c["match_status"] != "경험 있음" and c["hits"]), None)
                if target is None:
                    target = next((c for c in comparisons if c["hits"]), None)
                results[tool] = generate_guide(
                    next(r for r in state["requirements"]
                         if r["requirement_id"] == target["requirement_id"]),
                    target, state.get("api_key")) if target else {"status": "대상 없음"}
            elif tool == T.TOOL_VERIFY:
                g = results.get(T.TOOL_GUIDE, {})
                results[tool] = {"status": g.get("status", "-"),
                                 "dropped": g.get("attempts", [{}])[-1].get("dropped", [])}
            else:
                results[tool] = "ok"   # 공고수집·공고요구사항분석·알림예약은 다른 담당 모듈
        except Exception as e:  # noqa: BLE001
            errors.append(f"{tool}: {type(e).__name__} {e}")
            break               # 실패 후 다음 Tool로 넘어가지 않는다

    if not errors:
        EXECUTED_RUN_KEYS.add(state.get("run_key"))
    return {**state, "step_results": results, "errors": errors,
            "needs_user_confirm": T.TOOL_NOTIFY in state["plan"]}


def build_graph():
    """반환: (실행 객체, 사용 방식)"""
    try:
        from langgraph.graph import StateGraph, END

        g = StateGraph(AgentState)
        g.add_node("precheck", node_precheck)
        g.add_node("plan", node_plan)
        g.add_node("run", node_run)
        g.set_entry_point("precheck")
        g.add_conditional_edges(
            "precheck",
            lambda s: "stop" if s.get("stopped_reason") else "go",
            {"stop": END, "go": "plan"})
        g.add_edge("plan", "run")
        g.add_edge("run", END)
        return g.compile(), "langgraph"
    except Exception:
        class Sequential:
            def invoke(self, state):
                return node_run(node_plan(node_precheck(state)))
        return Sequential(), "sequential(대체)"
