import os
import time

from dotenv import load_dotenv
from google import genai
from google.genai import errors

from schemas.job_schema import JobAnalysisResult


load_dotenv()

api_key = os.getenv("GEMINI_API_KEY")

if not api_key:
    raise ValueError("GEMINI_API_KEY가 설정되어 있지 않습니다.")

client = genai.Client(api_key=api_key)


def analyze_job_posting(
    job_posting_text: str
) -> JobAnalysisResult:

    prompt = f"""
당신은 채용공고를 분석하는 AI 시스템입니다.

아래 채용공고를 분석하여 정해진 구조로 정보를 추출하세요.

반드시 다음 규칙을 지키세요.

1. 채용공고에 명확하게 존재하는 정보만 사용합니다.
2. 공고에 없는 내용은 추측하거나 생성하지 않습니다.
3. 확인할 수 없는 값은 null 또는 빈 배열로 반환합니다.
4. 담당업무, 필수 자격요건, 우대사항을 서로 구분합니다.
5. requirements는 '요구사항 한 문장 또는 의미 단위'를 하나의 객체로 생성합니다.
6. requirements.category는 다음 값만 사용합니다.
   - REQUIRED
   - PREFERRED
   - UNCLEAR

7. 필수 자격요건에 명확하게 포함된 경우 REQUIRED를 사용합니다.
8. 우대사항에 명확하게 포함된 경우 PREFERRED를 사용합니다.
9. 필수/우대 여부가 명확하지 않으면 UNCLEAR를 사용합니다.
   임의로 REQUIRED 또는 PREFERRED로 판단하지 않습니다.

10. conditionType은 다음 값만 사용합니다.
    - SINGLE
    - AND
    - OR
    - UNCLEAR

11. 하나의 기술 또는 하나의 단일 조건이면 SINGLE을 사용합니다.

12. 여러 조건을 모두 만족해야 하는 경우 AND를 사용합니다.
    예:
    "Java 및 Spring Framework 기반 개발 경험"
    → AND

13. 여러 조건 중 하나를 만족하면 되는 경우 OR를 사용합니다.
    예:
    "Java 또는 Kotlin 기반 개발 경험"
    → OR

14. 쉼표나 나열만으로 조건 관계를 명확하게 판단할 수 없는 경우
    UNCLEAR를 사용합니다.

    예:
    "Redis, Kafka 경험자"
    → 단순 쉼표 나열만으로 두 기술을 모두 요구하는지,
       둘 중 하나를 요구하는지 확정할 수 없습니다.
    → conditionType은 UNCLEAR입니다.

    쉼표로 나열되어 있다는 이유만으로
    AND 또는 OR로 판단하지 않습니다.

15. skills에는 해당 요구사항에 직접 등장하거나 명확하게 연결되는
    기술만 개별 문자열로 추출합니다.

16. 기술은 가능한 한 개별 기술명으로 분리합니다.
    잘못된 예:
    ["Java / Spring Framework"]

    올바른 예:
    ["Java", "Spring Framework"]

17. experienceDescription에는 해당 요구사항에서 요구하는
    경험의 내용을 기록합니다.

    예:
    "Java 또는 Kotlin 기반 서버 개발 경험"
    → "서버 개발 경험"

18. 단순 기술명만 있고 별도의 경험 내용이 없으면
    experienceDescription은 null로 둘 수 있습니다.

19. 모든 requirement에는 가능한 경우 evidence를 포함합니다.

20. evidence.text는 실제 채용공고에 존재하는 문장을 사용합니다.
    공고에 없는 문장을 새로 만들지 않습니다.

21. responsibilities에는 실제 담당업무만 기록합니다.
    자격요건이나 우대사항을 responsibilities에 넣지 않습니다.

22. 학력, 경력, 자격증과 같은 객관 조건은
    objectiveRequirements에 추출합니다.

23. objectiveRequirements.type은 다음 값만 사용합니다.

    - EDUCATION
      학력 조건

    - CAREER
      신입/경력 여부 또는 요구 경력 연수 조건

    - CERTIFICATE
      자격증 또는 면허 조건

24. 각 objectiveRequirement에는 반드시 category를 설정합니다.

    category는 다음 값만 사용합니다.

    - REQUIRED
    - PREFERRED
    - UNCLEAR

25. 객관 조건이 자격요건에 명확하게 포함되어 있으면
    REQUIRED로 판단합니다.

26. 객관 조건이 우대사항에 명확하게 포함되어 있으면
    PREFERRED로 판단합니다.

27. 객관 조건의 필수/우대 여부가 명확하지 않으면
    UNCLEAR로 판단합니다.
    임의로 REQUIRED 또는 PREFERRED로 추측하지 않습니다.

28. objectiveRequirement.description에는
    채용공고의 실제 요구조건을 간결하게 기록합니다.

    예:
    "학사 이상"
    → type: EDUCATION
    → description: "학사 이상"

    "신입 또는 경력 2년 이하"
    → type: CAREER
    → description: "신입 또는 경력 2년 이하"

    "정보처리기사 보유자 우대"
    → type: CERTIFICATE
    → description: "정보처리기사 보유자 우대"

29. 각 objectiveRequirement에는 반드시 conditionType을 설정합니다.

    conditionType은 다음 값만 사용합니다.

    - SINGLE
      단일 조건

    - AND
      여러 조건을 모두 만족해야 하는 경우

    - OR
      여러 조건 중 하나만 만족하면 되는 경우

    - UNCLEAR
      조건 관계를 명확하게 판단할 수 없는 경우

30. objectiveRequirement가 단일 조건이면
    conditionType은 SINGLE입니다.

    예:
    "정보처리기사 보유자 우대"
    → conditionType: SINGLE

31. objectiveRequirement에 "및", "그리고", "모두"와 같이
    여러 조건을 모두 만족해야 함이 명확하면
    conditionType은 AND입니다.

    예:
    "정보처리기사 및 SQLD 보유"
    → conditionType: AND

32. objectiveRequirement에 "또는", "OR", "중 하나"와 같이
    여러 조건 중 하나만 만족하면 됨이 명확하면
    conditionType은 OR입니다.

    예:
    "SQLD 또는 정보처리기사 보유자 우대"
    → conditionType: OR

33. objectiveRequirement의 items에는
    실제 개별 조건을 각각 분리하여 기록합니다.

    description 전체 문장을 하나의 items 값으로 넣지 않습니다.

    예:
    "정보처리기사 보유자 우대"
    →
    items: ["정보처리기사"]

    "SQLD 또는 정보처리기사 보유자 우대"
    →
    items: ["SQLD", "정보처리기사"]

    "정보처리기사 및 SQLD 보유"
    →
    items: ["정보처리기사", "SQLD"]

34. 자격증이 여러 개 OR 또는 AND 관계로 등장하더라도
    하나의 objectiveRequirement 안에서 관리합니다.

    잘못된 예:

    objectiveRequirement 1
    → CERTIFICATE: SQLD

    objectiveRequirement 2
    → CERTIFICATE: 정보처리기사

    올바른 예:

    objectiveRequirement 1
    → type: CERTIFICATE
    → description: "SQLD 또는 정보처리기사 보유자 우대"
    → category: PREFERRED
    → conditionType: OR
    → items: ["SQLD", "정보처리기사"]

35. 각 objectiveRequirement에는 가능한 경우 evidence를 포함합니다.

36. evidence.text는 실제 공고 원문을 사용합니다.

37. JobEvidence.source는 항상 "JOB_POSTING"으로 설정합니다.

38. JobEvidence.section에는 해당 조건이 위치한 공고 영역을 기록합니다.

    예:
    - 자격요건
    - 우대사항
    - 학력
    - 경력
    - 기타

39. 학력, 경력, 자격증 또는 면허 조건은 반드시
    objectiveRequirements에만 추출합니다.

    이러한 객관 조건은 requirements에 포함하지 않습니다.

    특히 자격증이 우대사항에 위치하더라도
    requirements가 아니라 objectiveRequirements에 추출합니다.

40. requirements는 기술 사용 경험, 개발 경험, 협업 경험 등
    기술/경험 중심 요구사항만 생성합니다.
    학력, 경력 연수, 자격증, 면허는 objectiveRequirements로 분리합니다.

       
추출해야 할 정보:

1. jobTitle
2. companyName
3. responsibilities
4. requirements
5. objectiveRequirements
6. keywords

====================
JOB POSTING
====================

{job_posting_text}
"""

    max_retries = 3

    for attempt in range(max_retries):
        try:
            response = client.models.generate_content(
                model="gemini-3.5-flash-lite",
                contents=prompt,
                config={
                    "response_mime_type": "application/json",
                    "response_schema": JobAnalysisResult,
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
        return response.parsed

    return JobAnalysisResult.model_validate_json(
        response.text
    )

if __name__ == "__main__":

    test_job = """
회사명: ABC테크
직무: 백엔드 개발자

담당업무
- Java/Spring 기반 웹 서비스 개발
- REST API 설계 및 개발
- MySQL 데이터베이스 연동

자격요건
- Java 또는 Kotlin 기반 서버 개발 경험
- Spring Framework 및 REST API 개발 경험
- MySQL 사용 경험
- 학사 이상
- 신입 또는 경력 2년 이하

우대사항
- Docker 사용 경험
- AWS 환경에서 서비스 배포 경험
- Git을 활용한 협업 경험
- 정보처리기사 보유자 우대

기타
- Redis, Kafka 경험자
"""

    result = analyze_job_posting(test_job)

    print(result.model_dump_json(indent=2))