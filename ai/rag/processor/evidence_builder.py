"""SemanticUnit에서 원문 근거 Evidence 생성"""

from typing import List, Optional

from .metadata import Evidence
from .semantic_unit import SemanticUnit


EVIDENCE_TYPE_MAP = {
    "개요": "SUMMARY",

    "사용 기술": "SKILL",
    "사용기술": "SKILL",
    "기술": "SKILL",
    "기술 스택": "SKILL",
    "기술스택": "SKILL",

    "담당 역할": "ROLE",
    "담당역할": "ROLE",
    "역할": "ROLE",

    "수행 내용": "ACTIVITY",
    "수행내용": "ACTIVITY",
    "내용": "ACTIVITY",
    
    "주요 성과": "RESULT",
    "주요성과": "RESULT",
    "성과": "RESULT",

    "주요 기능": "FEATURE",
    "주요기능": "FEATURE",
    "기능": "FEATURE",

    "핵심 파이프라인": "PIPELINE",
    "핵심파이프라인": "PIPELINE",
    "파이프라인": "PIPELINE",

    "참여 인원": "TEAM",
    "참여인원": "TEAM",
    "인원": "TEAM",

    "기간": "PERIOD",

    "프로젝트 URL": "URL",
    "프로젝트URL": "URL",
}


def _normalize_key(value: str) -> str:
    return " ".join(value.strip().split())


def _evidence_type(key: str) -> Optional[str]:
    return EVIDENCE_TYPE_MAP.get(_normalize_key(key))


def _location(block, row_index: Optional[int] = None) -> str:
    loc = block.location

    if loc.page is not None:
        base = f"p.{loc.page}"
    elif loc.table is not None:
        base = f"표 {loc.table}"
    elif loc.paragraph is not None:
        base = f"문단 {loc.paragraph}"
    else:
        base = "-"

    if row_index is not None:
        return f"{base} 행 {row_index}"

    return base


def build_evidences(
    units: List[SemanticUnit],
    user_id: int,
    document_id: int,
    doc_type: str,
    start: int = 1,
) -> List[Evidence]:

    evidences: List[Evidence] = []
    seq = start

    for unit in units:

        # 개인정보는 Evidence/RAG 대상으로 만들지 않는다.
        if not unit.embedding_enabled or unit.category == "PERSONAL_INFO":
            continue

        for block in unit.blocks:

            # -------------------------
            # Table
            # -------------------------
            if block.block_type == "table" and block.rows:

                rows = block.rows

                # 첫 행은 보통 header
                for row_index, row in enumerate(rows[1:], start=2):

                    if not row:
                        continue

                    key = _normalize_key(row[0])
                    values = [
                        str(value).strip()
                        for value in row[1:]
                        if str(value).strip()
                    ]

                    value = " | ".join(values)

                    if not key or not value:
                        continue

                    evidences.append(
                        Evidence(
                            evidence_id=f"EV-{seq:04d}",
                            user_id=user_id,
                            document_id=document_id,
                            doc_type=doc_type,
                            section=unit.section,
                            category=unit.category,
                            semantic_unit_id=unit.unit_id,
                            block_id=block.block_id,
                            evidence_type=_evidence_type(key),
                            source_type="document",
                            source_location=_location(
                                block,
                                row_index,
                            ),
                            # 원문 표현을 유지
                            evidence_text=f"{key} | {value}",
                        )
                    )

                    seq += 1

            # -------------------------
            # Paragraph
            # -------------------------
            elif block.block_type == "paragraph":

                # SemanticUnit의 첫 paragraph는 제목/섹션 헤더이므로
                # Evidence가 아니라 검색 문맥(metadata)으로 사용
                if block == unit.blocks[0]:
                    continue

                text = block.text.strip()

                if not text:
                    continue

                evidences.append(
                    Evidence(
                        evidence_id=f"EV-{seq:04d}",
                        user_id=user_id,
                        document_id=document_id,
                        doc_type=doc_type,
                        section=unit.section,
                        category=unit.category,
                        semantic_unit_id=unit.unit_id,
                        block_id=block.block_id,
                        evidence_type=None,
                        source_type="document",
                        source_location=_location(block),
                        evidence_text=text,
                    )
                )

                seq += 1

    return evidences