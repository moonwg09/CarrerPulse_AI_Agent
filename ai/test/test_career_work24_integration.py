import os
import unittest
from datetime import date
from unittest.mock import Mock, patch

os.environ.setdefault("GEMINI_API_KEY", "test-key")

from fastapi import HTTPException

from routers.career_router import analyze_job_with_files, analyze_work24_with_files
from schemas.job_collection_schema import JobCollectionResult
from schemas.raw_job_schema import RawJobPosting


class CareerWork24IntegrationTest(unittest.TestCase):
    @patch("routers.career_router._analyze_jobs_with_files")
    def test_existing_file_api_keeps_two_manual_jobs(self, pipeline_mock):
        pipeline_mock.return_value = {"matchResults": []}

        analyze_job_with_files(
            jobText1="첫 번째 공고",
            jobText2="두 번째 공고",
            closeDate1=date(2026, 11, 30),
            closeDate2=date(2026, 12, 15),
            startDate=date(2026, 11, 1),
            endDate=date(2026, 12, 31),
            weeklyAvailableHours=5.0,
            resume=Mock(),
            selfIntro=Mock(),
            portfolio=None,
        )

        jobs = pipeline_mock.call_args.kwargs["jobs"]
        self.assertEqual(2, len(jobs))
        self.assertEqual("첫 번째 공고", jobs[0]["text"])
        self.assertEqual("두 번째 공고", jobs[1]["text"])

    @patch("routers.career_router._analyze_jobs_with_files")
    @patch("routers.career_router.collect_work24_jobs")
    def test_collected_job_is_passed_to_existing_career_pipeline(
        self,
        collect_mock,
        pipeline_mock,
    ):
        job = RawJobPosting(
            jobId="work24_K123",
            externalJobId="K123",
            title="백엔드 개발자",
            companyName="테스트 주식회사",
            region="서울특별시 강남구",
            postedDate=date(2026, 10, 28),
            closeDate=date(2026, 11, 30),
            url="https://www.work24.go.kr/detail/K123",
            content="직무내용\nPython API 개발\n\n경력\n경력 2년 이상",
            source="WORK24",
        )
        collect_mock.return_value = JobCollectionResult(
            jobKeyword="개발자",
            region="서울",
            requestedLimit=1,
            collectedCount=1,
            jobs=[job],
        )
        pipeline_mock.return_value = {"matchResults": []}

        result = analyze_work24_with_files(
            jobKeyword="개발자",
            region="서울",
            limit=1,
            startDate=date(2026, 11, 1),
            endDate=date(2026, 12, 31),
            weeklyAvailableHours=5.0,
            resume=Mock(),
            selfIntro=Mock(),
            portfolio=None,
        )

        pipeline_jobs = pipeline_mock.call_args.kwargs["jobs"]
        self.assertEqual("work24_K123", pipeline_jobs[0]["jobId"])
        self.assertIn("회사명: 테스트 주식회사", pipeline_jobs[0]["text"])
        self.assertIn("채용공고 제목: 백엔드 개발자", pipeline_jobs[0]["text"])
        self.assertIn("경력 2년 이상", pipeline_jobs[0]["text"])
        self.assertEqual([job], result["collectedJobs"])

    @patch("routers.career_router.collect_work24_jobs")
    def test_empty_collection_returns_not_found(self, collect_mock):
        collect_mock.return_value = JobCollectionResult(
            jobKeyword="개발자",
            region="서울",
            requestedLimit=1,
            collectedCount=0,
            jobs=[],
        )

        with self.assertRaises(HTTPException) as raised:
            analyze_work24_with_files(
                jobKeyword="개발자",
                region="서울",
                limit=1,
                startDate=date(2026, 11, 1),
                endDate=date(2026, 12, 31),
                weeklyAvailableHours=5.0,
                resume=Mock(),
                selfIntro=Mock(),
                portfolio=None,
            )

        self.assertEqual(404, raised.exception.status_code)


if __name__ == "__main__":
    unittest.main()
