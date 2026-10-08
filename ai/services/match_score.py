from schemas.job_schema import RequirementCategory
from schemas.match_schema import (
    JobMatchResult,
    MatchScoreResult,
    MatchStatus,
    ObjectiveStatus,
)


def calculate_match_score(
    match_result: JobMatchResult
) -> MatchScoreResult:

    # =========================
    # 전체 요구조건
    # =========================

    total_count = 0
    evaluable_count = 0
    matched_count = 0

    # =========================
    # 필수 조건
    # =========================

    required_total = 0
    required_evaluable = 0
    required_matched = 0

    # =========================
    # 우대 조건
    # =========================

    preferred_total = 0
    preferred_evaluable = 0
    preferred_matched = 0

    # =========================
    # 객관 조건
    # =========================

    objective_total = 0
    objective_evaluable = 0
    objective_matched = 0


    # ---------------------------------
    # 1. 기술 / 경험 요구사항 계산
    # ---------------------------------

    for match in match_result.requirementMatches:

        total_count += 1

        is_evaluable = (
            match.status != MatchStatus.INSUFFICIENT_EVIDENCE
        )

        is_matched = (
            match.status == MatchStatus.EXPERIENCED
        )

        if is_evaluable:
            evaluable_count += 1

            if is_matched:
                matched_count += 1


        # 필수 조건
        if match.requirementCategory == RequirementCategory.REQUIRED:

            required_total += 1

            if is_evaluable:
                required_evaluable += 1

                if is_matched:
                    required_matched += 1


        # 우대 조건
        elif match.requirementCategory == RequirementCategory.PREFERRED:

            preferred_total += 1

            if is_evaluable:
                preferred_evaluable += 1

                if is_matched:
                    preferred_matched += 1


    # ---------------------------------
    # 2. 객관 조건 계산
    # ---------------------------------

    for condition in match_result.objectiveConditionMatches:

        total_count += 1
        objective_total += 1

        is_evaluable = (
            condition.status != ObjectiveStatus.UNCLEAR
        )

        is_matched = (
            condition.status == ObjectiveStatus.SATISFIED
        )

        if is_evaluable:
            evaluable_count += 1
            objective_evaluable += 1

            if is_matched:
                matched_count += 1
                objective_matched += 1


        # 객관 조건이면서 필수 조건
        if condition.requirementCategory == RequirementCategory.REQUIRED:

            required_total += 1

            if is_evaluable:
                required_evaluable += 1

                if is_matched:
                    required_matched += 1


        # 객관 조건이면서 우대 조건
        elif condition.requirementCategory == RequirementCategory.PREFERRED:

            preferred_total += 1

            if is_evaluable:
                preferred_evaluable += 1

                if is_matched:
                    preferred_matched += 1


    # ---------------------------------
    # 3. 비율 계산
    # ---------------------------------

    overall_match_rate = (
        matched_count / evaluable_count * 100
        if evaluable_count > 0
        else 0.0
    )

    required_match_rate = (
        required_matched / required_evaluable * 100
        if required_evaluable > 0
        else 0.0
    )

    preferred_match_rate = (
        preferred_matched / preferred_evaluable * 100
        if preferred_evaluable > 0
        else 0.0
    )

    objective_match_rate = (
        objective_matched / objective_evaluable * 100
        if objective_evaluable > 0
        else 0.0
    )

    evaluation_coverage = (
        evaluable_count / total_count * 100
        if total_count > 0
        else 0.0
    )


    return MatchScoreResult(
        overallMatchRate=round(overall_match_rate, 1),
        requiredMatchRate=round(required_match_rate, 1),
        preferredMatchRate=round(preferred_match_rate, 1),
        objectiveMatchRate=round(objective_match_rate, 1),
        evaluationCoverage=round(evaluation_coverage, 1),
    )