from enum import Enum
from typing import List
from pydantic import BaseModel, Field


class LearningCandidateType(str, Enum):
    SKILL = "SKILL"
    CERTIFICATE = "CERTIFICATE"


class RequirementFrequencyItem(BaseModel):
    name: str

    type: LearningCandidateType

    # 이 항목이 등장한 서로 다른 공고 수
    totalJobCount: int = 0

    # 필수 조건으로 등장한 공고 수
    requiredCount: int = 0

    # 우대 조건으로 등장한 공고 수
    preferredCount: int = 0

    # 어떤 공고들에서 등장했는지
    jobIds: List[str] = Field(default_factory=list)


class RequirementFrequencyResult(BaseModel):
    items: List[RequirementFrequencyItem] = Field(default_factory=list)