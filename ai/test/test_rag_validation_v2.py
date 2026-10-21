from unittest.mock import patch

from rag.embedding.embedding import VectorStore
from rag.processor.metadata import Evidence as RagEvidence
from rag.processor.retrieval_chunk import RetrievalChunk
from schemas.document_schema import BasicProfile, DocumentAnalysisResult
from schemas.job_schema import (
    ConditionType,
    JobAnalysisResult,
    JobRequirement,
    RequirementCategory,
)
from schemas.match_schema import JobMatchResult, MatchStatus, RequirementMatch
from services.rag_validation_service import validate_match_result_with_rag
from test.test_user_isolation import DeterministicEmbedder


def test_agent_validation_uses_rag_v2_evidence():
    vector_store = VectorStore(DeterministicEmbedder())
    evidence = RagEvidence(
        evidence_id="EV-0001",
        user_id=9,
        document_id=41,
        doc_type="resume",
        section="project",
        source_type="document",
        source_location="p.1",
        evidence_text="FastAPI REST API를 직접 설계하고 구현했습니다.",
    )
    chunk = RetrievalChunk(
        chunk_id="CH-0001",
        user_id=9,
        document_id=41,
        doc_type="resume",
        semantic_unit_id="UNIT-1",
        category="PROJECT",
        section="project",
        title="백엔드 프로젝트",
        text=evidence.evidence_text,
        evidence_ids=[evidence.evidence_id],
    )
    vector_store.add_document([evidence], [chunk])

    requirement = JobRequirement(
        description="FastAPI REST API 설계 경험",
        category=RequirementCategory.REQUIRED,
        conditionType=ConditionType.SINGLE,
        skills=["Python", "FastAPI"],
        experienceDescription="API를 직접 설계하고 구현한 경험",
    )
    job_analysis = JobAnalysisResult(requirements=[requirement])
    match_result = JobMatchResult(
        jobTitle="백엔드 개발자",
        companyName="테스트 회사",
        requirementMatches=[
            RequirementMatch(
                requirement=requirement.description,
                requirementCategory=requirement.category,
                status=MatchStatus.INSUFFICIENT_EVIDENCE,
                reason="1차 분석에서 근거 부족",
            )
        ],
    )

    def fake_validation(items):
        item = items[0]
        return [
            RequirementMatch(
                requirement=item["currentMatch"].requirement,
                requirementCategory=item["currentMatch"].requirementCategory,
                status=MatchStatus.EXPERIENCED,
                reason="RAG v2 Evidence에서 직접 수행 경험 확인",
                matchedSkills=["FastAPI"],
                userEvidence=item["ragEvidence"],
            )
        ]

    with patch(
        "services.rag_validation_service.validate_requirements_with_rag",
        side_effect=fake_validation,
    ):
        result = validate_match_result_with_rag(
            user_analysis=DocumentAnalysisResult(basicProfile=BasicProfile()),
            job_analysis=job_analysis,
            match_result=match_result,
            rag_store=vector_store,
            user_id=9,
        )

    validated = result.requirementMatches[0]
    assert validated.status == MatchStatus.EXPERIENCED
    assert validated.userEvidence[0].evidenceId == "EV-0001"
    assert validated.userEvidence[0].documentId == 41
    assert result.score.overallMatchRate == 100.0


if __name__ == "__main__":
    test_agent_validation_uses_rag_v2_evidence()
    print("RAG V2 VALIDATION TEST: PASS")
