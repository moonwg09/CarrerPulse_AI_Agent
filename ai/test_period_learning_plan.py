"""PLN-04/05의 핵심 생성 규칙. Gemini는 가상 응답으로 검증하며 외부 호출하지 않는다."""
import unittest
from unittest.mock import patch
from decimal import Decimal
from fastapi import HTTPException
from pydantic import ValidationError
from routers.learning_router import generate_plan
from services.period_learning_plan import PeriodRequest, ActivityProposal, generate_period_plan, GenerationUnavailable


def payload(**changes):
    value = dict(startDate="2026-10-20", endDate="2026-11-02", weeklyAvailableHours=8, mode="GEMINI",
        priorities=[dict(name="SQL", preparationType="TECHNICAL", rank=1, targets=["실행 계획"]),
                    dict(name="API", preparationType="PROJECT", rank=2, targets=["API 구현"])])
    value.update(changes)
    return value


def fixed_proposal(request):
    kinds = {"TECHNICAL": "TECH_STUDY", "CERTIFICATE": "CERTIFICATE",
             "PROJECT": "PROJECT", "DOCUMENT": "DOCUMENT_IMPROVEMENT"}
    return ActivityProposal(activities=[dict(
        targetIndex=index,
        taskName=f"{target.name} 준비",
        description="준비 범위: " + ", ".join(target.targets),
        activityType=kinds[target.preparationType],
        estimatedHours=4,
    ) for index, target in enumerate(request.priorities)])


class PeriodTests(unittest.TestCase):
    def generate(self, **changes):
        return generate_period_plan(
            PeriodRequest.model_validate(payload(**changes)),
            fixed_proposal,
        )

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

    def test_one_decimal_hour_budget_is_supported(self):
        result = self.generate(endDate="2026-10-20", weeklyAvailableHours="0.1")
        self.assertEqual(result["weeklyAvailableHours"], 0.1)
        self.assertFalse(result["allItemsScheduled"])

    def test_invalid_dates_hours_empty_priorities(self):
        for changes in [dict(endDate="2026-10-19"), dict(weeklyAvailableHours=0),
                        dict(weeklyAvailableHours=-1), dict(weeklyAvailableHours=169),
                        dict(weeklyAvailableHours="0.01"), dict(weeklyAvailableHours="NaN"),
                        dict(priorities=[])]:
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

    def test_http_missing_api_key_does_not_return_fake_plan(self):
        with patch.dict("os.environ", {"GEMINI_API_KEY": ""}):
            with self.assertRaises(HTTPException) as raised:
                generate_plan(PeriodRequest.model_validate(payload()))
        self.assertEqual(raised.exception.status_code, 503)


if __name__ == "__main__":
    unittest.main()
