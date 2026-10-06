"""실행 식별 키 생성 (SRS AGT-01 중복 실행 방지).

같은 신호가 두 번 들어와도 작업이 반복되지 않도록, 신호의 '정체성'을 문자열 하나로 요약한다.
키가 같으면 같은 작업으로 보고 두 번째는 실행하지 않는다.
"""
from datetime import date
from typing import Optional
from uuid import uuid4


def make_run_key(trigger_type: str, user_id: int,
                 job_posting_id: Optional[int] = None,
                 condition_key: Optional[str] = None,
                 run_date: Optional[str] = None) -> str:
    """시작 신호의 성격에 맞는 실행 키를 만든다.

    신호마다 '중복'의 의미가 다르기 때문에 규칙을 따로 둔다.

    - user_request: 사용자가 버튼을 두 번 누르면 두 번 실행되는 것이 정상이다.
      매번 다른 키를 주어 막지 않는다.
    - schedule: 하루 한 번이 원칙이다(일일 분석). 날짜를 키에 넣어,
      같은 날 두 번 호출되면 두 번째는 걸러지게 한다.
    - condition: 무엇이 바뀌어서 깨웠는지(condition_key)가 같으면 같은 작업이다.
      예를 들어 같은 공고가 다시 수정 알림을 보내도 한 번만 처리한다.
    """
    if trigger_type == "user_request":
        return f"user:{user_id}:{job_posting_id or '-'}:{uuid4().hex[:8]}"

    if trigger_type == "schedule":
        day = run_date or date.today().isoformat()
        return f"schedule:{user_id}:{job_posting_id or 'all'}:{day}"

    if trigger_type == "condition":
        return f"condition:{user_id}:{job_posting_id or '-'}:{condition_key or '-'}"

    return f"unknown:{user_id}:{uuid4().hex[:8]}"