"""실제로 bge-m3 모델에 넣을 객체"""
from dataclasses import dataclass, field
from typing import List


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

    text: str

    evidence_ids: List[str] = field(default_factory=list)
    block_ids: List[str] = field(default_factory=list)