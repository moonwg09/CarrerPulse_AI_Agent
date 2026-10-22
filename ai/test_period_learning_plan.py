"""PLN-04/05의 핵심 생성 규칙. Gemini는 가상 응답으로 검증하며 외부 호출하지 않는다."""
import unittest
from unittest.mock import patch
from decimal import Decimal
from pydantic import ValidationError
from fastapi.testclient import TestClient
from learning_main import app
from services.period_learning_plan import PeriodRequest, ActivityProposal, generate_period_plan, GenerationUnavailable


def payload(**changes):
    value = dict(startDate="2026-10-20", endDate="2026-11-02", weeklyAvailableHours=8, mode="TEST",
        priorities=[dict(name="SQL", preparationType="TECHNICAL", rank=1, targets=["실행 계획"]),
                    dict(name="API", preparationType="PROJECT", rank=2, targets=["API 구현"])])
    value.update(changes)
    return value


class PeriodTests(unittest.TestCase):
    def generate(self, **changes):
        return generate_period_plan(PeriodRequest.model_validate(payload(**changes)))

    def test_no_filler_and_preserves_input(self):
        result = self.generate()
        self.assertEqual(result["totalWeeks"], 2)
        self.assertEqual(len(result["weeks"]), 1)
        self.assertEqual(result["weeks"][0]["totalEstimatedHours"], 8)
        self.assertEqual(result["endDate"], "2026-11-02")
        self.assertTrue(result["allItemsScheduled"])
        self.assertIn("추정치", result["estimateNotice"])

    def test_split_at_weekly_cap_preserves_priority(self):
        result = self.generate(weeklyAvailableHours=3)
        self.assertEqual([w["totalEstimatedHours"] for w in result["weeks"]], [3, 3])
        self.assertEqual([i["name"] for w in result["weeks"] for i in w["items"]], ["SQL", "SQL", "API"])
        self.assertFalse(result["allItemsScheduled"])
        self.assertEqual(result["unscheduled"][0]["remainingHours"], 2)
        self.assertIsNotNone(result["adjustmentMessage"])

    def test_partial_week_capacity_and_same_day(self):
        result = self.generate(endDate="2026-10-20", weeklyAvailableHours=7)
        self.assertEqual(result["weeks"][0]["availableHours"], 1)
        self.assertEqual(result["weeks"][0]["totalEstimatedHours"], 1)
        self.assertEqual(sum(x["remainingHours"] for x in result["unscheduled"]), 7)

    def test_tiny_budget_is_not_rounded_up(self):
        result = self.generate(endDate="2026-10-20", weeklyAvailableHours="0.01")
        self.assertEqual(result["weeks"], [])
        self.assertFalse(result["allItemsScheduled"])

    def test_invalid_dates_hours_empty_priorities(self):
        for changes in [dict(endDate="2026-10-19"), dict(weeklyAvailableHours=0),
                        dict(weeklyAvailableHours=-1), dict(weeklyAvailableHours=169),
                        dict(weeklyAvailableHours="NaN"), dict(priorities=[])]:
            with self.subTest(changes=changes), self.assertRaises(ValidationError):
                PeriodRequest.model_validate(payload(**changes))

    def test_rejects_ai_target_injection_and_wrong_activity_type(self):
        request = PeriodRequest.model_validate(payload())
        for index, kind in [(99, "PROJECT"), (0, "CERTIFICATE")]:
            proposal = ActivityProposal(activities=[dict(targetIndex=index, activityType=kind,
                taskName="활동", description="설명", estimatedHours=1)])
            with self.assertRaises(GenerationUnavailable):
                generate_period_plan(request, lambda _: proposal)

    def test_ai_omission_rejected(self):
        proposal = ActivityProposal(activities=[dict(targetIndex=0, activityType="TECH_STUDY",
            taskName="SQL", description="설명", estimatedHours=1)])
        with self.assertRaises(GenerationUnavailable):
            generate_period_plan(PeriodRequest.model_validate(payload()), lambda _: proposal)

    def test_mocked_gemini_proposal_is_scheduled_without_fallback(self):
        proposal = ActivityProposal(activities=[dict(targetIndex=i, activityType=kind,
            taskName="제안 활동", description="세부 활동", estimatedHours=Decimal("2.25"))
            for i, kind in [(1, "PROJECT"), (0, "TECH_STUDY")]])
        result = generate_period_plan(PeriodRequest.model_validate(payload(mode="GEMINI")), lambda _: proposal)
        self.assertEqual(result["mode"], "GEMINI")
        self.assertEqual(result["weeks"][0]["items"][0]["name"], "SQL")
        self.assertEqual(result["weeks"][0]["totalEstimatedHours"], 4.5)

    def test_http_test_mode_and_missing_api_key(self):
        with TestClient(app) as client:
            response = client.post("/api/v1/learning/plans/generate", json=payload())
            self.assertEqual(response.status_code, 200)
            with patch.dict("os.environ", {"GEMINI_API_KEY": ""}):
                response = client.post("/api/v1/learning/plans/generate", json=payload(mode="GEMINI"))
                self.assertEqual(response.status_code, 503)


if __name__ == "__main__":
    unittest.main()
