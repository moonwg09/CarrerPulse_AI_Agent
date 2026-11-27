from types import SimpleNamespace

from schemas.document_schema import DocumentType, Evidence
from schemas.job_schema import RequirementCategory
from schemas.match_schema import (
    JobMatchResult,
    ObjectiveConditionMatch,
    ObjectiveConditionType,
    ObjectiveStatus,
)
from services.rag_validation_service import link_match_result_evidence_metadata


def result_with(evidence: Evidence) -> JobMatchResult:
    return JobMatchResult(
        objectiveConditionMatches=[
            ObjectiveConditionMatch(
                conditionType=ObjectiveConditionType.CERTIFICATE,
                requirement="정보처리기사 우대",
                requirementCategory=RequirementCategory.PREFERRED,
                status=ObjectiveStatus.SATISFIED,
                reason="정보처리기사 취득이 확인됩니다.",
                userEvidence=[evidence],
            )
        ]
    )


def main():
    source_text = "정보처리기사 자격증을 취득했습니다."
    indexed = SimpleNamespace(
        user_id=7,
        deleted=False,
        consented=True,
        evidence_text=source_text,
        doc_type="resume",
        source_location="p.1",
        evidence_id="EV-0154",
        document_id=11,
    )
    store = SimpleNamespace(evidences=[indexed])
    legacy = Evidence(text=source_text, source=DocumentType.RESUME)

    linked = link_match_result_evidence_metadata(result_with(legacy), store, 7)
    condition = linked.objectiveConditionMatches[0]
    assert condition.status == ObjectiveStatus.SATISFIED
    assert condition.userEvidence[0].evidenceId == "EV-0154"
    assert linked.score.objectiveMatchRate == 100.0

    unlinked = link_match_result_evidence_metadata(
        result_with(Evidence(text="다른 문장", source=DocumentType.RESUME)),
        store,
        7,
    )
    condition = unlinked.objectiveConditionMatches[0]
    assert condition.status == ObjectiveStatus.UNCLEAR
    assert condition.userEvidence == []
    assert unlinked.score.evaluationCoverage == 0.0
    print("OBJECTIVE EVIDENCE LINK TEST: PASS")


if __name__ == "__main__":
    main()
