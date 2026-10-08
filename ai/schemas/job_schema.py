from enum import Enum
from typing import List, Optional

from pydantic import BaseModel, Field


class RequirementCategory(str, Enum):
    REQUIRED = "REQUIRED"
    PREFERRED = "PREFERRED"
    UNCLEAR = "UNCLEAR"


class ConditionType(str, Enum):
    SINGLE = "SINGLE"
    AND = "AND"
    OR = "OR"
    UNCLEAR = "UNCLEAR"

class ObjectiveRequirementType(str, Enum):
    EDUCATION = "EDUCATION"
    CAREER = "CAREER"
    CERTIFICATE = "CERTIFICATE"

class JobEvidence(BaseModel):
    text: str
    source: str = "JOB_POSTING"
    section: Optional[str] = None


class JobRequirement(BaseModel):
    description: str

    category: RequirementCategory

    conditionType: ConditionType

    skills: List[str] = Field(default_factory=list)

    experienceDescription: Optional[str] = None

    evidence: List[JobEvidence] = Field(default_factory=list)

class ObjectiveRequirement(BaseModel):
    type: ObjectiveRequirementType
    # 공고 원래 조건 문장
    description: str

    # REQUIRED / PREFERRED / UNCLEAR
    category: RequirementCategory

    # SINGLE / AND / OR / UNCLEAR
    conditionType: ConditionType = ConditionType.SINGLE

    # 자격증 등의 개별 항목
    items: List[str] = Field(default_factory=list)

    evidence: List[JobEvidence] = Field(default_factory=list)


class JobAnalysisResult(BaseModel):
    jobTitle: Optional[str] = None
    companyName: Optional[str] = None

    responsibilities: List[str] = Field(default_factory=list)

    requirements: List[JobRequirement] = Field(default_factory=list)

    objectiveRequirements: List[ObjectiveRequirement] = Field(
        default_factory=list
    )

    keywords: List[str] = Field(default_factory=list)