"""Agent 실행 요청·응답 DTO (SRS AGT-01~05).

Spring의 일정 실행기(@Scheduled)와 조건 감지 코드가 호출하는 규격이다.
필드를 바꾸면 백엔드 호출부도 함께 바뀌므로, 변경은 팀 합의 후에 한다.
"""
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field

from .analyze_schema import Requirement, TriggerType


class AgentRunRequest(BaseModel):
    """Agent 한 번 실행 요청."""
    trigger_type: TriggerType
    user_id: int
    job_posting_id: Optional[int] = Field(
        default=None, description="user_request 신호에서는 필수")
    requirements: List[Requirement] = Field(
        default=[], description="공고 요구 항목. 비어 있으면 비교 단계가 건너뛰어진다")

    # 아래 두 값은 중복 실행 판정에만 쓰인다.
    run_key: Optional[str] = Field(
        default=None, description="직접 지정하지 않으면 서버가 규칙에 따라 생성")
    condition_key: Optional[str] = Field(
        default=None, description="condition 신호를 깨운 원인 식별자 (예: posting_updated:1234)")
    run_date: Optional[str] = Field(
        default=None, description="schedule 신호의 기준일 YYYY-MM-DD. 없으면 오늘")


class AgentRunResponse(BaseModel):
    """Agent 실행 결과.

    status 세 가지의 뜻:
    - 실행: Tool이 돌았다
    - 중단: 사전 검증에서 걸러졌다(중복 신호, 필수값 누락 등). 실패가 아니다
    - 실패: Tool 실행 중 오류가 났다 (AGT-05)
    """
    trigger_type: TriggerType
    run_key: str
    status: str
    stopped_reason: str = ""
    allowed_tools: List[str] = []
    plan: List[str] = Field(default=[], description="실제 실행한 Tool 순서 (AGT-02)")
    results: Dict[str, Any] = {}
    errors: List[str] = []
    needs_user_confirm: bool = False


class RunHistoryResponse(BaseModel):
    """실행 이력 조회 결과. 점검용이며, 현재는 메모리 기준이다."""
    count: int
    run_keys: List[str]