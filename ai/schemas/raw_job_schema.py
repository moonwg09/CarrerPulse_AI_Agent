from datetime import date
from typing import Optional

from pydantic import BaseModel


class RawJobPosting(BaseModel):
    jobId: str
    externalJobId: Optional[str] = None
    title: str
    companyName: str

    region: Optional[str] = None
    postedDate: Optional[date] = None
    closeDate: Optional[date] = None

    url: Optional[str] = None

    content: str

    source: str
