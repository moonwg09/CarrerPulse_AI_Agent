from unittest.mock import patch

from fastapi.testclient import TestClient

from main import app
from schemas.job_schema import JobAnalysisResult


def test_job_analysis_endpoint_returns_structured_result():
    expected = JobAnalysisResult(
        jobTitle="백엔드 개발자",
        companyName="테스트 회사",
    )

    with patch(
        "routers.job_router.analyze_job_posting",
        return_value=expected,
    ):
        response = TestClient(app).post(
            "/api/v1/jobs/analyze",
            json={"jobText": "Java 백엔드 개발자 채용"},
        )

    assert response.status_code == 200
    assert response.json()["jobTitle"] == "백엔드 개발자"
    assert response.json()["jobId"] is None


def test_job_analysis_endpoint_rejects_empty_text():
    response = TestClient(app).post(
        "/api/v1/jobs/analyze",
        json={"jobText": ""},
    )

    assert response.status_code == 422


if __name__ == "__main__":
    test_job_analysis_endpoint_returns_structured_result()
    test_job_analysis_endpoint_rejects_empty_text()
    print("JOB API CONTRACT TEST: PASS")
