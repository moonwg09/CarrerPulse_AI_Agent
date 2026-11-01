import unittest

from schemas.document_schema import DocumentType, Evidence
from schemas.job_schema import JobEvidence, RequirementCategory
from schemas.match_schema import JobMatchResult, MatchStatus, RequirementMatch
from services.match_score import calculate_match_score


class ResponsibilityScoringTest(unittest.TestCase):
    def test_responsibility_match_does_not_change_qualification_score(self):
        responsibility = RequirementMatch(
            requirement="REST API 설계 및 구현",
            requirementCategory=RequirementCategory.UNCLEAR,
            status=MatchStatus.EXPERIENCED,
            reason="사용자 수행 근거 확인",
            jobEvidence=[JobEvidence(text="REST API 설계 및 구현")],
            userEvidence=[Evidence(
                text="FastAPI로 REST API를 설계했습니다.",
                source=DocumentType.RESUME,
            )],
        )
        result = JobMatchResult(responsibilityMatches=[responsibility])

        score = calculate_match_score(result)

        self.assertEqual(0.0, score.overallMatchRate)
        self.assertEqual(0.0, score.evaluationCoverage)


if __name__ == "__main__":
    unittest.main()
