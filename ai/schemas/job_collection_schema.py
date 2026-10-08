from typing import List

from pydantic import BaseModel, Field

from schemas.raw_job_schema import RawJobPosting


class JobCollectionRequest(BaseModel):
    jobKeyword: str
    region: str

    limit: int = Field(
        default=10,
        ge=1,
        le=10,
    )


class JobCollectionResult(BaseModel):
    jobKeyword: str
    region: str

    requestedLimit: int
    collectedCount: int

    jobs: List[RawJobPosting]