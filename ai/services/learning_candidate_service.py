from typing import Dict, List

from schemas.frequency_schema import (
    LearningCandidateType,
    RequirementFrequencyResult,
)
from schemas.learning_candidate_schema import (
    LearningCandidateItem,
    LearningCandidateResult,
)
from schemas.match_schema import (
    MatchStatus,
    ObjectiveConditionType,
    ObjectiveStatus,
)
from schemas.recommendation_schema import JobRecommendationItem

from services.requirement_normalizer import (
    normalize_requirement_name,
)


def build_learning_candidates(
    frequency_result: RequirementFrequencyResult,
    jobs: List[JobRecommendationItem],
) -> LearningCandidateResult:

    candidate_map: Dict[tuple, LearningCandidateItem] = {}

    # 1. 시장 요구 빈도 결과를 기본값으로 복사
    for item in frequency_result.items:

        key = (
            item.name,
            item.type,
        )

        candidate_map[key] = LearningCandidateItem(
            name=item.name,
            type=item.type,
            totalJobCount=item.totalJobCount,
            requiredCount=item.requiredCount,
            preferredCount=item.preferredCount,
            jobIds=item.jobIds,
        )

    # 2. 공고별 사용자 상태 집계
    for job in jobs:

        # 같은 공고에서 같은 기술이 여러 번 나올 수 있으므로
        # 공고당 한 번만 집계하기 위한 임시 map
        skill_status_map = {}

        for requirement in job.matchResult.requirementMatches:

            # 경험 있음
            if requirement.status == MatchStatus.EXPERIENCED:

                for skill in requirement.matchedSkills:

                    skill_name = normalize_requirement_name(
                        skill,
                        LearningCandidateType.SKILL.value,
                    )

                    update_skill_status(
                        skill_status_map,
                        skill_name,
                        MatchStatus.EXPERIENCED,
                    )

            # 일부 경험
            elif requirement.status == MatchStatus.PARTIAL:

                for skill in requirement.missingSkills:

                    skill_name = normalize_requirement_name(
                        skill,
                        LearningCandidateType.SKILL.value,
                    )

                    update_skill_status(
                        skill_status_map,
                        skill_name,
                        MatchStatus.PARTIAL,
                    )

            # 명시적으로 경험 없음
            elif requirement.status == MatchStatus.NO_EXPERIENCE:

                for skill in requirement.missingSkills:

                    skill_name = normalize_requirement_name(
                        skill,
                        LearningCandidateType.SKILL.value,
                    )

                    update_skill_status(
                        skill_status_map,
                        skill_name,
                        MatchStatus.NO_EXPERIENCE,
                    )

            # 근거 부족
            elif (
                requirement.status
                == MatchStatus.INSUFFICIENT_EVIDENCE
            ):

                for skill in requirement.missingSkills:

                    skill_name = normalize_requirement_name(
                        skill,
                        LearningCandidateType.SKILL.value,
                    )

                    update_skill_status(
                        skill_status_map,
                        skill_name,
                        MatchStatus.INSUFFICIENT_EVIDENCE,
                    )

        # 3. SKILL 상태 반영
        for skill_name, status in skill_status_map.items():

            key = (
                skill_name,
                LearningCandidateType.SKILL,
            )

            candidate = candidate_map.get(key)

            if candidate is None:
                continue

            # 시장 빈도 계산에 포함된 공고에서 나온 상태만 집계
            if job.jobId not in candidate.jobIds:
                continue

            if status == MatchStatus.EXPERIENCED:
                candidate.experiencedCount += 1

            elif status == MatchStatus.PARTIAL:
                candidate.partialCount += 1

            elif status == MatchStatus.NO_EXPERIENCE:
                candidate.noExperienceCount += 1

            elif status == MatchStatus.INSUFFICIENT_EVIDENCE:
                candidate.insufficientEvidenceCount += 1

        # 4. CERTIFICATE 상태 집계
        certificate_status_map = {}

        for objective in (
            job.matchResult.objectiveConditionMatches
        ):

            if (
                objective.conditionType
                != ObjectiveConditionType.CERTIFICATE
            ):
                continue

            certificate_name = normalize_requirement_name(
                objective.requirement,
                LearningCandidateType.CERTIFICATE.value,
            )

            certificate_status_map[
                certificate_name
            ] = objective.status

        for certificate_name, status in (
            certificate_status_map.items()
        ):

            key = (
                certificate_name,
                LearningCandidateType.CERTIFICATE,
            )

            candidate = candidate_map.get(key)

            if candidate is None:
                continue

            if job.jobId not in candidate.jobIds:
                continue

            if status == ObjectiveStatus.SATISFIED:
                candidate.satisfiedCount += 1

            elif status == ObjectiveStatus.NOT_SATISFIED:
                candidate.notSatisfiedCount += 1

            elif status == ObjectiveStatus.UNCLEAR:
                candidate.unclearCount += 1

    # 5. 실제 학습 대상 여부 결정
    for candidate in candidate_map.values():

        if candidate.type == LearningCandidateType.SKILL:

            candidate.isLearningTarget = (
                candidate.partialCount > 0
                or candidate.noExperienceCount > 0
            )

        elif (
            candidate.type
            == LearningCandidateType.CERTIFICATE
        ):

            candidate.isLearningTarget = (
                candidate.notSatisfiedCount > 0
            )

    return LearningCandidateResult(
        items=list(candidate_map.values())
    )


def update_skill_status(
    status_map: dict,
    skill_name: str,
    new_status: MatchStatus,
):

    if not skill_name:
        return

    current_status = status_map.get(skill_name)

    if current_status is None:
        status_map[skill_name] = new_status
        return

    # 같은 공고에서 같은 기술이 여러 상태로 나오면
    # 더 부족한 상태를 우선
    priority = {
        MatchStatus.INSUFFICIENT_EVIDENCE: 0,
        MatchStatus.EXPERIENCED: 1,
        MatchStatus.PARTIAL: 2,
        MatchStatus.NO_EXPERIENCE: 3,
    }

    if priority[new_status] > priority[current_status]:
        status_map[skill_name] = new_status