"""검색 결과 내부 모델"""
"""이 chunk가 왜 검색됐는지를 평가할 수 있게 하기 위함"""

from dataclasses import dataclass, field
from typing import List


@dataclass
class RetrievalResult:
    """RetrievalChunk 검색 결과

    최종 API 응답용 모델이 아니라 검색 품질 분석과
    Evidence 복원을 위한 내부 모델임
    """

    chunk_id: str
    semantic_unit_id: str

    score: float
    dense_score: float
    keyword_score: float

    category: str
    section: str
    title: str

    evidence_ids: List[str] = field(default_factory=list)