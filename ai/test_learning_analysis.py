"""SRS 규칙 회귀 테스트. 네트워크·DB·Gemini 키 없이 실행한다."""
import json
import unittest
from datetime import date, timedelta
from pathlib import Path

from fastapi.testclient import TestClient
from pydantic import ValidationError
from learning_main import app
from services.learning_analysis import AnalysisRequest, analyze


TODAY = date(2026, 10, 20)


def fixture():
    path = Path(__file__).resolve().parents[1] / "backend/src/main/resources/learning/sample-jobs.json"
    jobs = json.loads(path.read_text(encoding="utf-8"))
    for job in jobs:
        job["postedDate"] = (TODAY - timedelta(days=job.pop("postedOffsetDays"))).isoformat()
        if "deadlineOffsetDays" in job:
            job["deadline"] = (TODAY + timedelta(days=job.pop("deadlineOffsetDays"))).isoformat()
    return dict(jobs=jobs, interests=["BACKEND"], referenceDate=TODAY.isoformat())


class LearningTests(unittest.TestCase):
    def run_analysis(self, payload=None):
        return analyze(AnalysisRequest.model_validate(payload or fixture()))

    def test_selects_ten_latest_and_excludes_closed_expired_other_jobs(self):
        result = self.run_analysis()

        # 새 BACKEND 공고 3개와 기존 최신 공고 7개를 선택한다.
        # FRONTEND 공고와 종료·마감 공고는 제외한다.
        self.assertEqual(
            result["targetJobIds"],
            ["103", "102", "101", "1", "2", "3", "4", "5", "6", "7"],
        )
        self.assertEqual(result["targetJobCount"], 10)
        self.assertEqual(result["referenceDate"], "2026-10-20")

    def test_frequency_is_per_posting_and_required_wins(self):
        result = self.run_analysis()
        java = next(x for x in result["frequency"] if x["name"] == "Java")
        # 신규 BACKEND 공고가 포함되어 기존 1~7번에서만 Java가 등장한다.
        # 공고 내부의 필수·우대 중복은 필수 한 번으로 집계한다.
        self.assertEqual((java["totalJobCount"], java["requiredCount"], java["preferredCount"]), (7, 7, 0))
        self.assertEqual(len(java["jobIds"]), 7)

    def test_fewer_than_ten_and_deadline_today_is_open(self):
        payload = fixture()
        payload["jobs"] = payload["jobs"][:2]
        payload["jobs"][0]["deadline"] = TODAY.isoformat()
        self.assertEqual(self.run_analysis(payload)["targetJobCount"], 2)

    def test_no_interests_or_no_matching_jobs(self):
        for interests in ([], ["DESIGN"]):
            payload = fixture()
            payload["interests"] = interests
            result = self.run_analysis(payload)
            self.assertEqual(result["frequency"], [])
            self.assertIn("분석할 수 없습니다", result["message"])

    def test_missing_records_are_confirmation_not_gaps(self):
        result = self.run_analysis()
        self.assertEqual(result["preparation"], [])
        # 확장된 BACKEND 공고의 9개 역량 모두 경험 미입력이므로 확인 대상이다.
        self.assertEqual(len(result["needsConfirmation"]), 9)

    def test_partial_only_missing_details_and_experienced_is_excluded(self):
        payload = fixture()
        payload["assessments"] = [
            dict(name="Java", type="SKILL", status="EXPERIENCED", evidence="API 개발 경험"),
            dict(name="SQL", type="SKILL", status="PARTIAL", missingDetails=["실행 계획"], evidence="기본 조회만 경험"),
        ]
        result = self.run_analysis(payload)
        self.assertEqual([x["name"] for x in result["excluded"]], ["Java"])
        self.assertEqual(result["preparation"][0]["targets"], ["실행 계획"])

    def test_partial_without_details_requires_confirmation(self):
        payload = fixture()
        payload["assessments"] = [dict(name="SQL", type="SKILL", status="PARTIAL", evidence="일부만 경험")]
        result = self.run_analysis(payload)
        self.assertFalse(result["preparation"])
        self.assertTrue(any(x["name"] == "SQL" for x in result["needsConfirmation"]))

    def test_priority_uses_total_frequency_not_required_frequency_and_ties(self):
        payload = fixture()
        payload["jobs"] = payload["jobs"][:3]
        for job in payload["jobs"]:
            job["requirements"] = [dict(name="A", type="SKILL", category="PREFERRED"),
                                   dict(name="B", type="EXPERIENCE", category="PREFERRED")]
        payload["jobs"][0]["requirements"].append(dict(name="C", type="CERTIFICATE", category="REQUIRED"))
        payload["assessments"] = [dict(name=name, type=kind, status="NO_EXPERIENCE", evidence="본인 미보유 확인")
            for name, kind in [("A", "SKILL"), ("B", "EXPERIENCE"), ("C", "CERTIFICATE")]]
        rows = self.run_analysis(payload)["preparation"]
        self.assertEqual([x["rank"] for x in rows], [1, 1, 3])
        self.assertEqual([x["preparationType"] for x in rows], ["TECHNICAL", "PROJECT", "CERTIFICATE"])

    def test_duplicate_inputs_are_rejected(self):
        payload = fixture()
        payload["jobs"].append(payload["jobs"][0])
        with self.assertRaises(ValidationError):
            AnalysisRequest.model_validate(payload)

    def test_confirmed_status_requires_evidence(self):
        payload = fixture()
        payload["assessments"] = [dict(name="Java", type="SKILL", status="NO_EXPERIENCE")]
        with self.assertRaises(ValidationError):
            AnalysisRequest.model_validate(payload)

    def test_http_contract_and_invalid_status(self):
        with TestClient(app) as client:
            self.assertEqual(client.post("/api/v1/learning/analyze", json=fixture()).status_code, 200)
            payload = fixture()
            payload["assessments"] = [dict(name="Java", type="SKILL", status="GUESS")]
            self.assertEqual(client.post("/api/v1/learning/analyze", json=payload).status_code, 422)


if __name__ == "__main__":
    unittest.main()
