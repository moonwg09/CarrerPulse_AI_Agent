from pathlib import Path
from datetime import date

from schemas.document_schema import DocumentAnalysisResult
from schemas.recommendation_schema import JobRecommendationItem

from services.recommendation_service import (
    build_recommendation_list,
    build_growth_candidate_list,
) 
from services.requirement_frequency import(
    calculate_requirement_frequency,
)

from services.learning_candidate_service import(
    build_learning_candidates,
)

from services.learning_priority import(
    calculate_learning_priority,
)

from datetime import date

from schemas.learning_plan_schema import (
    LearningPlanRequest,
)

from services.learning_plan_generator import (
    generate_learning_plan,
)


from services.rag_embedding_service import (
    embed_document_chunks,
)

from services.document_parser import extract_document
from services.rag_chunk_service import create_document_chunks

from agents.career_agent import(
    build_career_agent,
)

test_jobs = [
    {
        "jobId": "job-001",
        "closeDate": "2026-10-20",
        "text": """
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
    },

    {
        "jobId": "job-002",
        "closeDate": "2026-10-18",
        "text": """
회사명: 베타소프트
직무: Java 백엔드 개발자

담당업무
- Java 기반 서버 개발
- Spring Boot 기반 API 개발
- 클라우드 환경 배포 및 운영

자격요건
- Java 기반 백엔드 개발 경험
- Spring Framework 또는 Spring Boot 사용 경험
- REST API 개발 경험
- 학사 이상
- 신입 지원 가능

우대사항
- AWS EC2 사용 경험
- Docker 또는 Kubernetes 경험
- SQLD 또는 정보처리기사 보유자 우대
"""
    },

    {
        "jobId": "job-003",
        "closeDate": "2026-10-25",
        "text": """
회사명: 감마시스템
직무: 백엔드 개발자

담당업무
- 웹 서비스 백엔드 개발
- 데이터베이스 설계 및 운영
- 클라우드 기반 서비스 운영

자격요건
- Java 및 Spring Boot 기반 개발 경험
- PostgreSQL 사용 경험
- AWS 운영 경험
- Git을 활용한 협업 경험
- 학사 이상

우대사항
- Redis 사용 경험
- Kafka 사용 경험
- Kubernetes 사용 경험
"""
    },

    {
        "jobId": "job-004",
        "closeDate": "2026-10-30",
        "text": """
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
    },

    {
        "jobId": "job-005",
        "closeDate": "2026-10-15",
        "text": """
회사명: 엡실론테크
직무: 백엔드 개발자

담당업무
- 서버 애플리케이션 개발
- 데이터 처리 및 API 연동

자격요건
- Java, Kotlin 경험자
- Spring 계열 프레임워크 경험자
- 데이터베이스 사용 경험
- 관련 전공자

우대사항
- 클라우드 사용 경험 우대
- 관련 자격증 보유자 우대

기타
- Redis, Kafka 경험자
- 협업 도구 사용 경험자
"""
    }
]


