"""SemanticUnit + Evidence를 검색용 RetrievalChunk로 변환"""

from collections import defaultdict
from typing import List

from .metadata import Evidence
from .retrieval_chunk import RetrievalChunk
from .semantic_unit import SemanticUnit


def build_retrieval_chunks(
    units: List[SemanticUnit],
    evidences: List[Evidence],
    user_id: int,
    document_id: int,
    doc_type: str,
) -> List[RetrievalChunk]:

    evidence_map = defaultdict(list)

    for evidence in evidences:
        if evidence.semantic_unit_id:
            evidence_map[evidence.semantic_unit_id].append(evidence)

    chunks: List[RetrievalChunk] = []

    for unit in units:

        if not unit.embedding_enabled:
            continue

        if unit.category == "PERSONAL_INFO":
            continue

        unit_evidences = evidence_map.get(unit.unit_id, [])

        if not unit_evidences:
            continue

        lines = [
            f"[분류] {unit.category}",
            f"[항목] {unit.title}",
        ]

        if unit.project_status:
            lines.append(f"[상태] {unit.project_status}")

        lines.append("")

        for evidence in unit_evidences:
            
            # URL은 채용요건 의미 검색에는 도움이 되지 않아서, 원문 근거(Evidence)로만 보존
            if evidence.evidence_type == "URL":
                continue
            lines.append(evidence.evidence_text)

        chunks.append(
            RetrievalChunk(
                chunk_id=f"CH-{len(chunks) + 1:04d}",
                user_id=user_id,
                document_id=document_id,
                doc_type=doc_type,
                semantic_unit_id=unit.unit_id,
                category=unit.category,
                section=unit.section,
                title=unit.title,
                text="\n".join(lines),
                evidence_ids=[
                    evidence.evidence_id
                    for evidence in unit_evidences
                ],
                block_ids=unit.block_ids,
            )
        )

    return chunks