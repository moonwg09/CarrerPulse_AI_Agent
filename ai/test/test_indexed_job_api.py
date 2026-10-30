from types import SimpleNamespace
from unittest.mock import Mock, patch

import numpy as np
from fastapi import HTTPException
from pydantic import ValidationError

from rag.embedding.embedding import VectorStore
from rag.processor.metadata import Evidence
from rag.processor.retrieval_chunk import RetrievalChunk
from rag.store import get_scoped_store
from routers.career_router import (
    IndexedJobAnalyzeRequest,
    analyze_indexed_job,
)
from schemas.document_schema import BasicProfile, DocumentAnalysisResult
from schemas.match_schema import JobMatchResult


class FakeEmbedder:
    def encode(self, texts):
        return np.zeros((len(texts), 2), dtype=np.float32)


def _evidence(user_id, document_id, evidence_id, consented=True):
    return Evidence(
        evidence_id=evidence_id,
        user_id=user_id,
        document_id=document_id,
        doc_type="resume",
        section="프로젝트",
        source_type="document",
        source_location="p.1",
        evidence_text=f"{document_id}번 문서 근거",
        consented=consented,
    )


def _chunk(user_id, document_id, chunk_id, evidence_id):
    return RetrievalChunk(
        chunk_id=chunk_id,
        user_id=user_id,
        document_id=document_id,
        doc_type="resume",
        semantic_unit_id="SU-1",
        category="PROJECT",
        section="프로젝트",
        title="테스트",
        text=f"{document_id}번 문서 검색 문장",
        evidence_ids=[evidence_id],
    )


def test_scoped_store_filters_user_document_and_consent():
    source = VectorStore(FakeEmbedder())
    source.add_evidences([
        _evidence(1, 10, "EV-0001"),
        _evidence(1, 11, "EV-0002"),
        _evidence(1, 12, "EV-0003", consented=False),
        _evidence(2, 10, "EV-0001"),
    ])
    source.add_chunks([
        _chunk(1, 10, "RC-0001", "EV-0001"),
        _chunk(1, 11, "RC-0002", "EV-0002"),
        _chunk(1, 12, "RC-0003", "EV-0003"),
        _chunk(2, 10, "RC-0001", "EV-0001"),
    ])

    with patch("rag.store.get_store", return_value=source):
        scoped = get_scoped_store(user_id=1, document_ids=[10, 12])

    assert [(item.user_id, item.document_id) for item in scoped.evidences] == [(1, 10)]
    assert [(item.user_id, item.document_id) for item in scoped.chunks] == [(1, 10)]
    assert scoped.vectors.shape == (1, 2)


def test_request_rejects_duplicate_document_ids():
    try:
        IndexedJobAnalyzeRequest(
            userId=1,
            jobId=2,
            jobText="백엔드 개발자",
            documentIds=[10, 10],
        )
    except ValidationError:
        return
    raise AssertionError("중복 documentIds가 검증을 통과했습니다.")


def test_request_rejects_blank_job_text():
    try:
        IndexedJobAnalyzeRequest(
            userId=1,
            jobId=2,
            jobText="   ",
            documentIds=[10],
        )
    except ValidationError:
        return
    raise AssertionError("공백 jobText가 검증을 통과했습니다.")


def test_analyze_indexed_job_uses_only_scoped_store():
    evidence = _evidence(1, 10, "EV-0001")
    scoped_store = SimpleNamespace(evidences=[evidence])
    analysis = DocumentAnalysisResult(basicProfile=BasicProfile())
    expected = JobMatchResult(jobTitle="백엔드 개발자")
    career_agent = Mock()
    career_agent.invoke.return_value = {"matchResult": expected}

    request = IndexedJobAnalyzeRequest(
        userId=1,
        jobId=2,
        jobText="백엔드 개발자 공고",
        documentIds=[10],
    )

    with (
        patch("routers.career_router.get_scoped_store", return_value=scoped_store),
        patch("routers.career_router.analyze_documents", return_value=analysis) as analyzer,
        patch("routers.career_router.build_career_agent", return_value=career_agent),
        patch(
            "routers.career_router.link_match_result_evidence_metadata",
            return_value=expected,
        ),
    ):
        response = analyze_indexed_job(request)

    assert response == expected
    analyzer.assert_called_once()
    assert analyzer.call_args.kwargs["resume_contents"] == [
        {"text": "10번 문서 근거", "page": 1}
    ]
    state = career_agent.invoke.call_args.args[0]
    assert state["userId"] == 1
    assert state["ragStore"] is scoped_store
    assert state["jobText"] == request.jobText


def test_analyze_indexed_job_rejects_missing_index():
    request = IndexedJobAnalyzeRequest(
        userId=1,
        jobId=2,
        jobText="백엔드 개발자 공고",
        documentIds=[10, 11],
    )
    scoped_store = SimpleNamespace(evidences=[_evidence(1, 10, "EV-0001")])

    with patch("routers.career_router.get_scoped_store", return_value=scoped_store):
        try:
            analyze_indexed_job(request)
        except HTTPException as exception:
            assert exception.status_code == 409
            assert exception.detail["code"] == "RAG_INDEX_NOT_READY"
            assert exception.detail["documentIds"] == [11]
            return

    raise AssertionError("색인이 없는 documentId가 검증을 통과했습니다.")


if __name__ == "__main__":
    test_scoped_store_filters_user_document_and_consent()
    test_request_rejects_duplicate_document_ids()
    test_request_rejects_blank_job_text()
    test_analyze_indexed_job_uses_only_scoped_store()
    test_analyze_indexed_job_rejects_missing_index()
    print("INDEXED JOB API TEST: PASS")
