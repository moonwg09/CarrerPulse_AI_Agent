from schemas.document_schema import DocumentAnalysisResult
from schemas.job_schema import (
    JobAnalysisResult,
    JobRequirement,
    RequirementCategory,
    ConditionType,
)
from schemas.match_schema import (
    JobMatchResult,
    RequirementMatch,
    MatchStatus,
)

from schemas.rag_schema import DocumentChunk

from services.rag_embedding_service import (
    embed_document_chunks,
)

from services.rag_validation_service import (
    validate_match_result_with_rag,
)

from pathlib import Path

def main():

    # -------------------------------------------------
    # 1. 실제 사용자 분석 Json 로딩
    # -------------------------------------------------
    user_json_text = Path(
        "test_files/user_analysis.json"
    ).read_text(
        encoding="utf-8"
    )

    user_analysis = (
        DocumentAnalysisResult
        .model_validate_json(
            user_json_text
        )
    )

    # -------------------------------------------------
    # 2. 테스트용 공고 요구사항
    # -------------------------------------------------
    spring_requirement = JobRequirement(
        description=(
            "Spring Boot 기반 REST API 개발 경험"
        ),
        category=RequirementCategory.REQUIRED,
        conditionType=ConditionType.SINGLE,
        skills=[
            "Spring Boot",
            "REST API",
        ],
        experienceDescription=(
            "Spring Boot를 활용한 REST API 개발 경험"
        ),
        evidence=[],
    )

    kubernetes_requirement = JobRequirement(
        description=(
            "Kubernetes 운영 및 배포 경험"
        ),
        category=RequirementCategory.PREFERRED,
        conditionType=ConditionType.SINGLE,
        skills=[
            "Kubernetes",
        ],
        experienceDescription=(
            "Kubernetes 기반 서비스 배포 및 운영 경험"
        ),
        evidence=[],
    )

    job_analysis = JobAnalysisResult(
        requirements=[
            spring_requirement,
            kubernetes_requirement,
        ]
    )

    # -------------------------------------------------
    # 3. 1차 matcher 결과를 가정
    # -------------------------------------------------
    spring_match = RequirementMatch(
        requirement=(
            "Spring Boot 기반 REST API 개발 경험"
        ),
        requirementCategory=(
            RequirementCategory.REQUIRED
        ),
        status=MatchStatus.PARTIAL,
        reason=(
            "Spring 관련 경험은 확인되지만 "
            "Spring Boot 기반 REST API 수행 근거가 부족합니다."
        ),
        matchedSkills=[
            "Spring"
        ],
        missingSkills=[
            "Spring Boot",
            "REST API",
        ],
        jobEvidence=[],
        userEvidence=[],
    )

    kubernetes_match = RequirementMatch(
        requirement=(
            "Kubernetes 운영 및 배포 경험"
        ),
        requirementCategory=(
            RequirementCategory.PREFERRED
        ),
        status=(
            MatchStatus.INSUFFICIENT_EVIDENCE
        ),
        reason=(
            "Kubernetes 직접 수행 근거를 "
            "확인할 수 없습니다."
        ),
        matchedSkills=[],
        missingSkills=[
            "Kubernetes"
        ],
        jobEvidence=[],
        userEvidence=[],
    )

    match_result = JobMatchResult(
        requirementMatches=[
            spring_match,
            kubernetes_match,
        ],
        objectiveConditionMatches=[],
    )

    # -------------------------------------------------
    # 4. 테스트용 실제 사용자 원문 Chunk
    # -------------------------------------------------
    chunks = [
        DocumentChunk(
            chunkId="RESUME_0",
            documentType="RESUME",
            text=(
                "MBook 프로젝트에서 "
                "Spring Boot와 JPA를 사용하여 "
                "REST API를 구현했습니다."
            ),
            page=1,
        ),

        DocumentChunk(
            chunkId="RESUME_1",
            documentType="RESUME",
            text=(
                "AWS EC2 환경에 Docker 컨테이너로 "
                "서비스를 배포하고 "
                "GitHub Actions를 이용해 CI를 구성했습니다."
            ),
            page=1,
        ),

        DocumentChunk(
            chunkId="SELF_INTRO_0",
            documentType="SELF_INTRO",
            text=(
                "Linux 수동 배포, Docker 컨테이너화, "
                "AWS 배포를 수행했습니다."
            ),
            paragraphIndex=8,
        ),
    ]

    embedded_chunks = embed_document_chunks(
        chunks
    )

    # -------------------------------------------------
    # 5. RAG 적용 전 출력
    # -------------------------------------------------
    print("=" * 80)
    print("RAG 적용 전")
    print("=" * 80)

    for match in match_result.requirementMatches:
        print(
            f"{match.requirement}"
        )
        print(
            f"status = {match.status}"
        )
        print(
            f"reason = {match.reason}"
        )
        print()

    # -------------------------------------------------
    # 6. RAG 재검증
    # -------------------------------------------------
    validated_result = (
        validate_match_result_with_rag(
            user_analysis=user_analysis,
            job_analysis=job_analysis,
            match_result=match_result,
            embedded_chunks=embedded_chunks,
            top_k=3,
        )
    )

    # -------------------------------------------------
    # 7. RAG 적용 후 출력
    # -------------------------------------------------
    print()
    print("=" * 80)
    print("RAG 적용 후")
    print("=" * 80)

    for match in (
        validated_result.requirementMatches
    ):

        print(
            f"{match.requirement}"
        )

        print(
            f"status = {match.status}"
        )

        print(
            f"reason = {match.reason}"
        )

        print(
            f"matchedSkills = "
            f"{match.matchedSkills}"
        )

        print(
            f"missingSkills = "
            f"{match.missingSkills}"
        )

        print(
            f"userEvidence = "
            f"{len(match.userEvidence)}개"
        )

        for evidence in match.userEvidence:

            print(
                f"  - {evidence.text}"
            )

        print()


if __name__ == "__main__":
    main()