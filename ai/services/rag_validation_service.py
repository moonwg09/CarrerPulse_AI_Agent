import os
from typing import Dict, List, Optional

from dotenv import load_dotenv
from google import genai
from pydantic import BaseModel

from schemas.document_schema import (
    DocumentAnalysisResult,
    Evidence,
)

from schemas.job_schema import (
    JobAnalysisResult,
    JobRequirement,
)

from schemas.match_schema import (
    JobMatchResult,
    RequirementMatch,
    MatchStatus,
)

from schemas.rag_schema import (
    EmbeddedDocumentChunk,
    RagSearchResult,
)

from services.rag_retriever import (
    retrieve_requirement_evidence,
)

from services.match_score import (
    calculate_match_score,
)

from services.recommendation_classifier import (
    classify_job,
)

from services.match_warning import (
    build_match_warnings,
)


load_dotenv()

api_key = os.getenv(
    "GEMINI_API_KEY"
)

if not api_key:
    raise ValueError(
        "GEMINI_API_KEY가 설정되어 있지 않습니다."
    )


client = genai.Client(
    api_key=api_key
)


# -------------------------------------------------
# Gemini가 여러 RequirementMatch를
# 한 번에 반환하기 위한 응답 스키마
# -------------------------------------------------
class RagBatchValidationResult(
    BaseModel
):
    matches: List[
        RequirementMatch
    ]


# -------------------------------------------------
# 1. RAG 재검증 대상 여부 판단
# -------------------------------------------------
def should_validate_with_rag(
    match: RequirementMatch,
) -> bool:

    # PARTIAL
    if (
        match.status
        == MatchStatus.PARTIAL
    ):
        return True

    # INSUFFICIENT_EVIDENCE
    if (
        match.status
        == MatchStatus.INSUFFICIENT_EVIDENCE
    ):
        return True

    # userEvidence 자체가 없음
    if not match.userEvidence:
        return True

    return False


# -------------------------------------------------
# 2. RequirementMatch와 대응되는
#    JobRequirement 찾기
# -------------------------------------------------
def find_job_requirement(
    job_analysis: JobAnalysisResult,
    match: RequirementMatch,
) -> Optional[
    JobRequirement
]:

    for requirement in (
        job_analysis.requirements
    ):

        if (
            requirement.description.strip()
            == match.requirement.strip()
        ):
            return requirement

    return None


# -------------------------------------------------
# 3. RAG 검색 결과 → Evidence 변환
# -------------------------------------------------
def convert_rag_results_to_evidence(
    rag_results: List[
        RagSearchResult
    ],
) -> List[
    Evidence
]:

    evidence_list: List[
        Evidence
    ] = []

    for result in rag_results:

        chunk = result.chunk

        evidence_list.append(
            Evidence(
                text=chunk.text,
                source=chunk.documentType,
                page=chunk.page,
                paragraphIndex=(
                    chunk.paragraphIndex
                ),
            )
        )

    return evidence_list


