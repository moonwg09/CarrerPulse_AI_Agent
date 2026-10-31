from types import SimpleNamespace
from unittest.mock import patch

from routers import career_router


def main():
    request = career_router.GuidanceRequest(
        userId=7,
        documentIds=[11],
        requirement="Spring Boot 개발 경험",
        requirementType="REQUIRED",
        status="일부만 있음",
        missingPart="운영 경험이 문서에 부족합니다.",
    )
    fake_client = SimpleNamespace(models=SimpleNamespace())

    with (
        patch.object(career_router, "get_scoped_store", return_value=object()),
        patch.object(career_router, "search_evidence", return_value=[(0.9, object())]),
        patch.object(career_router, "get_gemini_client", return_value=fake_client),
        patch.object(
            career_router,
            "generate_and_verify",
            return_value={
                "status": "확인된 근거 기반",
                "lines": ["확인된 프로젝트 역할을 먼저 작성하세요."],
            },
        ),
    ):
        response = career_router.generate_document_guidance(request)

    assert response.status == "확인된 근거 기반"
    assert response.evidenceCount == 1
    assert response.lines == ["확인된 프로젝트 역할을 먼저 작성하세요."]
    print("GUIDANCE API TEST: PASS")


if __name__ == "__main__":
    main()
