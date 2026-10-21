from rag.embedding.embedding import VectorStore
from rag.processor.metadata import Evidence as RagEvidence
from rag.processor.retrieval_chunk import RetrievalChunk
from schemas.job_schema import ConditionType, JobRequirement, RequirementCategory
from services.rag_v2_adapter import build_rag_v2_query, retrieve_requirement_evidence_v2
from test.test_user_isolation import DeterministicEmbedder


def test_main_requirement_and_evidence_are_converted():
    vector_store = VectorStore(DeterministicEmbedder())
    rag_evidence = RagEvidence(
        evidence_id="EV-0001",
        user_id=7,
        document_id=31,
        doc_type="resume",
        section="project",
        source_type="document",
        source_location="p.2",
        evidence_text="Python FastAPI REST API를 직접 설계하고 구현했습니다.",
    )
    chunk = RetrievalChunk(
        chunk_id="CH-0001",
        user_id=7,
        document_id=31,
        doc_type="resume",
        semantic_unit_id="UNIT-1",
        category="PROJECT",
        section="project",
        title="API 프로젝트",
        text=rag_evidence.evidence_text,
        evidence_ids=[rag_evidence.evidence_id],
    )
    vector_store.add_document([rag_evidence], [chunk])

    requirement = JobRequirement(
        description="REST API 설계 및 구현",
        category=RequirementCategory.REQUIRED,
        conditionType=ConditionType.SINGLE,
        skills=["Python", "FastAPI"],
        experienceDescription="API를 직접 구현한 경험",
    )

    query = build_rag_v2_query(requirement)
    assert "REST API 설계 및 구현" in query
    assert "FastAPI" in query
    assert "API를 직접 구현한 경험" in query

    results = retrieve_requirement_evidence_v2(
        store=vector_store,
        requirement=requirement,
        user_id=7,
        top_k=3,
    )

    assert len(results) == 1
    assert results[0].evidenceId == "EV-0001"
    assert results[0].documentId == 31
    assert results[0].page == 2
    assert results[0].source.value == "RESUME"


if __name__ == "__main__":
    test_main_requirement_and_evidence_are_converted()
    print("RAG V2 ADAPTER TEST: PASS")