# -------------------------------------------------
# 4. 여러 requirement를
#    Gemini 한 번으로 재검증
# -------------------------------------------------
def validate_requirements_with_rag(
    validation_items: List[
        Dict
    ],
) -> List[
    RequirementMatch
]:

    if not validation_items:
        return []

    formatted_items: List[str] = []

    for index, item in enumerate(
        validation_items,
        start=1,
    ):

        requirement = item[
            "requirement"
        ]

        current_match = item[
            "currentMatch"
        ]

        rag_evidence = item[
            "ragEvidence"
        ]

        # -----------------------------------------
        # f-string 내부에서 직접 "\n".join()
        # 하지 않고 미리 문자열 생성
        # -----------------------------------------
        requirement_json = (
            requirement.model_dump_json(
                indent=2
            )
        )

        current_match_json = (
            current_match.model_dump_json(
                indent=2
            )
        )

        rag_evidence_json = "\n".join(
            evidence.model_dump_json(
                indent=2
            )
            for evidence in rag_evidence
        )

        formatted_item = f"""
====================
검증 대상 {index}
====================

[JOB REQUIREMENT]

{requirement_json}


[현재 1차 비교 결과]

{current_match_json}


[RAG 검색 근거 후보]

{rag_evidence_json}
"""

        formatted_items.append(
            formatted_item
        )

    validation_text = "\n".join(
        formatted_items
    )

    prompt = f"""
당신은 채용공고 요구사항과 사용자 경험을
근거 기반으로 재검증하는 AI 시스템입니다.

현재 한 개의 채용공고에 대해
1차 비교 결과가 생성되었습니다.

그중 판단이 불충분하거나
일부만 확인되었거나
사용자 근거가 없는 요구사항들에 대해
RAG를 이용하여 사용자 원문에서
관련 근거 후보를 추가 검색했습니다.

아래의 각 검증 대상을
서로 독립적으로 판단하세요.

반드시 다음 규칙을 지키세요.

1. RAG 검색 결과는 관련 가능성이 높은
   후보일 뿐이며 그 자체가
   경험을 증명하지 않습니다.

2. embedding similarity가 높다는 이유만으로
   EXPERIENCED로 판단하지 않습니다.

3. 공고 요구사항을 직접적으로 뒷받침하는
   실제 수행 근거가 확인될 때만
   EXPERIENCED로 판단합니다.

4. 관련 경험은 확인되지만
   요구사항 전체 또는 요구 수준의 일부만
   충족하면 PARTIAL로 판단합니다.

5. 기존 근거와 RAG 근거를 모두 검토했음에도
   실제 수행 여부를 판단하기 어렵다면
   INSUFFICIENT_EVIDENCE를 유지합니다.

6. RAG에서 직접 근거를 찾지 못했다는 이유만으로
   NO_EXPERIENCE로 판단하지 않습니다.

7. 이번 RAG 재검증에서는
   새로운 NO_EXPERIENCE를 생성하지 않습니다.

8. 유사 기술을 동일 기술로 간주하지 않습니다.

예:
Docker 경험 ≠ Kubernetes 경험
AWS 경험 ≠ GCP 경험
MySQL 경험 ≠ PostgreSQL 경험
Spring Framework 경험만으로
Spring Boot 경험을 자동 확정하지 않습니다.

9. 단순 기술명 나열이나 관심 표현은
   실제 수행 경험으로 인정하지 않습니다.

10. 프로젝트, 업무, 실습 등에서
    실제 수행한 내용이 확인되어야
    경험 근거로 인정합니다.

11. 기존 userEvidence와
    RAG 검색 근거를 함께 검토합니다.

12. userEvidence에는 최종 판단에 실제 사용한
    근거만 포함합니다.

13. 제공되지 않은 근거를 만들어내지 않습니다.

14. requirement는 기존 값을 유지합니다.

15. requirementCategory는 기존 값을 유지합니다.

16. jobEvidence는 기존 값을 유지합니다.

17. matchedSkills에는 실제 근거가 확인된
    기술만 포함합니다.

18. missingSkills에는 공고에서 요구하지만
    최종적으로 근거가 확인되지 않은
    기술을 포함합니다.

19. 입력된 검증 대상의 개수와
    출력 matches의 개수는 반드시 동일해야 합니다.

20. 입력 순서와 출력 matches의 순서를
    반드시 동일하게 유지합니다.


====================
RAG 재검증 대상
====================

{validation_text}
"""

    # ---------------------------------------------
    # 공고 하나당 Gemini 재검증 1회
    # ---------------------------------------------
    response = client.models.generate_content(
        model="gemini-3.5-flash-lite",
        contents=prompt,
        config={
            "response_mime_type": (
                "application/json"
            ),
            "response_schema": (
                RagBatchValidationResult
            ),
        },
    )

    if response.parsed is not None:

        result = response.parsed

    else:

        result = (
            RagBatchValidationResult
            .model_validate_json(
                response.text
            )
        )

    return result.matches


