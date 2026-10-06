"""하이브리드 검색 (SRS RAG-01)."""
from typing import List, Tuple

import numpy as np

# 아래 세 값은 SRS 미정 항목이다. 실제 모델을 붙인 뒤 정답 세트로 조정해야 한다.
DEFAULT_TOP_K = 3
DEFAULT_ALPHA = 0.5        # 임베딩 가중치(나머지는 키워드 가중치)
DEFAULT_MIN_SCORE = 0.15   # 이 값이 RAG-03('근거 부족')의 경계선


def _keyword_score(query: str, text: str) -> float:
    """키워드 점수를 0~1 범위로 구한다."""
    # rank_bm25가 있으면 BM25 점수를 쓰고, 없으면 단어 겹침 비율로 대체한다.
    try:
        from rank_bm25 import BM25Okapi

        raw = float(BM25Okapi([text.lower().split()]).get_scores(query.lower().split())[0])
        return raw / (raw + 1.0)
    except Exception:
        q, t = set(query.lower().split()), set(text.lower().split())
        return len(q & t) / (len(q) or 1)


def search(store, query: str, user_id: int, top_k: int = DEFAULT_TOP_K,
           alpha: float = DEFAULT_ALPHA,
           min_score: float = DEFAULT_MIN_SCORE) -> List[Tuple[float, object]]:
    """근거를 검색한다. 반환: [(점수, Evidence)] — 점수 내림차순."""
    # RAG-01·DOC-09: 본인 자료, 동의한 자료, 삭제되지 않은 자료만 검색 대상에 넣는다.
    # 이 필터를 검색식보다 먼저 적용해야 다른 사용자의 근거가 섞이지 않는다.
    pool = [(i, e) for i, e in enumerate(store.evidences)
            if e.user_id == user_id and e.consented and not e.deleted]
    if not pool or store.vectors is None:
        return []

    # 기술명은 키워드로, 수행 내용은 임베딩으로 찾는다. 두 점수를 alpha로 섞는다.
    qv = store.embedder.encode([query])[0]
    scored = []
    for i, e in pool:
        vec_s = float(np.dot(qv, store.vectors[i]))
        kw_s = _keyword_score(query, e.evidence_text)
        scored.append((alpha * vec_s + (1 - alpha) * kw_s, e))

    # 점수가 낮은 결과는 버린다. 남은 결과가 없으면 호출한 쪽에서 RAG-03 경로로 간다.
    scored.sort(key=lambda x: -x[0])
    return [(round(s, 3), e) for s, e in scored[:top_k] if s >= min_score]


def build_query(requirement: dict) -> str:
    """공고 요구 항목을 검색 질의문으로 바꾼다."""
    # 기술명만 넣으면 기술을 나열한 목록만 걸린다. 필요한 수행 경험 설명(REQ-05)을
    # 함께 붙여야 '무엇을 해 봤는지'가 적힌 항목이 걸린다.
    return f'{requirement["text"]} {requirement["needed_experience"]}'
