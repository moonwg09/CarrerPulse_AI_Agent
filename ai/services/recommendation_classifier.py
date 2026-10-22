from schemas.match_schema import (
    JobMatchResult,
    RecommendationStatus,
)


def classify_job(
    match_result: JobMatchResult
) -> RecommendationStatus:

    if match_result.score is None:
        raise ValueError(
            "일치율 계산 결과(score)가 없습니다."
        )

    match_rate = match_result.score.overallMatchRate

    if match_rate >= 50.0:
        return RecommendationStatus.RECOMMENDED

    if match_rate >= 30.0:
        return RecommendationStatus.GROWTH_CANDIDATE

    return RecommendationStatus.EXCLUDED