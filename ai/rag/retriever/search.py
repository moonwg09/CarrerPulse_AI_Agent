"""하이브리드 검색 (SRS RAG-01)."""
from typing import List, Tuple

from .models import RetrievalResult

import numpy as np

# 아래 세 값은 SRS 미정 항목이다. 실제 모델을 붙인 뒤 정답 세트로 조정해야 한다.
DEFAULT_TOP_K = 3
DEFAULT_ALPHA = 0.5        # 임베딩 가중치(나머지는 키워드 가중치) # TODO: 최적값 탐색(현재는 0.5가 baseline)
DEFAULT_MIN_SCORE = 0.15   # 이 값이 RAG-03('근거 부족')의 경계선


def search(
    store,
    query: str,
    user_id: int,
    top_k: int = DEFAULT_TOP_K,
    alpha: float = DEFAULT_ALPHA,
    min_score: float = DEFAULT_MIN_SCORE,
) -> List[Tuple[float, object]]:
    """RetrievalChunk를 검색하고 원본 Evidence를 반환한다.

    외부 호환을 위해 반환 형식은 기존과 동일하게
    [(score, Evidence), ...] 형태를 유지한다.
    """

    results = _hybrid_search_chunks(
        store=store,
        query=query,
        user_id=user_id,
        top_k=top_k,
        alpha=alpha,
    )

    # 낮은 검색 점수 제거
    results = [
        result
        for result in results
        if result.score >= min_score
    ]

    return _resolve_retrieval_results(
        store=store,
        results=results,
        user_id=user_id,
    )


# chunk 기반 dense 검색 구현
def _dense_search_chunks(
    store,
    query: str,
    user_id: int,
    top_k: int = DEFAULT_TOP_K,
):
    """RetrievalChunk를 대상으로 dense 검색한다.

    현재 단계에서는 bge-m3 cosine similarity만 사용한다.
    Embedder가 normalize_embeddings=True이므로 dot product를
    cosine similarity로 사용할 수 있다.
    """

    if store.vectors is None:
        return []

    # query embedding
    query_vector = store.embedder.encode([query])[0]

    scored = []

    # vector의 각 행은 vector_chunk_ids의 같은 위치 chunk와 대응한다.
    for vector_index, chunk_id in enumerate(store.vector_chunk_ids):
        chunk = store.get_chunk(chunk_id)

        if chunk is None:
            continue

        # 다른 사용자의 Chunk 제외
        if chunk.user_id != user_id:
            continue

        score = float(
            np.dot(
                query_vector,
                store.vectors[vector_index],
            )
        )

        scored.append((score, chunk))

    scored.sort(
        key=lambda item: item[0],
        reverse=True,
    )

    return scored[:top_k]


def _tokenize(text: str) -> List[str]:
    """BM25용 단순 토큰화.

    현재는 baseline으로 공백 기준 토큰화를 사용한다.
    추후 한국어 형태소 분석기 도입 여부를 평가 데이터로 결정한다.
    """
    return text.lower().split()


def _bm25_search_chunks(
    store,
    query: str,
    user_id: int,
    top_k: int = DEFAULT_TOP_K,
):
    """RetrievalChunk corpus 전체를 대상으로 BM25 검색한다."""

    # 사용자에게 속한 검색 가능 Chunk만 corpus로 구성
    chunks = [
        chunk
        for chunk in store.chunks
        if chunk.user_id == user_id
    ]

    if not chunks:
        return []

    tokenized_corpus = [
        _tokenize(chunk.text)
        for chunk in chunks
    ]

    query_tokens = _tokenize(query)

    if not query_tokens:
        return []

    try:
        from rank_bm25 import BM25Okapi

        bm25 = BM25Okapi(tokenized_corpus)
        raw_scores = bm25.get_scores(query_tokens)

    except Exception:
        # rank_bm25 사용이 불가능한 경우 단순 token overlap fallback
        query_set = set(query_tokens)

        raw_scores = []

        for tokens in tokenized_corpus:
            token_set = set(tokens)

            score = (
                len(query_set & token_set) / len(query_set)
                if query_set
                else 0.0
            )

            raw_scores.append(score)

    scored = [
        (float(score), chunk)
        for score, chunk in zip(raw_scores, chunks)
    ]

    scored.sort(
        key=lambda item: item[0],
        reverse=True,
    )

    return scored[:top_k]


def build_query(requirement: dict) -> str:
    """공고 요구 항목을 검색 질의문으로 바꾼다."""
    # 기술명만 넣으면 기술을 나열한 목록만 걸린다. 필요한 수행 경험 설명(REQ-05)을
    # 함께 붙여야 '무엇을 해 봤는지'가 적힌 항목이 걸린다.
    return f'{requirement["text"]} {requirement["needed_experience"]}'


