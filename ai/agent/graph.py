"""Agent 실행 흐름 (SRS AGT-01~05).

시작 신호 → 사전 검증 → Tool 선택 → 실행 → 결과 전달
langgraph가 없으면 같은 순서의 대체 실행으로 내려간다.
"""
from typing import Any, Dict, List, TypedDict

from .tools import ALLOWED_TOOLS, DEFAULT_PLAN, compare_requirement, generate_guide
from . import tools as T


class AgentState(TypedDict, total=False):
    """노드 사이에서 넘겨지는 상태. LangGraph의 State에 해당한다."""
    trigger_type: str          # user_request | schedule | condition (AGT-01)
    run_key: str               # 중복 실행 방지 키 (AGT-01)
    user_id: int
    job_posting_id: int
    requirements: List[dict]
    store: Any
    api_key: str
    allowed_tools: List[str]   # AGT-02
    plan: List[str]
    step_results: Dict[str, Any]
    needs_user_confirm: bool   # AGT-04
    errors: List[str]          # AGT-05
    stopped_reason: str


# 이미 실행한 신호를 기억한다. 운영에서는 DB의 처리 이력 테이블로 바꾼다.
EXECUTED_RUN_KEYS = set()


def node_precheck(state: AgentState) -> AgentState:
    """AGT-03: 실제 조회·저장 전에 중복 신호와 필수 입력값을 확인한다."""
    # 조건을 만족하지 못하면 Tool을 부르지 않고 사유만 남긴다.
    # 같은 신호가 두 번 와도 작업이 반복되지 않도록 run_key를 먼저 검사한다(AGT-01).
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
    # 허용 목록과 교집합을 취하므로, 목록 밖의 Tool은 계획 단계에서 사라진다.
    if state.get("stopped_reason"):
        return state
    wanted = DEFAULT_PLAN[state["trigger_type"]]
    return {**state, "plan": [t for t in wanted if t in state["allowed_tools"]]}


def node_run(state: AgentState) -> AgentState:
    """AGT-04·05: Tool을 순서대로 실행하고 결과를 다음 단계로 넘긴다."""
    if state.get("stopped_reason"):
        return state

    results: Dict[str, Any] = {}
    errors: List[str] = []
    comparisons: List[dict] = []

    for tool in state["plan"]:
        try:
            # 경험비교: 공고의 요구 항목을 하나씩 비교한다. hits(근거)는 내부 전달용이라
            # 결과에서는 빼고, 뒤 단계인 작성방향생성에서만 사용한다.
            if tool == T.TOOL_COMPARE:
                comparisons = [compare_requirement(state["store"], r, state["user_id"],
                                                   api_key=state.get("api_key"))
                               for r in state.get("requirements", [])]
                results[tool] = [{k: v for k, v in c.items() if k != "hits"}
                                 for c in comparisons]

            # 작성방향생성: 보완이 필요한 항목을 먼저 고르고, 없으면 근거가 있는 항목을 쓴다.
            elif tool == T.TOOL_GUIDE:
                target = next((c for c in comparisons
                               if c["match_status"] != "경험 있음" and c["hits"]), None)
                if target is None:
                    target = next((c for c in comparisons if c["hits"]), None)
                results[tool] = generate_guide(
                    next(r for r in state["requirements"]
                         if r["requirement_id"] == target["requirement_id"]),
                    target, state.get("api_key")) if target else {"status": "대상 없음"}

            # 근거검증: 생성 단계에서 이미 검증을 수행하므로 그 결과를 요약해 남긴다.
            elif tool == T.TOOL_VERIFY:
                g = results.get(T.TOOL_GUIDE, {})
                results[tool] = {"status": g.get("status", "-"),
                                 "dropped": g.get("attempts", [{}])[-1].get("dropped", [])}

            # 공고수집·공고요구사항분석·알림예약은 다른 팀원 담당 모듈이라 자리만 둔다.
            else:
                results[tool] = "ok"

        except Exception as e:  # noqa: BLE001
            # AGT-05: 실패한 결과를 정상 결과처럼 다음 Tool로 넘기지 않는다.
            errors.append(f"{tool}: {type(e).__name__} {e}")
            break

    # 성공한 실행만 이력에 남긴다. 실패한 작업은 다시 시도할 수 있어야 하기 때문이다.
    if not errors:
        EXECUTED_RUN_KEYS.add(state.get("run_key"))

    return {**state, "step_results": results, "errors": errors,
            "needs_user_confirm": T.TOOL_NOTIFY in state["plan"]}


def build_graph():
    """실행 그래프를 만든다. 반환: (실행 객체, 사용 방식)"""
    # langgraph가 설치되어 있으면 상태 그래프로 구성한다.
    # 사전 검증에서 중단되면 이후 노드로 가지 않도록 조건부 분기를 건다.
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

    # langgraph가 없어도 같은 순서로 돌 수 있게 대체 실행을 제공한다.
    except Exception:
        class Sequential:
            def invoke(self, state):
                return node_run(node_plan(node_precheck(state)))
        return Sequential(), "sequential(대체)"
