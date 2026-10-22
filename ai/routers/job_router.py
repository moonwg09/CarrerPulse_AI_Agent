from fastapi import APIRouter
from pydantic import BaseModel, Field

from schemas.job_schema import JobAnalysisResult
from services.job_analyzer import analyze_job_posting


router = APIRouter(
    prefix="/api/v1/jobs",
    tags=["jobs"],
)


class JobPostingAnalyzeRequest(BaseModel):
    jobText: str = Field(min_length=1)


@router.post(
    "/analyze",
    response_model=JobAnalysisResult,
)
def analyze_job(request: JobPostingAnalyzeRequest):
    """공고 원문을 DB 저장 가능한 요구사항 구조로 분석한다."""
    return analyze_job_posting(request.jobText)
