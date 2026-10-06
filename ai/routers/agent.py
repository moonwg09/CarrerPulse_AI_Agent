"""Agent 시작 신호 입구 (SRS AGT-01).

세 가지 신호를 모두 같은 흐름으로 처리한다.
- user_request: 사용자가 화면에서 분석을 요청
- schedule:     Spring의 일정 실행기가 매일 호출
- condition:    공고 변경 등 조건이 감지되었을 때 호출
"""
from fastapi import APIRouter

from agent.graph import EXECUTED_RUN_KEYS, build_graph
from agent.run_key import make_run_key
from rag import store
from schemas.agent_schema import AgentRunRequest, AgentRunResponse, RunHistoryResponse

router = APIRouter(prefix="/agent", tags=["agent"])

# 그래프는 실행마다 새로 만들 필요가 없으므로 모듈 적재 시 한 번만 구성한다.
_graph, _mode = build_graph()


@router.post("/run", response_model=AgentRunResponse)
def run_agent(req: AgentRunRequest):
    """시작 신호를 받아 Agent를 한 번 실행한다.

    세 신호를 엔드포인트 하나로 받는 이유: 신호에 따라 달라지는 것은
    '어떤 Tool이 허용되는가'뿐이고, 그 판단은 이미 Agent 안(ALLOWED_TOOLS)에 있다.
    입구를 신호마다 따로 만들면 같은 규칙이 바깥에도 생겨 두 곳이 어긋나게 된다.
    """
    # 호출자가 키를 주지 않으면 신호 성격에 맞는 규칙으로 생성한다.
    run_key = req.run_key or make_run_key(
        req.trigger_type, req.user_id, req.job_posting_id,
        req.condition_key, req.run_date)

    state = {
        "trigger_type": req.trigger_type,
        "run_key": run_key,
        "user_id": req.user_id,
        "job_posting_id": req.job_posting_id,
        # Agent 내부 Tool은 평범한 dict를 기대하므로 Pydantic 모델을 풀어서 넘긴다.
        "requirements": [r.model_dump() for r in req.requirements],
        "store": store.get_store(),
    }

    out = _graph.invoke(state)

    # 중단과 실패를 구분해서 돌려준다.
    # 중복 신호로 걸러진 것은 정상 동작이므로 실패로 보고하면 안 된다.
    if out.get("stopped_reason"):
        status = "중단"
    elif out.get("errors"):
        status = "실패"
    else:
        status = "실행"

    return AgentRunResponse(
        trigger_type=req.trigger_type,
        run_key=run_key,
        status=status,
        stopped_reason=out.get("stopped_reason", ""),
        allowed_tools=out.get("allowed_tools", []),
        plan=out.get("plan", []),
        results=out.get("step_results", {}),
        errors=out.get("errors", []),
        needs_user_confirm=out.get("needs_user_confirm", False),
    )


@router.get("/runs", response_model=RunHistoryResponse)
def list_runs():
    """실행 이력 조회 (점검용).

    현재 이력은 프로세스 메모리에 있어 서버를 끄면 사라진다.
    운영에서는 DB 테이블로 옮겨야 하며, 그 작업이 다음 단계다.
    """
    keys = sorted(k for k in EXECUTED_RUN_KEYS if k)
    return RunHistoryResponse(count=len(keys), run_keys=keys)


@router.get("/mode")
def agent_mode():
    """Agent가 langgraph로 도는지 대체 실행으로 도는지 확인한다."""
    return {"agent_mode": _mode}