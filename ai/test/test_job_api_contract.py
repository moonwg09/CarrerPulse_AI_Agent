from unittest.mock import patch

from pydantic import ValidationError

from routers.job_router import JobPostingAnalyzeRequest, analyze_job
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
        response = analyze_job(
            JobPostingAnalyzeRequest(
                jobText="Java 백엔드 개발자 채용",
            )
        )

    assert response.jobTitle == "백엔드 개발자"
    assert response.jobId is None


def test_job_analysis_endpoint_rejects_empty_text():
    try:
        JobPostingAnalyzeRequest(jobText="")
    except ValidationError:
        pass
    else:
        raise AssertionError("빈 공고 본문이 거부되지 않았습니다.")


if __name__ == "__main__":
    test_job_analysis_endpoint_returns_structured_result()
    test_job_analysis_endpoint_rejects_empty_text()
    print("JOB API CONTRACT TEST: PASS")
