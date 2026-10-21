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


def _normalized_text(value: str) -> str:
    return " ".join(value.split())


def _to_main_evidence(evidence, retrieval_score=None) -> Evidence:
    return Evidence(
        text=evidence.evidence_text,
        source=_document_type(evidence.doc_type),
        page=_page(evidence.source_location),
        paragraphIndex=_paragraph(evidence.source_location),
        evidenceId=evidence.evidence_id,
        documentId=evidence.document_id,
        sourceLocation=evidence.source_location,
        retrievalScore=(
            round(retrieval_score, 6)
            if retrieval_score is not None
            else None
        ),
    )


def link_indexed_evidence_metadata(
    store,
    evidences: List[Evidence],
    user_id: int,
) -> List[Evidence]:
    """기존 분석 근거를 같은 사용자의 색인 원문과 정확히 연결한다.

    문장과 문서 유형이 모두 같은 원본이 하나일 때만 연결한다. 같은 문장이
    여러 문서에 중복되어 출처를 확정할 수 없으면 기존 근거를 유지한다.
    이 과정은 검색이 아니므로 retrievalScore를 새로 만들지 않는다.
    """
    candidates = {}

    for indexed in store.evidences:
        if (
            indexed.user_id != user_id
            or indexed.deleted
            or not indexed.consented
        ):
            continue

        key = (
            _normalized_text(indexed.evidence_text),
            _document_type(indexed.doc_type),
        )
        candidates.setdefault(key, []).append(indexed)

    linked = []
    for evidence in evidences:
        if evidence.evidenceId:
            linked.append(evidence)
            continue

        key = (_normalized_text(evidence.text), evidence.source)
        matches = candidates.get(key, [])

        if len(matches) == 1:
            linked.append(_to_main_evidence(matches[0]))
        else:
            linked.append(evidence)

    return linked


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
        _to_main_evidence(evidence, retrieval_score=score)
        for score, evidence in hits
    ]
