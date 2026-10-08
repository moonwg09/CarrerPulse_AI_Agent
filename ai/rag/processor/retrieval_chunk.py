"""실제로 bge-m3 모델에 넣을 객체"""
from dataclasses import dataclass, field
from typing import List, Optional


@dataclass
class RetrievalChunk:
    chunk_id: str

    user_id: int
    document_id: int
    doc_type: str

    semantic_unit_id: str

    category: str
    section: str
    title: str

    # 실제 embedding/search 대상 텍스트
    text: str

    # 검색 결과에서 원문 Evidence로 돌아가기 위한 연결
    evidence_ids: List[str] = field(default_factory=list)

    # 원문 추적용
    block_ids: List[str] = field(default_factory=list)

    # 실제 vector 저장소의 식별자
    embedding_ref: Optional[str] = None