def _normalize_scores(score_by_chunk_id: dict) -> dict:
    """검색 점수를 0~1 범위로 min-max 정규화한다."""
    if not score_by_chunk_id:
        return {}

    values = list(score_by_chunk_id.values())
    min_score = min(values)
    max_score = max(values)

    if max_score == min_score:
        return {
            chunk_id: 1.0
            for chunk_id in score_by_chunk_id
        }

    return {
        chunk_id: (score - min_score) / (max_score - min_score)
        for chunk_id, score in score_by_chunk_id.items()
    }


def _hybrid_search_chunks(
    store,
    query: str,
    user_id: int,
    top_k: int = DEFAULT_TOP_K,
    alpha: float = DEFAULT_ALPHA,
) -> List[RetrievalResult]:
    """Dense + BM25 hybrid RetrievalChunk 검색."""

    chunks = [
        chunk
        for chunk in store.chunks
        if chunk.user_id == user_id
    ]

    if not chunks or store.vectors is None:
        return []

    # -------------------------
    # 1. Dense score
    # -------------------------

    query_vector = store.embedder.encode([query])[0]

    dense_scores = {}

    vector_index_by_chunk_id = {
        chunk_id: index
        for index, chunk_id in enumerate(store.vector_chunk_ids)
    }

    for chunk in chunks:
        vector_index = vector_index_by_chunk_id.get(chunk.chunk_id)

        if vector_index is None:
            continue

        dense_scores[chunk.chunk_id] = float(
            np.dot(
                query_vector,
                store.vectors[vector_index],
            )
        )

    # -------------------------
    # 2. BM25 score
    # -------------------------

    tokenized_corpus = [
        _tokenize(chunk.text)
        for chunk in chunks
    ]

    query_tokens = _tokenize(query)

    bm25_scores = {}

    if query_tokens:
        try:
            from rank_bm25 import BM25Okapi

            bm25 = BM25Okapi(tokenized_corpus)
            raw_scores = bm25.get_scores(query_tokens)

        except ImportError:
            query_set = set(query_tokens)

            raw_scores = [
                (
                    len(query_set & set(tokens)) / len(query_set)
                    if query_set
                    else 0.0
                )
                for tokens in tokenized_corpus
            ]

        bm25_scores = {
            chunk.chunk_id: float(score)
            for chunk, score in zip(chunks, raw_scores)
        }

    # -------------------------
    # 3. normalization
    # -------------------------

    normalized_dense = _normalize_scores(dense_scores)
    normalized_bm25 = _normalize_scores(bm25_scores)

    # -------------------------
    # 4. hybrid score
    # -------------------------

    results = []

    for chunk in chunks:
        dense_score = normalized_dense.get(chunk.chunk_id, 0.0)
        keyword_score = normalized_bm25.get(chunk.chunk_id, 0.0)

        hybrid_score = (
            alpha * dense_score
            + (1.0 - alpha) * keyword_score
        )

        results.append(
            RetrievalResult(
                chunk_id=chunk.chunk_id,
                semantic_unit_id=chunk.semantic_unit_id,
                score=round(hybrid_score, 6),
                dense_score=round(dense_score, 6),
                keyword_score=round(keyword_score, 6),
                category=chunk.category,
                section=chunk.section,
                title=chunk.title,
                evidence_ids=chunk.evidence_ids.copy(),
            )
        )

    results.sort(
        key=lambda result: result.score,
        reverse=True,
    )

    return results[:top_k]


def _resolve_retrieval_results(
    store,
    results: List[RetrievalResult],
    user_id: int,
) -> List[Tuple[float, object]]:
    """RetrievalResult가 참조하는 원본 Evidence를 복원한다.

    검색 결과의 Chunk 순위를 유지하면서,
    동의했고 삭제되지 않은 현재 사용자의 Evidence만 반환한다.
    """

    resolved = []
    seen_evidence_ids = set()

    for result in results:
        evidences = store.resolve_evidences(result.evidence_ids)

        for evidence in evidences:
            # 다른 사용자의 Evidence 제외
            if evidence.user_id != user_id:
                continue

            # 검색 동의하지 않은 Evidence 제외
            if not evidence.consented:
                continue

            # 삭제된 Evidence 제외
            if evidence.deleted:
                continue

            if evidence.evidence_type == "URL":
                continue

            # 같은 Evidence가 여러 Chunk에서 연결될 경우 중복 방지
            if evidence.evidence_id in seen_evidence_ids:
                continue

            seen_evidence_ids.add(evidence.evidence_id)

            resolved.append(
                (
                    result.score,
                    evidence,
                )
            )

    return resolved