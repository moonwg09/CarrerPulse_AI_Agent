from typing import List

from schemas.learning_candidate_schema import (
    LearningCandidateItem,
    LearningCandidateResult,
)


def calculate_learning_priority(
    result: LearningCandidateResult,
) -> List[LearningCandidateItem]:

    learning_targets = [
        item
        for item in result.items
        if item.isLearningTarget
    ]

    learning_targets.sort(
        key=lambda item: (
            -item.requiredCount,
            -item.totalJobCount,
            -(
                item.noExperienceCount
                + item.notSatisfiedCount
            ),
            -item.partialCount,
            -item.preferredCount,
            item.name,
        )
    )

    return learning_targets