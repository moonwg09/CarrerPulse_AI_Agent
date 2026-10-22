from typing import List, Optional

from pydantic import BaseModel, Field

from schemas.document_schema import DocumentType


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


class RagDocumentIndexResponse(BaseModel):
    documentId: int
    documentType: DocumentType
    evidenceCount: int
    extractionMethod: str
    pageCount: int
    extractedText: str


class RagDocumentDeleteResponse(BaseModel):
    documentId: int
    deletedEvidenceCount: int
