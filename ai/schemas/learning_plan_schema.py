from datetime import date
from enum import Enum
from typing import List, Optional

from pydantic import BaseModel, Field

from schemas.frequency_schema import LearningCandidateType


class LearningActivityType(str, Enum):
    TECH_STUDY = "TECH_STUDY"
    PROJECT = "PROJECT"
    CERTIFICATE = "CERTIFICATE"
    DOCUMENT_IMPROVEMENT = "DOCUMENT_IMPROVEMENT"


class LearningPlanRequest(BaseModel):
    startDate: date
    endDate: date
    weeklyAvailableHours: float


class LearningPlanItem(BaseModel):
    priority: int

    name: str
    type: LearningCandidateType

    activityType: LearningActivityType

    taskName: str
    description: Optional[str] = None

    estimatedHours: float

    # 왜 이 항목이 계획에 포함됐는지 보여주기 위한 근거
    totalJobCount: int = 0
    requiredCount: int = 0
    preferredCount: int = 0


class WeeklyLearningPlan(BaseModel):
    weekNumber: int

    items: List[LearningPlanItem] = Field(
        default_factory=list
    )

    totalEstimatedHours: float = 0.0


class LearningPlanResult(BaseModel):
    startDate: date
    endDate: date
    weeklyAvailableHours: float

    totalWeeks: int

    weeks: List[WeeklyLearningPlan] = Field(
        default_factory=list
    )

    # 기간 안에 모든 학습 항목을 배치했는지
    allItemsScheduled: bool = True

    # 시간이 부족할 경우 사용자에게 안내
    adjustmentMessage: Optional[str] = None