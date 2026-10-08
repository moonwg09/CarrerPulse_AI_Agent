from datetime import date
from pydantic import BaseModel

from schemas.match_schema import JobMatchResult


class JobRecommendationItem(BaseModel):
    jobId: str
    closeDate: date
    matchResult: JobMatchResult