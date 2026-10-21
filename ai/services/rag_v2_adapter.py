import re
from typing import List

from rag.retriever.search import search
from schemas.document_schema import DocumentType, Evidence
from schemas.job_schema import JobRequirement


DOCUMENT_TYPE_MAP = {
    "resume": DocumentType.RESUME,
    "이력서": DocumentType.RESUME,
    "self_intro": DocumentType.SELF_INTRO,
    "self-intro": DocumentType.SELF_INTRO,
    "자기소개서": DocumentType.SELF_INTRO,
    "portfolio": DocumentType.PORTFOLIO,
    "포트폴리오": DocumentType.PORTFOLIO,
}


def build_rag_v2_query(requirement: JobRequirement) -> str:
    """main JobRequirement를 RAG v2 검색 질의로 변환한다."""
    parts = [requirement.description.strip()]

    for skill in requirement.skills:
        skill = skill.strip()
        if skill and skill not in parts:
            parts.append(skill)

    if requirement.experienceDescription:
        experience = requirement.experienceDescription.strip()
        if experience and experience not in parts:
            parts.append(experience)

    return "\n".join(part for part in parts if part)


def _document_type(value: str) -> DocumentType:
    normalized = value.strip().lower()
    return DOCUMENT_TYPE_MAP.get(normalized, DocumentType.PORTFOLIO)


def _page(location: str):
    match = re.fullmatch(r"p\.(\d+)", location.strip())
    return int(match.group(1)) if match else None


def _paragraph(location: str):
    match = re.fullmatch(r"문단\s+(\d+)", location.strip())
    return int(match.group(1)) if match else None


def retrieve_requirement_evidence_v2(
    store,
    requirement: JobRequirement,
    user_id: int,
    top_k: int = 3,
) -> List[Evidence]:
    """RAG v2 검색 결과를 기존 main Evidence 응답으로 변환한다."""
    query = build_rag_v2_query(requirement)
    if not query:
        return []

    hits = search(
        store=store,
        query=query,
        user_id=user_id,
        top_k=top_k,
    )

    return [
        Evidence(
            text=evidence.evidence_text,
            source=_document_type(evidence.doc_type),
            page=_page(evidence.source_location),
            paragraphIndex=_paragraph(evidence.source_location),
            evidenceId=evidence.evidence_id,
            documentId=evidence.document_id,
            sourceLocation=evidence.source_location,
            retrievalScore=round(score, 6),
        )
        for score, evidence in hits
    ]
