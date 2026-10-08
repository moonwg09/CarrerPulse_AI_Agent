import os
import time
from typing import List, Dict

from dotenv import load_dotenv
from google import genai
from google.genai import errors

from schemas.document_schema import DocumentAnalysisResult, DocumentType
from services.document_parser import extract_document


# .env 로드
load_dotenv()

api_key = os.getenv("GEMINI_API_KEY")

if not api_key:
    raise ValueError("GEMINI_API_KEY가 설정되어 있지 않습니다.")

client = genai.Client(api_key=api_key)


def format_document(
    document_type: DocumentType,
    contents: List[Dict]
) -> str:

    result = []

    for item in contents:

        if "page" in item:
            location = f"PAGE {item['page']}"

        elif "paragraphIndex" in item:
            location = f"PARAGRAPH {item['paragraphIndex']}"

        else:
            location = "UNKNOWN"

        result.append(
            f"[{document_type.value} | {location}]\n"
            f"{item['text']}"
        )

    return "\n\n".join(result)


def analyze_documents(
    resume_contents: List[Dict],
    self_intro_contents: List[Dict],
    portfolio_contents: List[Dict] | None = None
) -> DocumentAnalysisResult:

    resume_text = format_document(
        DocumentType.RESUME,
        resume_contents
    )

    self_intro_text = format_document(
        DocumentType.SELF_INTRO,
        self_intro_contents
    )

    portfolio_text = ""

    if portfolio_contents:
        portfolio_text = format_document(
            DocumentType.PORTFOLIO,
            portfolio_contents
        )

    prompt = f"""
당신은 취업 지원 문서를 분석하는 AI 시스템입니다.

사용자의 이력서, 자기소개서, 포트폴리오를 분석하여
정해진 구조의 데이터로 추출하세요.

반드시 다음 규칙을 지키세요.

1. 문서에 명확하게 존재하는 정보만 추출합니다.
2. 문서에 없는 정보는 추측하거나 생성하지 않습니다.
3. 확인할 수 없는 값은 null 또는 빈 배열로 반환합니다.
4. 모든 영역에는 반드시 evidence를 포함합니다.
5. evidence.text는 반드시 실제 입력 문서에 존재하는 문장을 사용합니다.
6. evidence.source는 RESUME, SELF_INTRO, PORTFOLIO 중 하나만 사용합니다.
7. PDF 근거는 page 값을 사용합니다.
8. DOCX 근거는 paragraphIndex 값을 사용합니다.
9. 동일한 기술은 중복 생성하지 않습니다.
10. 하나의 Skill 객체에는 반드시 기술 하나만 저장합니다.

잘못된 예:
"Java / Spring Framework"
"Oracle / MySQL"
"Docker / Docker Compose"

올바른 예:
"Java"
"Spring Framework"
"Oracle"
"MySQL"
"Docker"
"Docker Compose"

11. Skill category는 다음 값 중 하나만 사용합니다.

LANGUAGE
FRAMEWORK
DATABASE
CLOUD
DEVOPS
OS
AI
LIBRARY
TOOL
OTHER

12. 프로젝트와 실제 회사 경력은 반드시 구분합니다.

13. 프로젝트의 evidence에는 프로젝트 존재 근거뿐 아니라
role, skills, tasks, achievements를 판단하는 데 사용한 핵심 근거를 포함합니다.

14. 이력서뿐 아니라 자기소개서에서도 프로젝트 경험,
문제 해결 경험, 협업 경험과 관련된 근거를 적극적으로 찾습니다.

15. competencies에는 사용자의 역량을 추측해서 넣지 않습니다.
문서에 구체적인 경험이 존재하는 경우에만 추출합니다.

16. education에는 학교 교육뿐 아니라 교육/연수 과정도 포함합니다.

학교:
type = SCHOOL

교육/연수:
type = TRAINING

17. certificates에는 가능한 경우
issueDate와 issuer도 추출합니다.

18. 프로젝트 기간이 명시된 경우 period에 반드시 저장합니다.

19. achievements와 일반 tasks를 구분합니다.


예:
"로그인 API 구현"
→ tasks

"재현율 91% 달성"
→ achievements

"데이터 구축 시간 90% 단축"
→ achievements

20. 프로젝트의 참여 인원과 담당 역할을 구분합니다.

예:
"4인(BE2/FE2), 백엔드"
→ teamSize = "4인"
→ role = "백엔드"

"1인"
→ teamSize = "1인"
→ role = null

참여 인원을 role에 넣지 않습니다.

21. 자기소개서의 내용을 coverLetterSections로 분류합니다.

22. coverLetterSections.types는 다음 값만 사용합니다.
    - MOTIVATION
    - JOB_COMPETENCY
    - OTHER
    - UNCLASSIFIED

23. 자기소개서 한 항목이 여러 성격을 동시에 가지면 types에 여러 값을 넣을 수 있습니다.

24. MOTIVATION은 지원동기, 직무 선택 이유, 회사/직무에 지원한 이유에 해당합니다.

25. JOB_COMPETENCY는 직무 관련 역량, 기술 활용 경험, 문제 해결, 협업, 프로젝트 수행 경험에 해당합니다.

26. OTHER는 성격, 성장과정, 가치관 등 위 두 범주에 직접 해당하지 않는 자기소개서 내용입니다.

27. 명확하게 분류하기 어려운 경우 UNCLASSIFIED를 사용합니다.
    억지로 MOTIVATION 또는 JOB_COMPETENCY로 분류하지 않습니다.

28. coverLetterSections.title은 원문에 제목이나 문항명이 있으면 그대로 사용합니다.
    명확한 제목이 없으면 null로 둡니다.

29. coverLetterSections.summary는 해당 항목의 핵심 내용을 짧게 요약합니다.
    원문에 없는 새로운 경험이나 의미를 추가하지 않습니다.

30. coverLetterSections.evidence에는 반드시 SELF_INTRO의 실제 원문 근거를 포함합니다.

31. competencies와 coverLetterSections는 서로 다른 목적입니다.
    - coverLetterSections: 자기소개서 내용의 유형과 구조
    - competencies: 실제 경험을 통해 확인되는 역량
    두 항목 모두 필요한 경우 동시에 생성합니다.

추출 영역:

1. basicProfile
2. skills
3. projects
4. experiences
5. education
6. certificates
7. competencies
8. coverLetterSections


====================
RESUME
====================

{resume_text}


====================
SELF INTRODUCTION
====================

{self_intro_text}


====================
PORTFOLIO
====================

{portfolio_text if portfolio_text else "제공되지 않음"}
"""

    max_retries = 3

    for attempt in range(max_retries):
        try:
            response = client.models.generate_content(
                model="gemini-3.5-flash-lite",
                contents=prompt,
                config={
                    "response_mime_type": "application/json",
                    "response_schema": DocumentAnalysisResult,
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

    # Gemini가 Pydantic 형태로 파싱해줬으면 바로 반환
    if response.parsed is not None:
        return response.parsed

    # parsed가 없으면 JSON 문자열을 직접 Pydantic으로 검증
    return DocumentAnalysisResult.model_validate_json(
        response.text
    )


# 테스트 코드
if __name__ == "__main__":

    resume = extract_document(
        "test_files/test_resume_new.docx"
    )

    self_intro = extract_document(
        "test_files/test_self_intro.docx"
    )

    result = analyze_documents(
        resume_contents=resume,
        self_intro_contents=self_intro
    )

    print(
        result.model_dump_json(
            indent=2
        )
    )