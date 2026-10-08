from typing import List
from pydantic import BaseModel, Field

from schemas.frequency_schema import LearningCandidateType


class LearningCandidateItem(BaseModel):
    name: str
    type: LearningCandidateType

    # 시장 요구 빈도
    totalJobCount: int = 0
    requiredCount: int = 0
    preferredCount: int = 0
    jobIds: List[str] = Field(default_factory=list)

    # SKILL 사용자 상태
    experiencedCount: int = 0
    partialCount: int = 0
    noExperienceCount: int = 0
    insufficientEvidenceCount: int = 0

    # CERTIFICATE 사용자 상태
    satisfiedCount: int = 0
    notSatisfiedCount: int = 0
    unclearCount: int = 0

    # 실제 학습 대상 여부
    isLearningTarget: bool = False


class LearningCandidateResult(BaseModel):
    items: List[LearningCandidateItem] = Field(
        default_factory=list
    )