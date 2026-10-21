"""실제 Gemini/Qwen을 사용하는 CareerPulse 핵심 시연 흐름 점검 스크립트.

자동 단위 테스트에는 포함하지 않고, API 키가 설정된 개발 환경에서 직접 실행한다.
"""
from datetime import date
from pathlib import Path

from fastapi import UploadFile

from routers.career_router import analyze_job_with_files


JOB_TEXT_1 = """
회사명: 알파테크
직무: 백엔드 개발자

담당업무
- Java/Spring 기반 웹 서비스 개발
- REST API 설계 및 개발
- MySQL 데이터베이스 연동
- Docker 기반 서비스 배포

자격요건
- Java 또는 Kotlin 기반 서버 개발 경험
- Spring Framework 및 REST API 개발 경험
- MySQL 사용 경험
- 신입 또는 경력 2년 이하

우대사항
- Docker 사용 경험
- AWS 환경에서 서비스 배포 경험
- Git을 활용한 협업 경험
- 정보처리기사 보유자 우대
"""


JOB_TEXT_2 = """
회사명: 델타랩
직무: Node.js 백엔드 개발자

담당업무
- Node.js 기반 API 서버 개발
- TypeScript 기반 서비스 개발
- MongoDB 데이터 모델링
- Kubernetes 기반 서비스 운영

자격요건
- Node.js 개발 경험
- TypeScript 개발 경험
- MongoDB 사용 경험
- Kubernetes 운영 경험
- 경력 3년 이상

우대사항
- GraphQL 개발 경험
- GCP 사용 경험
- Kafka 운영 경험
"""


def run_pipeline():
    ai_root = Path(__file__).resolve().parents[1]
    resume_path = ai_root / "test_files" / "test_resume_new.docx"
    self_intro_path = ai_root / "test_files" / "test_self_intro.docx"

    with resume_path.open("rb") as resume_file, self_intro_path.open("rb") as intro_file:
        result = analyze_job_with_files(
            jobText1=JOB_TEXT_1,
            jobText2=JOB_TEXT_2,
            closeDate1=date(2026, 11, 5),
            closeDate2=date(2026, 11, 6),
            startDate=date(2026, 10, 22),
            endDate=date(2026, 11, 30),
            weeklyAvailableHours=10,
            resume=UploadFile(
                file=resume_file,
                filename=resume_path.name,
            ),
            selfIntro=UploadFile(
                file=intro_file,
                filename=self_intro_path.name,
            ),
            portfolio=None,
            userId=9001,
            ragConsent=True,
        )

    assert result["ragIndex"]["embeddingMode"] == "qwen3"
    assert len(result["ragIndex"]["documents"]) == 2
    assert len(result["matchResults"]) == 2
    assert result["requirementFrequency"] is not None
    assert result["learningPlan"] is not None

    evidence_count = sum(
        document["evidenceCount"]
        for document in result["ragIndex"]["documents"]
    )
    cited_evidence_count = sum(
        len(requirement.userEvidence)
        for match_result in result["matchResults"]
        for requirement in match_result.requirementMatches
    )

    print("CAREER PIPELINE E2E: PASS")
    print(f"Embedding: {result['ragIndex']['embeddingMode']}")
    print(f"Indexed evidence: {evidence_count}")
    print(f"Matched jobs: {len(result['matchResults'])}")
    print(f"Cited evidence: {cited_evidence_count}")
    print(f"Recommendations: {len(result['recommendations'])}")
    print(f"Growth candidates: {len(result['growthCandidates'])}")
    print(f"Learning priorities: {len(result['learningPriority'])}")
    print(f"Learning weeks: {len(result['learningPlan'].weeks)}")

    return result


if __name__ == "__main__":
    run_pipeline()
