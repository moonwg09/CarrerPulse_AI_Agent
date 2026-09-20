"""근거 메타데이터 (SRS PAR-06·07 / ERD EVIDENCE_SOURCES, DOCUMENT_CHUNKS)."""
from dataclasses import dataclass, asdict
from typing import List, Dict, Optional


@dataclass
class Evidence:
    evidence_id: str            # EV-0001
    user_id: int
    document_id: int
    doc_type: str               # 이력서 / 자기소개서 / 포트폴리오
    section: str                # STRUCTURED_EXPERIENCES.experience_type
    source_type: str            # document / git / user_input
    source_location: str        # PAR-07: "p.1"
    evidence_text: str
    consented: bool = True      # RAG-01: 동의한 자료만 검색
    deleted: bool = False       # DOC-09: 삭제 자료 제외
    embedding_ref: Optional[str] = None

    def to_row(self) -> Dict:
        """DB 저장용 dict."""
        return asdict(self)


def build_evidences(chunks: List[Dict], user_id: int, document_id: int,
                    start: int = 1, source_type: str = "document") -> List[Evidence]:
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