if __name__ == "__main__":

    # 1. 사용자 분석 JSON 로딩
    user_json_text = Path(
        "test_files/user_analysis.json"
    ).read_text(encoding="utf-8")

    user_result = DocumentAnalysisResult.model_validate_json(
        user_json_text
    )

    # -------------------------------------------------
    # 실제 사용자 문서 파싱
    # -------------------------------------------------
    resume_path = "test_files/test_resume_new.docx"
    self_intro_path = "test_files/test_self_intro.docx"

    resume_items = extract_document(
        resume_path
    )

    self_intro_items = extract_document(
        self_intro_path
    )

    # -------------------------------------------------
    # 실제 문서 → RAG Chunk
    # -------------------------------------------------
    resume_chunks = create_document_chunks(
        parsed_items=resume_items,
        document_type="RESUME",
        chunk_size=700,
        overlap=100,
    )

    self_intro_chunks = create_document_chunks(
        parsed_items=self_intro_items,
        document_type="SELF_INTRO",
        chunk_size=700,
        overlap=100,
    )

    rag_chunks = (
        resume_chunks
        + self_intro_chunks
    )

    print(
        f"RAG chunk 생성 완료: "
        f"{len(rag_chunks)}개"
    )

    # -------------------------------------------------
    # 사용자 문서 embedding
    # -------------------------------------------------
    embedded_rag_chunks = (
        embed_document_chunks(
            rag_chunks
        )
    )

    print(
        f"RAG embedding 생성 완료: "
        f"{len(embedded_rag_chunks)}개"
    )


    recommendation_items = []
    analyzed_jobs = []

        

     # -------------------------------------------------
    # Agent Graph 생성
    # -------------------------------------------------
    career_agent = build_career_agent()

    # -------------------------------------------------
    # 2. 공고 5개 Agent 실행
    # -------------------------------------------------
    for job in test_jobs:

        print()
        print("=" * 80)
        print(
            f"공고 Agent 실행 시작: "
            f"{job['jobId']}"
        )
        print("=" * 80)

        state = {
            "jobText": job["text"],
            "userAnalysis": user_result,
            "embeddedChunks": embedded_rag_chunks,
            "jobAnalysis": None,
            "matchResult": None,
        }

        # 공고 1개에 대해 Agent 전체 실행
        final_state = career_agent.invoke(
            state
        )

        job_analysis = final_state[
            "jobAnalysis"
        ]

        match_result = final_state[
            "matchResult"
        ]

        # 요구 역량 빈도 계산용
        analyzed_jobs.append(
            (
                job["jobId"],
                job_analysis
            )
        )

        # 추천 / 사용자 상태 비교용
        recommendation_items.append(
            JobRecommendationItem(
                jobId=job["jobId"],
                closeDate=date.fromisoformat(
                    job["closeDate"]
                ),
                matchResult=match_result,
            )
        )

        print(
            f"{match_result.companyName} | "
            f"전체={match_result.score.overallMatchRate}% | "
            f"필수={match_result.score.requiredMatchRate}% | "
            f"우대={match_result.score.preferredMatchRate}% | "
            f"객관={match_result.score.objectiveMatchRate}% | "
            f"평가가능={match_result.score.evaluationCoverage}% | "
            f"상태={match_result.recommendationStatus} | "
            f"경고={match_result.warnings}"
        )


    # 4. 추천 목록 생성
    recommendations = build_recommendation_list(
        recommendation_items
    )

    growth_candidates = build_growth_candidate_list(
        recommendation_items
    )

    # 5. 요구 역량 빈도 계산
    frequency_result = calculate_requirement_frequency(
        analyzed_jobs
    )

    # 6. 사용자 상태와 결합
    learning_candidates = build_learning_candidates(
        frequency_result=frequency_result,
        jobs=recommendation_items,
    )

    # 7. 기본 학습 우선순위 계산
    learning_priority = calculate_learning_priority(
        learning_candidates
    )

    # 8. 학습계획 요청
    learning_plan_request = LearningPlanRequest(
        startDate=date(2026, 10, 12),
        endDate=date(2026, 12, 31),
        weeklyAvailableHours=10,
    )

    # 9. 주차별 학습계획 생성
    learning_plan = generate_learning_plan(
        priority_items=learning_priority,
        request=learning_plan_request,
    )

    # 10. 최종 추천 결과 출력
    print()
    print("=" * 80)
    print("최종 추천 공고")
    print("=" * 80)

    for index, job in enumerate(
        recommendations,
        start=1
    ):
        print(
            f"{index}. "
            f"{job.matchResult.companyName} | "
            f"{job.matchResult.jobTitle} | "
            f"전체={job.matchResult.score.overallMatchRate}% | "
            f"필수={job.matchResult.score.requiredMatchRate}% | "
            f"우대={job.matchResult.score.preferredMatchRate}% | "
            f"객관={job.matchResult.score.objectiveMatchRate}% | "
            f"평가가능={job.matchResult.score.evaluationCoverage}% | "
            f"상태={job.matchResult.recommendationStatus} | "
            f"경고={job.matchResult.warnings} | "
            f"마감일={job.closeDate}"
        )


    # 11. 성장 후보 결과 출력
    print()
    print("=" * 80)
    print("성장 후보 공고")
    print("=" * 80)

    for index, job in enumerate(
        growth_candidates,
        start=1
    ):
        print(
            f"{index}. "
            f"{job.matchResult.companyName} | "
            f"{job.matchResult.jobTitle} | "
            f"전체={job.matchResult.score.overallMatchRate}% | "
            f"필수={job.matchResult.score.requiredMatchRate}% | "
            f"평가가능={job.matchResult.score.evaluationCoverage}% | "
            f"상태={job.matchResult.recommendationStatus} | "
            f"경고={job.matchResult.warnings} | "
            f"마감일={job.closeDate}"
        )

    # 12. 요구 역량 빈도 출력
    print()
    print("=" * 80)
    print("요구 역량 빈도")
    print("=" * 80)

    for item in frequency_result.items:
        print(
            f"{item.name} | "
            f"type={item.type} | "
            f"전체={item.totalJobCount} | "
            f"필수={item.requiredCount} | "
            f"우대={item.preferredCount} | "
            f"공고={item.jobIds}"
        )

    # 13. 사용자 상태 결합 결과 출력
    print()
    print("=" * 80)
    print("시장 요구 빈도 + 사용자 상태")
    print("=" * 80)

    for item in learning_candidates.items:

        print(
            f"{item.name} | "
            f"type={item.type} | "
            f"전체={item.totalJobCount} | "
            f"필수={item.requiredCount} | "
            f"우대={item.preferredCount} | "
            f"EXPERIENCED={item.experiencedCount} | "
            f"PARTIAL={item.partialCount} | "
            f"NO_EXPERIENCE={item.noExperienceCount} | "
            f"INSUFFICIENT={item.insufficientEvidenceCount} | "
            f"SATISFIED={item.satisfiedCount} | "
            f"NOT_SATISFIED={item.notSatisfiedCount} | "
            f"UNCLEAR={item.unclearCount} | "
            f"학습대상={item.isLearningTarget}"
        )

    # 14. 기본 학습 우선순위 출력
    print()
    print("=" * 80)
    print("기본 학습 우선순위")
    print("=" * 80)

    for index, item in enumerate(
        learning_priority,
        start=1
    ):
        print(
            f"{index}. "
            f"{item.name} | "
            f"type={item.type} | "
            f"전체={item.totalJobCount} | "
            f"필수={item.requiredCount} | "
            f"우대={item.preferredCount} | "
            f"NO_EXPERIENCE={item.noExperienceCount} | "
            f"PARTIAL={item.partialCount} | "
            f"NOT_SATISFIED={item.notSatisfiedCount}"
        )

    # 15. 주차별 학습 계획
    print()
    print("=" * 80)
    print("주차별 학습계획")
    print("=" * 80)

    print(
        f"기간: "
        f"{learning_plan.startDate} ~ "
        f"{learning_plan.endDate}"
    )

    print(
        f"사용 가능 주차: "
        f"{learning_plan.totalWeeks}주"
    )

    print(
        f"실제 계획 주차: "
        f"{len(learning_plan.weeks)}주"
    )

    print(
        f"주당 가능 시간: "
        f"{learning_plan.weeklyAvailableHours}시간"
    )

    print(
        f"전체 항목 배치 완료: "
        f"{learning_plan.allItemsScheduled}"
    )

    if learning_plan.adjustmentMessage:
        print(
            f"조정 안내: "
            f"{learning_plan.adjustmentMessage}"
        )

    for week in learning_plan.weeks:

        print()
        print(
            f"[{week.weekNumber}주차] "
            f"총 {week.totalEstimatedHours}시간"
        )

        for index, item in enumerate(
            week.items,
            start=1,
        ):
            print(
                f"  {index}. "
                f"{item.name} | "
                f"{item.activityType} | "
                f"{item.taskName} | "
                f"{item.estimatedHours}시간"
            )

            if item.description:
                print(
                    f"     - {item.description}"
                )