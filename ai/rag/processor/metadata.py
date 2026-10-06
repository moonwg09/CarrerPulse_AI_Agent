"""근거 메타데이터 (SRS PAR-06·07 / ERD EVIDENCE_SOURCES, DOCUMENT_CHUNKS)."""
from dataclasses import dataclass, asdict
from typing import List, Dict, Optional


@dataclass
class Evidence:
    """검색과 판정의 최소 단위. ERD의 EVIDENCE_SOURCES 컬럼에 맞춰 두었다.

    - source_location : PAR-07·CMP-05 — 없으면 근거 출처를 화면에 표시할 수 없다
    - consented       : RAG-01·DOC-01 — 동의한 자료만 검색 대상
    - deleted         : DOC-09        — 삭제 요청한 자료를 분석에서 제외
    """
    evidence_id: str            # EV-0001
    user_id: int
    document_id: int
    doc_type: str               # 이력서 / 자기소개서 / 포트폴리오
    section: str                # STRUCTURED_EXPERIENCES.experience_type 와 연결
    source_type: str            # document / git / user_input
    source_location: str        # "p.1", "문단 3"
    evidence_text: str
    consented: bool = True
    deleted: bool = False
    embedding_ref: Optional[str] = None   # DOCUMENT_CHUNKS.embedding_ref

    def to_row(self) -> Dict:
        """DB 저장용 dict로 변환한다."""
        return asdict(self)


def build_evidences(chunks: List[Dict], user_id: int, document_id: int,
                    start: int = 1, source_type: str = "document") -> List[Evidence]:
    """분할된 항목을 Evidence 목록으로 바꾼다."""
    # 위치 정보는 자료 형식에 맞춰 만든다(PDF는 페이지, DOCX는 문단).
    out = []
    for i, c in enumerate(chunks, start=start):
        loc = f'p.{c["page"]}' if c.get("page") else (
            f'문단 {c["para"]}' if c.get("para") else "-")
        out.append(Evidence(
            evidence_id=f"EV-{i:04d}",
            user_id=user_id,
            document_id=document_id,
            doc_type=c["doc_type"],
            section=c["section"],
            source_type=source_type,
            source_location=loc,
            evidence_text=c["text"],
        ))
    return out
