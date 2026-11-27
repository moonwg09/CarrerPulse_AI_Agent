# 경험 상태(4가지) 판정 테스트
from rag.judge.rules import (
    decide_status,
    match_rate,
    STATUS_HAVE,
    STATUS_PARTIAL,
    STATUS_UNKNOWN,
    STATUS_NONE,
)

# 1. 경험 있음
assert decide_status({
    "has_evidence": True,
    "scope_specified": True,
    "partially_satisfied": False,
}) == STATUS_HAVE

# 2. 일부만 있음
assert decide_status({
    "has_evidence": True,
    "scope_specified": False,
    "partially_satisfied": True,
}) == STATUS_PARTIAL

# 3. 기록 부족 (수행 범위 불명확)
assert decide_status({
    "has_evidence": True,
    "scope_specified": False,
    "partially_satisfied": False,
}) == STATUS_UNKNOWN

# 4. 기록 부족 (검색 근거 없음)
assert decide_status({
    "has_evidence": False,
    "scope_specified": False,
    "partially_satisfied": False,
}) == STATUS_UNKNOWN

# 5. 경험 없음 (사용자가 직접 확인)
assert decide_status(
    {"has_evidence": False},
    user_confirmed_no=True,
) == STATUS_NONE

# 6. 일치율 계산
results = [
    {"match_status": STATUS_HAVE},
    {"match_status": STATUS_HAVE},
    {"match_status": STATUS_PARTIAL},
    {"match_status": STATUS_UNKNOWN},
]

matched, total, rate, recommended = match_rate(results)

assert matched == 2
assert total == 4
assert rate == 0.5
assert recommended is True

print("JUDGE RULES TEST: PASS")
