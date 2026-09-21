"""분석 API (SRS RAG-01~03, CMP-02, GDE-08·09, AGT-01~05).

Spring이 호출하는 엔드포인트를 모아 둔다.
권한 확인과 입력값 검증은 Spring이 먼저 수행하고, 여기서는 분석만 담당한다.
"""
import os
from datetime import date

from fastapi import APIRouter, HTTPException

from agent import build_graph
from agent.tools import compare_requirement
from rag import store
from rag.judge import match_rate
from schemas.analyze_schema import (
    CompareRequest, CompareResponse, ComparisonResult, DeleteDocumentResponse,
    DroppedLine, EvidenceRef, GuideLine, GuideRequest, GuideResponse,
    IndexDocumentRequest, IndexDocumentResponse,
)

router = APIRouter(prefix="/analyze", tags=["analyze"])

# Agent 그래프는 한 번만 만들어 재사용한다.
_app, _agent_mode = build_graph()


def _to_evidence_refs(hits):
    """검색 결과를 응답 DTO로 바꾼다. 원문은 일부만 잘라 보낸다."""
    return [EvidenceRef(evidence_id=e.evidence_id, doc_type=e.doc_type, section=e.section,
                        source_location=e.source_location,
                        excerpt=e.evidence_text[:200], score=score)
            for score, e in hits]


@router.post("/documents", response_model=IndexDocumentResponse)
def index_document(req: IndexDocumentRequest):
    """서류 텍스트를 색인한다 (PAR-06·07 저장, RAG-01 준비)."""
    # 동의하지 않은 자료는 분석하지 않는다(DOC-01). 색인은 하되 검색에서 제외된다.
    evidence_ids = store.index_document(
        user_id=req.user_id, document_id=req.document_id,
        doc_type=req.doc_type, text=req.text, consented=req.consented)
    return IndexDocumentResponse(document_id=req.document_id, evidence_ids=evidence_ids,
                                 evidence_count=len(evidence_ids),
                                 embedding_mode=store.embedding_mode())


@router.delete("/documents/{document_id}", response_model=DeleteDocumentResponse)
def delete_document(document_id: int):
    """서류 삭제 요청을 즉시 반영한다 (DOC-09)."""
    removed = store.delete_document(document_id)
    return DeleteDocumentResponse(document_id=document_id, deleted_evidence_count=removed)


@router.post("/compare", response_model=CompareResponse)
def compare(req: CompareRequest):
    """공고 요구 항목과 사용자 서류를 비교해 상태를 판정한다 (CMP-02, REC-02)."""
    # 분석할 자료가 없으면 비교를 진행하지 않고 안내한다(DOC-12).
    if store.count_evidences(req.user_id) == 0:
        raise HTTPException(status_code=409,
                            detail={"code": "NO_DOCUMENT",
                                    "message": "분석에 사용할 서류가 없습니다. 서류를 먼저 등록해 주세요."})

    api_key = os.environ.get("GEMINI_API_KEY")
    results, raw = [], []
    for requirement in req.requirements:
        r = compare_requirement(store.get_store(), requirement.model_dump(),
                                req.user_id, api_key=api_key)
        raw.append(r)
        results.append(ComparisonResult(
            requirement_id=r["requirement_id"], match_status=r["match_status"],
            match_score=r["match_score"], analysis_reason=r["analysis_reason"],
            missing_part=r.get("missing_part", ""),
            evidences=_to_evidence_refs(r["hits"])))

    # 일치율은 '경험 있음'만 1개로 계산하며, 합격 가능성을 뜻하지 않는다.
    matched, total, rate, recommended = match_rate(raw)
    return CompareResponse(user_id=req.user_id, job_posting_id=req.job_posting_id,
                           results=results, matched_count=matched, total_count=total,
                           match_rate=round(rate, 3), recommended=recommended)


@router.post("/guide", response_model=GuideResponse)
def guide(req: GuideRequest):
    """작성 방향을 생성하고 근거를 검증해 돌려준다 (GDE-08·09, AGT-01~05)."""
    if store.count_evidences(req.user_id) == 0:
        raise HTTPException(status_code=409,
                            detail={"code": "NO_DOCUMENT",
                                    "message": "분석에 사용할 서류가 없습니다. 서류를 먼저 등록해 주세요."})

    # run_key 는 같은 신호가 중복으로 들어와도 한 번만 실행되게 하는 값이다(AGT-01).
    run_key = req.run_key or f"u{req.user_id}-job{req.job_posting_id}-guide-{date.today():%Y%m%d}"
    state = {"trigger_type": req.trigger_type, "run_key": run_key,
             "user_id": req.user_id, "job_posting_id": req.job_posting_id,
             "requirements": [r.model_dump() for r in req.requirements],
             "store": store.get_store(), "api_key": os.environ.get("GEMINI_API_KEY")}
    out = _app.invoke(state)

    # 사전 검증에서 막혔거나 Tool 실행이 실패하면 결과 대신 사유를 돌려준다(AGT-03·05).
    if out.get("stopped_reason"):
        return GuideResponse(user_id=req.user_id, job_posting_id=req.job_posting_id,
                             status="중단", reason=out["stopped_reason"],
                             plan=out.get("plan", []))
    if out.get("errors"):
        raise HTTPException(status_code=502,
                            detail={"code": "TOOL_FAILED", "message": "; ".join(out["errors"])})

    generated = out["step_results"].get("작성방향생성", {})
    if generated.get("status") not in ("제공", "보류"):
        return GuideResponse(user_id=req.user_id, job_posting_id=req.job_posting_id,
                             status="대상 없음",
                             reason=generated.get("reason", "안내할 항목이 없습니다."),
                             plan=out.get("plan", []))

    # 검증을 통과한 문장만 lines 에 담고, 제외된 문장은 사유와 함께 따로 돌려준다.
    import re
    lines = [GuideLine(text=t, cited=re.findall(r"EV-\d{4}", t))
             for t in generated.get("lines", [])]
    dropped = [DroppedLine(text=t, reason=why)
               for t, why in out["step_results"].get("근거검증", {}).get("dropped", [])]
    return GuideResponse(user_id=req.user_id, job_posting_id=req.job_posting_id,
                         status=generated["status"], lines=lines, dropped=dropped,
                         plan=out.get("plan", []))


@router.get("/health")
def health():
    """AI 분석 기능의 상태. Spring 헬스체크와 시연 점검에 쓴다."""
    return {"status": "ok", "embedding_mode": store.embedding_mode(),
            "agent_mode": _agent_mode,
            "gemini": "configured" if os.environ.get("GEMINI_API_KEY") else "mock"}