# -------------------------------------------------
# 5. JobMatchResult 전체 RAG 재검증
# -------------------------------------------------
def validate_match_result_with_rag(
    user_analysis: DocumentAnalysisResult,
    job_analysis: JobAnalysisResult,
    match_result: JobMatchResult,
    embedded_chunks: List[
        EmbeddedDocumentChunk
    ],
    top_k: int = 3,
) -> JobMatchResult:

    # ---------------------------------------------
    # Gemini에 한 번에 보낼 검증 대상
    # ---------------------------------------------
    validation_items = []

    # 기존 requirementMatches의 위치 저장
    target_indexes = []

    for index, current_match in enumerate(
        match_result.requirementMatches
    ):

        # -----------------------------------------
        # RAG 대상이 아니면 건너뜀
        # -----------------------------------------
        if not should_validate_with_rag(
            current_match
        ):
            continue

        # -----------------------------------------
        # 대응되는 원래 JobRequirement 찾기
        # -----------------------------------------
        requirement = (
            find_job_requirement(
                job_analysis=job_analysis,
                match=current_match,
            )
        )

        if requirement is None:
            continue

        # -----------------------------------------
        # requirement별 RAG 검색
        # 검색은 각각 수행
        # Gemini 판단만 나중에 한 번에 수행
        # -----------------------------------------
        rag_results = (
            retrieve_requirement_evidence(
                requirement=requirement,
                embedded_chunks=(
                    embedded_chunks
                ),
                top_k=top_k,
            )
        )

        # 검색 결과가 없으면
        # 기존 상태 그대로 유지
        if not rag_results:
            continue

        rag_evidence = (
            convert_rag_results_to_evidence(
                rag_results
            )
        )

        validation_items.append(
            {
                "requirement": requirement,
                "currentMatch": current_match,
                "ragEvidence": rag_evidence,
            }
        )

        target_indexes.append(
            index
        )

    # ---------------------------------------------
    # RAG 대상이 하나도 없으면
    # Gemini 호출 없이 그대로 반환
    # ---------------------------------------------
    if not validation_items:

        print(
            "RAG 재검증 대상 없음"
        )

        return match_result

    print(
        f"RAG 재검증 대상: "
        f"{len(validation_items)}개"
    )

    print(
        "Gemini RAG 재검증 호출: "
        "공고당 1회"
    )

    # ---------------------------------------------
    # 모든 RAG 대상 requirement를
    # Gemini 한 번으로 재검증
    # ---------------------------------------------
    validated_matches = (
        validate_requirements_with_rag(
            validation_items
        )
    )

    # ---------------------------------------------
    # 응답 개수 확인
    # ---------------------------------------------
    if (
        len(validated_matches)
        != len(target_indexes)
    ):
        raise ValueError(
            "RAG 배치 재검증 결과 개수가 "
            "입력 대상 개수와 다릅니다."
        )

    # ---------------------------------------------
    # 기존 matchResult에 결과 반영
    # ---------------------------------------------
    for (
        target_index,
        validated_match,
        validation_item,
    ) in zip(
        target_indexes,
        validated_matches,
        validation_items,
    ):

        current_match = (
            validation_item[
                "currentMatch"
            ]
        )

        # -----------------------------------------
        # 변경되면 안 되는 값은
        # 기존 값으로 강제 유지
        # -----------------------------------------
        validated_match.requirement = (
            current_match.requirement
        )

        validated_match.requirementCategory = (
            current_match.requirementCategory
        )

        validated_match.jobEvidence = (
            current_match.jobEvidence
        )

        # -----------------------------------------
        # RAG 재검증 단계에서
        # 새로운 NO_EXPERIENCE 생성 방지
        # -----------------------------------------
        if (
            validated_match.status
            == MatchStatus.NO_EXPERIENCE
        ):
            validated_match.status = (
                MatchStatus
                .INSUFFICIENT_EVIDENCE
            )

        match_result.requirementMatches[
            target_index
        ] = validated_match

    # ---------------------------------------------
    # RAG 적용 후 점수 다시 계산
    # ---------------------------------------------
    match_result.score = (
        calculate_match_score(
            match_result
        )
    )

    # ---------------------------------------------
    # 추천 상태 다시 계산
    # ---------------------------------------------
    match_result.recommendationStatus = (
        classify_job(
            match_result
        )
    )

    # ---------------------------------------------
    # 경고 다시 계산
    # ---------------------------------------------
    match_result.warnings = (
        build_match_warnings(
            match_result
        )
    )

    return match_result