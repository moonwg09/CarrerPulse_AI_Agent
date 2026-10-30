from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from schemas.job_collection_schema import JobCollectionRequest, JobCollectionResult
from schemas.job_schema import JobAnalysisResult
from services.job_analyzer import analyze_job_posting
from services.job_sources.work24_crawler import (
    Work24CollectionError,
    collect_work24_jobs,
)


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


@router.post("/collect", response_model=JobCollectionResult)
def collect_jobs(request: JobCollectionRequest) -> JobCollectionResult:
    """고용24에서 직무와 지역 조건에 맞는 공고를 수집한다."""
    try:
        return collect_work24_jobs(
            job_keyword=request.jobKeyword,
            region=request.region,
            limit=request.limit,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Work24CollectionError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
