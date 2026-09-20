"""하이브리드 검색 (SRS RAG-01).

기술명은 키워드로 정확히, 수행 내용은 임베딩으로 의미를 찾는다.
검색 전에 user_id / consented / deleted 필터를 반드시 적용한다.
"""
from typing import List, Tuple

import numpy as np

DEFAULT_TOP_K = 3
DEFAULT_ALPHA = 0.5      # 임베딩 가중치
DEFAULT_MIN_SCORE = 0.15  # 이 값이 RAG-03('근거 부족')의 경계. 정답 세트로 조정할 것


def _keyword_score(query: str, text: str) -> float:
    try:
        from rank_bm25 import BM25Okapi

        bm25 = BM25Okapi([text.lower().split()])
        raw = float(bm25.get_scores(query.lower().split())[0])
        return raw / (raw + 1.0)  # 0~1로 압축
    except Exception:
        q = set(query.lower().split())
        t = set(text.lower().split())
        return len(q & t) / (len(q) or 1)


def search(store, query: str, user_id: int, top_k: int = DEFAULT_TOP_K,
           alpha: float = DEFAULT_ALPHA,
           min_score: float = DEFAULT_MIN_SCORE) -> List[Tuple[float, object]]:
    """반환: [(score, Evidence)] — 점수 내림차순, min_score 미만 제외."""
    pool = [(i, e) for i, e in enumerate(store.evidences)
            if e.user_id == user_id and e.consented and not e.deleted]  # RAG-01
    if not pool or store.vectors is None:
        return []
    qv = store.embedder.encode([query])[0]
    scored = []
    for i, e in pool:
        vec_s = float(np.dot(qv, store.vectors[i]))
        kw_s = _keyword_score(query, e.evidence_text)
        scored.append((alpha * vec_s + (1 - alpha) * kw_s, e))
    scored.sort(key=lambda x: -x[0])
    return [(round(s, 3), e) for s, e in scored[:top_k] if s >= min_score]


def build_query(requirement: dict) -> str:
    """공고 요구 항목을 검색 질의로 바꾼다. 기술명만 넣으면 목록만 걸린다."""
    return f'{requirement["text"]} {requirement["needed_experience"]}'
