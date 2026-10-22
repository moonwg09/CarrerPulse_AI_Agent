from collections import defaultdict
from typing import Dict, List, Tuple

from schemas.job_schema import (
    JobAnalysisResult,
    RequirementCategory,
    ObjectiveRequirementType,
)
from schemas.frequency_schema import (
    LearningCandidateType,
    RequirementFrequencyItem,
    RequirementFrequencyResult,
)

from services.requirement_normalizer import(
    normalize_requirement_name,
)


def calculate_requirement_frequency(
    analyzed_jobs: List[Tuple[str, JobAnalysisResult]]
) -> RequirementFrequencyResult:

    # 여러 공고의 최종 빈도 누적
    frequency_map = defaultdict(
        lambda: {
            "type": None,
            "totalJobCount": 0,
            "requiredCount": 0,
            "preferredCount": 0,
            "jobIds": [],
        }
    )

    # -----------------------------------------------------
    # 공고 하나씩 처리
    # analyzed_jobs:
    # [
    #     ("job-001", JobAnalysisResult),
    #     ("job-002", JobAnalysisResult),
    # ]
    # -----------------------------------------------------
    for job_id, job_analysis in analyzed_jobs:

        # 한 공고 내부 중복 제거용
        #
        # key:
        #   (항목명, SKILL/CERTIFICATE)
        #
        # value:
        #   REQUIRED/PREFERRED
        #
        # 같은 공고 안에서 동일 항목이
        # REQUIRED와 PREFERRED에 모두 있으면
        # REQUIRED를 우선함
        job_candidates: Dict[
            tuple,
            RequirementCategory
        ] = {}

        # =================================================
        # 1. 모든 SKILL 수집
        # =================================================
        for requirement in job_analysis.requirements:

            # UNCLEAR는 빈도 통계에서 제외
            if requirement.category not in (
                RequirementCategory.REQUIRED,
                RequirementCategory.PREFERRED,
            ):
                continue

            for skill in requirement.skills:

                skill_name = normalize_requirement_name(
                    skill,
                    LearningCandidateType.SKILL.value,
                )

                if not skill_name:
                    continue

                key = (
                    skill_name,
                    LearningCandidateType.SKILL,
                )

                current_category = job_candidates.get(key)

                # 처음 나온 기술
                if current_category is None:
                    job_candidates[key] = requirement.category
                    continue

                # 같은 공고에 REQUIRED가 하나라도 있으면 REQUIRED 우선
                if (
                    requirement.category
                    == RequirementCategory.REQUIRED
                ):
                    job_candidates[key] = (
                        RequirementCategory.REQUIRED
                    )

        # =================================================
        # 2. 모든 CERTIFICATE 요구조건 수집
        # =================================================
        for objective in job_analysis.objectiveRequirements:

            if (
                objective.type
                != ObjectiveRequirementType.CERTIFICATE
            ):
                continue

            if objective.category not in (
                RequirementCategory.REQUIRED,
                RequirementCategory.PREFERRED,
            ):
                continue

            # 자격증은 description이 아니라
            # 구조화된 items를 기준으로 개별 집계
            for certificate in objective.items:

                certificate_name = normalize_requirement_name(
                    certificate,
                    LearningCandidateType.CERTIFICATE.value,
                )

                if not certificate_name:
                    continue

                key = (
                    certificate_name,
                    LearningCandidateType.CERTIFICATE,
                )

                current_category = job_candidates.get(key)

                if current_category is None:
                    job_candidates[key] = objective.category
                    continue

                # 같은 공고에서 동일 자격증이
                # REQUIRED / PREFERRED 둘 다 나오면 REQUIRED 우선
                if (
                    objective.category
                    == RequirementCategory.REQUIRED
                ):
                    job_candidates[key] = (
                        RequirementCategory.REQUIRED
                    )

        # =================================================
        # 3. 공고 내부 중복 제거가 끝난 결과를
        #    전체 빈도에 반영
        # =================================================
        for (
            candidate_name,
            candidate_type,
        ), category in job_candidates.items():

            frequency_key = (
                candidate_name,
                candidate_type,
            )

            data = frequency_map[frequency_key]

            data["type"] = candidate_type
            data["totalJobCount"] += 1
            data["jobIds"].append(job_id)

            if category == RequirementCategory.REQUIRED:
                data["requiredCount"] += 1

            elif category == RequirementCategory.PREFERRED:
                data["preferredCount"] += 1

    # =====================================================
    # 4. RequirementFrequencyResult 형태로 변환
    # =====================================================
    items = []

    for (
        candidate_name,
        candidate_type,
    ), data in frequency_map.items():

        items.append(
            RequirementFrequencyItem(
                name=candidate_name,
                type=candidate_type,
                totalJobCount=data["totalJobCount"],
                requiredCount=data["requiredCount"],
                preferredCount=data["preferredCount"],
                jobIds=data["jobIds"],
            )
        )

    # 전체 빈도 높은 순
    # → 필수 빈도 높은 순
    # → 우대 빈도 높은 순
    items.sort(
        key=lambda item: (
            -item.totalJobCount,
            -item.requiredCount,
            -item.preferredCount,
            item.name,
        )
    )

    return RequirementFrequencyResult(
        items=items
    )