from enum import Enum
from typing import List, Optional

from pydantic import BaseModel, Field

from schemas.document_schema import Evidence
from schemas.job_schema import JobEvidence, RequirementCategory


class MatchStatus(str, Enum):
    EXPERIENCED = "EXPERIENCED"
    PARTIAL = "PARTIAL"
    INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE"
    NO_EXPERIENCE = "NO_EXPERIENCE"


class RequirementMatch(BaseModel):
    requirement: str

    requirementCategory: RequirementCategory

    status: MatchStatus

    reason: str

    matchedSkills: List[str] = Field(default_factory=list)

    missingSkills: List[str] = Field(default_factory=list)

    jobEvidence: List[JobEvidence] = Field(default_factory=list)

    userEvidence: List[Evidence] = Field(default_factory=list)


class ObjectiveConditionType(str, Enum):
    EDUCATION = "EDUCATION"
    CAREER = "CAREER"
    CERTIFICATE = "CERTIFICATE"


class ObjectiveStatus(str, Enum):
    SATISFIED = "SATISFIED"
    NOT_SATISFIED = "NOT_SATISFIED"
    UNCLEAR = "UNCLEAR"

class RecommendationStatus(str, Enum):
    RECOMMENDED = "RECOMMENDED"
    GROWTH_CANDIDATE = "GROWTH_CANDIDATE"
    EXCLUDED = "EXCLUDED"


class ObjectiveConditionMatch(BaseModel):
    conditionType: ObjectiveConditionType
    requirement: str
    requirementCategory: RequirementCategory
    status: ObjectiveStatus
    reason: str
    # 객관 조건 판정에 실제로 사용한 사용자 문서 원문 근거.
    # 백엔드에서 SATISFIED/NOT_SATISFIED를 확정하려면 색인 Evidence와
    # 연결되는 근거가 반드시 필요하다.
    userEvidence: List[Evidence] = Field(default_factory=list)

class MatchScoreResult(BaseModel):
    overallMatchRate: float = 0.0
    requiredMatchRate: float = 0.0
    preferredMatchRate: float = 0.0
    objectiveMatchRate: float = 0.0
    evaluationCoverage: float = 0.0


class JobMatchResult(BaseModel):
    jobTitle: Optional[str] = None

    companyName: Optional[str] = None

    requirementMatches: List[RequirementMatch] = Field(default_factory=list)

    # 주요 업무는 사용자 수행 경험과 비교하지만 자격요건 일치율에는 반영하지 않는다.
    responsibilityMatches: List[RequirementMatch] = Field(default_factory=list)

    objectiveConditionMatches: List[ObjectiveConditionMatch] = Field(
        default_factory=list
    )

    score: Optional[MatchScoreResult] = None

    recommendationStatus: Optional[RecommendationStatus] = None

    warnings: List[str] = Field(default_factory=list)
