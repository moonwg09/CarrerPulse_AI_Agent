from typing import List

from google.genai import types

from schemas.rag_schema import (
    DocumentChunk,
    EmbeddedDocumentChunk,
)
from services.gemini_client import get_gemini_client

EMBEDDING_MODEL = "gemini-embedding-001"
EMBEDDING_DIMENSION = 768


def embed_document_chunks(
    chunks: List[DocumentChunk],
) -> List[EmbeddedDocumentChunk]:
    if not chunks:
        return []

    client = get_gemini_client()

    texts = [
        chunk.text
        for chunk in chunks
    ]

    response = client.models.embed_content(
        model=EMBEDDING_MODEL,
        contents=texts,
        config=types.EmbedContentConfig(
            task_type="RETRIEVAL_DOCUMENT",
            output_dimensionality=(
                EMBEDDING_DIMENSION
            ),
        ),
    )

    if not response.embeddings:
        raise ValueError(
            "문서 embedding 생성에 실패했습니다."
        )

    if len(response.embeddings) != len(chunks):
        raise ValueError(
            "chunk 개수와 embedding 개수가 "
            "일치하지 않습니다."
        )

    embedded_chunks = []

    for chunk, embedding in zip(
        chunks,
        response.embeddings,
    ):
        embedded_chunks.append(
            EmbeddedDocumentChunk(
                chunk=chunk,
                embedding=embedding.values,
            )
        )

    return embedded_chunks


def embed_query(
    query: str,
) -> List[float]:
    query = query.strip()

    if not query:
        raise ValueError(
            "RAG 검색 query가 비어 있습니다."
        )

    client = get_gemini_client()

    response = client.models.embed_content(
        model=EMBEDDING_MODEL,
        contents=query,
        config=types.EmbedContentConfig(
            task_type="RETRIEVAL_QUERY",
            output_dimensionality=(
                EMBEDDING_DIMENSION
            ),
        ),
    )

    if not response.embeddings:
        raise ValueError(
            "query embedding 생성에 실패했습니다."
        )

    return response.embeddings[0].values
