import os
import time

from dotenv import load_dotenv
from google import genai
from google.genai import errors
from pathlib import Path

from schemas.document_schema import DocumentAnalysisResult
from schemas.job_schema import JobAnalysisResult
from schemas.match_schema import JobMatchResult
from schemas.match_schema import MatchStatus
from services.match_score import calculate_match_score
from services.recommendation_classifier import classify_job
from services.match_warning import build_match_warnings


load_dotenv()

api_key = os.getenv("GEMINI_API_KEY")

if not api_key:
    raise ValueError("GEMINI_API_KEY가 설정되어 있지 않습니다.")

client = genai.Client(api_key=api_key)

def normalize_match_statuses(
    result: JobMatchResult
) -> JobMatchResult:

    for requirement in result.requirementMatches:

        # NO_EXPERIENCE는
        # 사용자가 "경험이 없다"고 명확하게 밝힌 근거가 있어야 함
        #
        # 근거 없이 NO_EXPERIENCE가 나왔다면
        # 단순히 문서에서 확인되지 않은 것이므로
        # INSUFFICIENT_EVIDENCE로 변경
        if (
            requirement.status
            == MatchStatus.NO_EXPERIENCE
            and not requirement.userEvidence
        ):
            requirement.status = (
                MatchStatus.INSUFFICIENT_EVIDENCE
            )

        # PARTIAL은 최소한 관련 경험 근거가 있어야 함
        #
        # 관련 경험 근거가 전혀 없는데 PARTIAL이면
        # 일부 경험이라고 확정할 수 없으므로
        # INSUFFICIENT_EVIDENCE로 변경
        elif (
            requirement.status
            == MatchStatus.PARTIAL
            and not requirement.userEvidence
        ):
            requirement.status = (
                MatchStatus.INSUFFICIENT_EVIDENCE
            )

    return result

