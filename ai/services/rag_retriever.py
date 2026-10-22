from math import sqrt
from typing import List, Optional

from schemas.job_schema import JobRequirement
from schemas.rag_schema import (
    EmbeddedDocumentChunk,
    RagSearchResult,
)

from services.rag_embedding_service import (
    embed_query,
)


# -------------------------------------------------
# 1. 공고 요구사항 → RAG 검색 Query 생성
# -------------------------------------------------
def build_requirement_query(
    requirement: JobRequirement,
) -> str:

    parts: List[str] = []

    # 요구사항 원문/설명
    if requirement.description:
        parts.append(
            requirement.description.strip()
        )

    # 기술명
    for skill in requirement.skills:

        skill = skill.strip()

        if (
            skill
            and skill not in parts
        ):
            parts.append(skill)

    # 수행 경험 설명
    if requirement.experienceDescription:

        experience_description = (
            requirement
            .experienceDescription
            .strip()
        )

        if (
            experience_description
            and experience_description
            not in parts
        ):
            parts.append(
                experience_description
            )

    return "\n".join(parts)


# -------------------------------------------------
# 2. Cosine Similarity 계산
# -------------------------------------------------
def cosine_similarity(
    vector_a: List[float],
    vector_b: List[float],
) -> float:

    if not vector_a or not vector_b:
        return 0.0

    if len(vector_a) != len(vector_b):
        raise ValueError(
            "embedding vector의 차원이 "
            "일치하지 않습니다."
        )

    dot_product = sum(
        a * b
        for a, b in zip(
            vector_a,
            vector_b,
        )
    )

    norm_a = sqrt(
        sum(
            a * a
            for a in vector_a
        )
    )

    norm_b = sqrt(
        sum(
            b * b
            for b in vector_b
        )
    )

    if norm_a == 0 or norm_b == 0:
        return 0.0

    return (
        dot_product
        / (norm_a * norm_b)
    )


# -------------------------------------------------
# 3. 관련 Chunk 검색
# -------------------------------------------------
def search_relevant_chunks(
    query: str,
    embedded_chunks: List[
        EmbeddedDocumentChunk
    ],
    top_k: int = 3,
    min_similarity: Optional[
        float
    ] = None,
) -> List[RagSearchResult]:

    if not query.strip():
        return []

    if not embedded_chunks:
        return []

    if top_k <= 0:
        raise ValueError(
            "top_k는 1 이상이어야 합니다."
        )

    # Query embedding
    query_embedding = embed_query(
        query
    )

    results: List[
        RagSearchResult
    ] = []

    # 각 문서 chunk와 유사도 계산
    for embedded_chunk in (
        embedded_chunks
    ):

        similarity = (
            cosine_similarity(
                query_embedding,
                embedded_chunk.embedding,
            )
        )

        # threshold가 설정되어 있을 때만 적용
        if (
            min_similarity is not None
            and similarity
            < min_similarity
        ):
            continue

        results.append(
            RagSearchResult(
                chunk=(
                    embedded_chunk.chunk
                ),
                similarity=round(
                    similarity,
                    4,
                ),
            )
        )

    # 유사도 높은 순
    results.sort(
        key=lambda result: (
            result.similarity
        ),
        reverse=True,
    )

    return results[:top_k]


# -------------------------------------------------
# 4. Requirement 하나를 바로 검색
# -------------------------------------------------
def retrieve_requirement_evidence(
    requirement: JobRequirement,
    embedded_chunks: List[
        EmbeddedDocumentChunk
    ],
    top_k: int = 3,
) -> List[RagSearchResult]:

    query = (
        build_requirement_query(
            requirement
        )
    )

    return search_relevant_chunks(
        query=query,
        embedded_chunks=embedded_chunks,
        top_k=top_k,
    )