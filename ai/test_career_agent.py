from pathlib import Path

from schemas.document_schema import (
    DocumentAnalysisResult,
)

from services.document_parser import (
    extract_document,
)

from services.rag_chunk_service import (
    create_document_chunks,
)

from services.rag_embedding_service import (
    embed_document_chunks,
)

from agents.career_agent import (
    build_career_agent,
)


def main():

    # ---------------------------------------------
    # 1. 사용자 분석 JSON 로딩
    # ---------------------------------------------
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

    # ---------------------------------------------
    # 2. 실제 사용자 문서 파싱
    # ---------------------------------------------
    resume_items = extract_document(
        "test_files/test_resume_new.docx"
    )

    self_intro_items = extract_document(
        "test_files/test_self_intro.docx"
    )

    # ---------------------------------------------
    # 3. 실제 문서 → RAG Chunk 생성
    # ---------------------------------------------
    resume_chunks = create_document_chunks(
        parsed_items=resume_items,
        document_type="RESUME",
    )

    self_intro_chunks = create_document_chunks(
        parsed_items=self_intro_items,
        document_type="SELF_INTRO",
    )

    rag_chunks = (
        resume_chunks
        + self_intro_chunks
    )

    print()
    print("=" * 80)
    print(
        f"RAG chunk 생성 완료: "
        f"{len(rag_chunks)}개"
    )
    print("=" * 80)

    # ---------------------------------------------
    # 4. 사용자 문서 embedding
    # ---------------------------------------------
    embedded_chunks = embed_document_chunks(
        rag_chunks
    )

    print(
        f"RAG embedding 생성 완료: "
        f"{len(embedded_chunks)}개"
    )

    # ---------------------------------------------
    # 5. 테스트 공고
    # ---------------------------------------------
    job_text = """
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

    # ---------------------------------------------
    # 6. Agent State 초기화
    # ---------------------------------------------
    state = {
        "jobText": job_text,
        "userAnalysis": user_analysis,
        "embeddedChunks": embedded_chunks,
        "jobAnalysis": None,
        "matchResult": None,
    }

    # ---------------------------------------------
    # 7. LangGraph Agent 생성
    # ---------------------------------------------
    career_agent = build_career_agent()

    # ---------------------------------------------
    # 8. LangGraph Agent 실행
    # ---------------------------------------------
    final_state = career_agent.invoke(
        state
    )

    # ---------------------------------------------
    # 9. 최종 State 결과 가져오기
    # ---------------------------------------------
    job_analysis = final_state[
        "jobAnalysis"
    ]

    match_result = final_state[
        "matchResult"
    ]

    # ---------------------------------------------
    # 10. Agent 최종 결과 출력
    # ---------------------------------------------
    print()
    print("=" * 80)
    print("Agent 최종 결과")
    print("=" * 80)

    print(
        f"회사명: "
        f"{match_result.companyName}"
    )

    print(
        f"직무: "
        f"{match_result.jobTitle}"
    )

    print(
        f"요구사항 개수: "
        f"{len(job_analysis.requirements)}"
    )

    print(
        f"최종 전체 일치율: "
        f"{match_result.score.overallMatchRate}%"
    )

    print(
        f"최종 추천 상태: "
        f"{match_result.recommendationStatus}"
    )

    print()

    for item in (
        match_result.requirementMatches
    ):

        print(
            f"{item.requirement}"
        )

        print(
            f"  status = "
            f"{item.status}"
        )

        print(
            f"  userEvidence = "
            f"{len(item.userEvidence)}개"
        )

        print(
            f"  reason = "
            f"{item.reason}"
        )

        if item.userEvidence:

            for evidence in (
                item.userEvidence
            ):

                print(
                    f"    - "
                    f"{evidence.text}"
                )

        print()


if __name__ == "__main__":
    main()