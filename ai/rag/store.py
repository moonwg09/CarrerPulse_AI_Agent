"""서류 색인 저장소 (SRS RAG-01, DOC-09, PAR-06·07).

Spring이 추출해 보낸 텍스트를 항목으로 나누고 근거로 만들어 보관한다.
현재는 프로세스 메모리에 둔다. 운영에서는 Chroma나 pgvector로 교체하고,
ERD의 DOCUMENT_CHUNKS.embedding_ref 에 벡터 ID를 저장한다.
"""
from threading import Lock
from typing import Dict, List, Optional

from .embedding import Embedder, VectorStore
from .processor import split_sections, build_evidences

# 임베딩 모델 적재는 오래 걸리므로 프로세스당 한 번만 만든다.
_embedder: Optional[Embedder] = None
_store: Optional[VectorStore] = None
_lock = Lock()

# 사용자별 근거 번호가 겹치지 않도록 문서 단위로 시작 번호를 관리한다.
_next_index: Dict[int, int] = {}


def get_store() -> VectorStore:
    """전역 벡터 저장소를 돌려준다(없으면 생성)."""
    global _embedder, _store
    with _lock:
        if _store is None:
            _embedder = Embedder()
            _store = VectorStore(_embedder)
    return _store


def embedding_mode() -> str:
    """현재 사용 중인 임베딩 방식(bge-m3 / fallback)."""
    get_store()
    return _embedder.mode if _embedder else "unknown"


def index_document(user_id: int, document_id: int, doc_type: str, text: str,
                   consented: bool = True) -> List[str]:
    """서류 텍스트를 항목으로 나눠 색인한다. 반환: 생성된 근거 ID 목록."""
    # 같은 document_id 를 다시 보내면 이전 근거를 지우고 새로 넣는다(PAR-08 재분석).
    store = get_store()
    store.delete_by_document(document_id)

    pages = [{"page": 1, "text": text}]
    chunks = split_sections(pages, doc_type)
    start = _next_index.get(user_id, 1)
    evidences = build_evidences(chunks, user_id=user_id, document_id=document_id, start=start)
    for e in evidences:
        e.consented = consented
    _next_index[user_id] = start + len(evidences)

    store.add(evidences)
    return [e.evidence_id for e in evidences]


def delete_document(document_id: int) -> int:
    """서류 삭제 요청 처리 (DOC-09). 반환: 삭제된 근거 수."""
    return get_store().delete_by_document(document_id)


def count_evidences(user_id: int) -> int:
    """해당 사용자의 검색 가능한 근거 수. 자료 없음 안내(DOC-12)에 사용한다."""
    return sum(1 for e in get_store().evidences
               if e.user_id == user_id and e.consented and not e.deleted)
