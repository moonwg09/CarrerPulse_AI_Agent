from typing import List, Optional

from pydantic import BaseModel, Field


class DocumentChunk(BaseModel):
    chunkId: str

    documentType: str

    text: str

    page: Optional[int] = None

    paragraphIndex: Optional[int] = None


class EmbeddedDocumentChunk(BaseModel):
    chunk: DocumentChunk

    embedding: List[float] = Field(
        default_factory=list
    )


class RagSearchResult(BaseModel):
    chunk: DocumentChunk

    similarity: float