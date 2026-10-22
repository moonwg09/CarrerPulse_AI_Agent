from typing import TypedDict, Optional, List

from schemas.document_schema import (
    DocumentAnalysisResult,
)

from schemas.job_schema import (
    JobAnalysisResult,
)

from schemas.match_schema import (
    JobMatchResult,
)

from schemas.rag_schema import (
    EmbeddedDocumentChunk,
)


class CareerAgentState(TypedDict):

    # 입력
    jobText: str

    # 이미 분석해 둔 사용자 정보
    userAnalysis: DocumentAnalysisResult

    # 이미 생성해 둔 사용자 문서 embedding
    embeddedChunks: List[
        EmbeddedDocumentChunk
    ]

    # Agent 실행 중 생성되는 값
    jobAnalysis: Optional[
        JobAnalysisResult
    ]

    matchResult: Optional[
        JobMatchResult
    ]