def match_user_to_job(
    user_analysis: DocumentAnalysisResult,
    job_analysis: JobAnalysisResult
) -> JobMatchResult:

    print("사용자 JSON 로딩 성공")
    print("공고 JSON 로딩 성공")
    print(f"공고 요구사항 개수: {len(job_analysis.requirements)}")
    print("Gemini matcher 호출 시작")

    user_json = user_analysis.model_dump_json(indent=2)
    job_json = job_analysis.model_dump_json(indent=2)

    prompt = f"""
당신은 사용자의 경력 및 역량과 채용공고 요구사항을 비교하는 AI 시스템입니다.

아래 USER ANALYSIS와 JOB ANALYSIS를 비교하여
각 채용공고 requirement에 대한 판단 결과를 생성하세요.

반드시 다음 규칙을 지키세요.

1. JOB ANALYSIS의 requirements를 기준으로 하나씩 비교합니다.

2. 사용자에게 실제 근거가 있는 경우에만 경험이 있다고 판단합니다.

3. 사용자 문서에 해당 내용이 없다는 이유만으로
   NO_EXPERIENCE로 판단하지 않습니다.

4. MatchStatus는 다음 네 값만 사용합니다.

   EXPERIENCED
   - 요구사항을 충족하는 구체적인 사용자 경험과 근거가 확인됩니다.

   PARTIAL
   - 관련 경험은 확인되지만 요구사항 전체 또는 요구 수준의 일부만 충족합니다.

   INSUFFICIENT_EVIDENCE
   - 현재 사용자 문서만으로는 해당 경험 여부를 판단하기 어렵습니다.

   NO_EXPERIENCE
   - 사용자가 해당 경험이 없다고 명확하게 밝힌 근거가 있을 때만 사용합니다.

5. 사용자의 skills 이름만 보고 경험이 있다고 단정하지 않습니다.
   가능하면 projects, experiences, competencies 등의 실제 수행 근거를 함께 확인합니다.

6. userEvidence에는 USER ANALYSIS 안에 실제로 존재하는 evidence만 사용합니다.
   새로운 근거를 만들어내지 않습니다.

7. jobEvidence에는 JOB ANALYSIS의 해당 requirement evidence를 사용합니다.

8. reason에는 왜 해당 상태로 판단했는지 짧고 명확하게 설명합니다.

9. matchedSkills에는 공고 요구사항 중 사용자 근거로 확인되는 기술만 넣습니다.

10. missingSkills에는 공고에서 요구하지만 사용자 근거에서 확인되지 않는 기술을 넣습니다.

11. missingSkills가 있다고 해서 반드시 NO_EXPERIENCE는 아닙니다.

12. conditionType이 OR인 경우:
    여러 기술 중 하나 이상의 요구조건을 충분한 근거로 충족하면
    EXPERIENCED로 판단할 수 있습니다.

13. conditionType이 AND인 경우:
    모든 주요 조건에 대한 근거가 확인되어야 EXPERIENCED입니다.
    일부 조건만 확인되면 PARTIAL로 판단합니다.

14. conditionType이 SINGLE인 경우:
    해당 단일 요구조건에 대한 근거를 기준으로 판단합니다.

15. conditionType이 UNCLEAR인 경우:
    공고 원문의 의미를 유지하면서 보수적으로 판단합니다.
    조건 관계를 임의로 만들어내지 않습니다.

16. REQUIRED, PREFERRED, UNCLEAR 여부 자체는
    경험 상태를 바꾸는 기준이 아닙니다.
    requirementCategory는 공고의 원래 값을 그대로 유지합니다.

17. 학력, 경력, 자격증 조건은 requirementMatches에 포함하지 않습니다.
    해당 조건들은 objectiveConditionMatches에서 별도로 비교합니다.

18. 모든 requirement에 대해 반드시 하나의 RequirementMatch를 생성합니다.

19. conditionType이 OR이고 하나 이상의 선택 조건을 충분히 충족하여
    status가 EXPERIENCED인 경우, 충족하지 않은 대안 기술은
    missingSkills에 포함하지 않습니다.

20. JOB ANALYSIS의 objectiveRequirements를 하나씩 사용자 데이터와 비교합니다.

21. 각 objectiveRequirement에 대해 반드시 하나의
    ObjectiveConditionMatch를 생성합니다.

22. objectiveConditionMatches의 status는 다음 세 값만 사용합니다.

    SATISFIED
    - 사용자 데이터에서 해당 조건을 충족하는 것이 명확하게 확인됩니다.

    NOT_SATISFIED
    - 사용자 데이터에서 해당 조건을 충족하지 못하는 것이 명확하게 확인됩니다.

    UNCLEAR
    - 현재 사용자 데이터만으로 충족 여부를 판단하기 어렵습니다.

23. objectiveRequirement.type이 EDUCATION인 경우
    USER ANALYSIS의 education 중 type이 SCHOOL인 항목을 기준으로 비교합니다.

24. TRAINING 유형의 교육과정은 정규 학력으로 판단하지 않습니다.

25. 학력 단계는 의미를 고려하여 비교합니다.

    예:
    공고: "학사 이상"
    사용자: "전문학사"
    → NOT_SATISFIED

    공고: "전문학사 이상"
    사용자: "전문학사"
    → SATISFIED

26. objectiveRequirement.type이 CAREER인 경우
    USER ANALYSIS의 basicProfile.careerLevel과 experiences를 함께 참고합니다.

27. 공고가 "신입 또는 경력 2년 이하"이고
    사용자가 신입으로 확인되면 SATISFIED로 판단합니다.

28. 사용자의 경력 수준 또는 요구 경력 충족 여부를
    현재 데이터로 명확하게 판단할 수 없으면 UNCLEAR로 판단합니다.

29. objectiveRequirement.type이 CERTIFICATE인 경우
    USER ANALYSIS의 certificates와 비교합니다.

30. 자격증 이름이 정확히 일치하거나
    동일한 자격증임이 명확한 경우에만 SATISFIED로 판단합니다.

31. 서로 다른 등급 또는 서로 다른 자격증은
    같은 자격증으로 판단하지 않습니다.

    예:
    "정보처리기사"
    ≠
    "정보처리산업기사"

32. 사용자가 요구 자격증을 보유하지 않은 것이 명확하면
    NOT_SATISFIED로 판단합니다.

33. 자격증 상태도 함께 확인합니다.
    시험 일부 합격, 1차 합격 등은 최종 자격증 보유로 판단하지 않습니다.

34. objectiveRequirement.category는 JOB ANALYSIS의 값을 그대로 유지합니다.
    REQUIRED, PREFERRED, UNCLEAR 여부가
    SATISFIED / NOT_SATISFIED / UNCLEAR 판정 자체를 바꾸지는 않습니다.

35. objectiveConditionMatch에는 해당 객관 조건의 category를
    반드시 그대로 포함합니다.

36. objectiveConditionMatch.requirement에는
    objectiveRequirement.description을 그대로 사용합니다.

37. 객관 조건 비교의 reason에는
    어떤 사용자 데이터와 비교했는지 짧고 명확하게 설명합니다.

38. JOB ANALYSIS의 모든 objectiveRequirement에 대해
    빠짐없이 하나의 ObjectiveConditionMatch를 생성합니다.   

39. 사용자 문서에서 해당 기술이나 경험이 확인되지 않는다는 이유만으로
    NO_EXPERIENCE를 사용하지 않습니다.

    문서에 근거가 없거나 실제 수행 여부를 확인할 수 없는 경우에는
    INSUFFICIENT_EVIDENCE를 사용합니다.

    NO_EXPERIENCE는 사용자가 해당 경험이 없다고 명확하게 밝힌
    근거가 있을 때만 사용합니다.

40. EXPERIENCED로 판단하는 경우에는 해당 경험을 실제로 수행했다는
    구체적인 evidence가 반드시 userEvidence에 포함되어야 합니다.

    단순 기술명, 숙련도 표기, 기술 목록만으로는
    구체적인 경험 요구사항을 EXPERIENCED로 판단하지 않습니다.

    예:
    "AWS | 하"
    만 존재하는 경우
    "AWS 환경에서 서비스 배포 경험"을 EXPERIENCED로 판단하지 않습니다.

41. reason에서 특정 프로젝트, 배포, 협업 등의 수행 경험을 근거로 언급했다면
    그 내용을 뒷받침하는 실제 evidence를 반드시 userEvidence에도 포함합니다.
    userEvidence에 존재하지 않는 내용을 reason에서 근거처럼 사용하지 않습니다.

====================
USER ANALYSIS
====================

{user_json}


====================
JOB ANALYSIS
====================

{job_json}
"""

    max_retries = 3

    for attempt in range(max_retries):
        try:
            response = client.models.generate_content(
                model="gemini-3.5-flash-lite",
                contents=prompt,
                config={
                    "response_mime_type": "application/json",
                    "response_schema": JobMatchResult,
                },
            )

            break

        except errors.ServerError:
            if attempt == max_retries - 1:
                raise

            wait_time = 2 ** attempt

            print(
                f"Gemini 서버 오류 발생. "
                f"{wait_time}초 후 재시도합니다."
            )

            time.sleep(wait_time)

    if response.parsed is not None:
        result = response.parsed
    else:
        result = JobMatchResult.model_validate_json(response.text)

    result = normalize_match_statuses(result)

    result.score = calculate_match_score(result)

    result.recommendationStatus = classify_job(result)

    result.warnings = build_match_warnings(result)

    return result

if __name__ == "__main__":
    user_json_text = Path(
        "test_files/user_analysis.json"
    ).read_text(encoding="utf-8")

    job_json_text = Path(
        "test_files/job_analysis.json"
    ).read_text(encoding="utf-8")

    user_result = DocumentAnalysisResult.model_validate_json(
        user_json_text
    )

    job_result = JobAnalysisResult.model_validate_json(
        job_json_text
    )

    result = match_user_to_job(
        user_analysis=user_result,
        job_analysis=job_result
    )

    print(result.model_dump_json(indent=2))