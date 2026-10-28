from fastapi import APIRouter, HTTPException

from schemas.job_collection_schema import JobCollectionRequest, JobCollectionResult
from services.job_sources.work24_crawler import (
    Work24CollectionError,
    collect_work24_jobs,
)


router = APIRouter(
    prefix="/api/v1/jobs",
    tags=["jobs"],
)


@router.post("/collect", response_model=JobCollectionResult)
def collect_jobs(request: JobCollectionRequest) -> JobCollectionResult:
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
