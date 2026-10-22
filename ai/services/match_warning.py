from typing import List

from schemas.match_schema import JobMatchResult


MIN_EVALUATION_COVERAGE = 60.0


def build_match_warnings(
    match_result: JobMatchResult
) -> List[str]:

    warnings = []

    if match_result.score is None:
        return warnings

    if (
        match_result.score.evaluationCoverage
        < MIN_EVALUATION_COVERAGE
    ):
        warnings.append(
            "현재 사용자 데이터로 판단할 수 없는 요구조건이 많아 "
            "일치율 해석에 주의가 필요합니다."
        )

    return warnings