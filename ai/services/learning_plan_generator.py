import json
import os
import time
from math import ceil
from typing import List

from dotenv import load_dotenv
from google import genai
from google.genai import errors

from schemas.learning_candidate_schema import (
    LearningCandidateItem,
)
from schemas.learning_plan_schema import (
    LearningPlanRequest,
    LearningPlanResult,
)


load_dotenv()

api_key = os.getenv("GEMINI_API_KEY")

if not api_key:
    raise ValueError(
        "GEMINI_API_KEY가 설정되어 있지 않습니다."
    )

client = genai.Client(
    api_key=api_key
)


def generate_learning_plan(
    priority_items: List[LearningCandidateItem],
    request: LearningPlanRequest,
) -> LearningPlanResult:

    # ---------------------------------------------
    # 1. 입력값 검증
    # ---------------------------------------------
    if request.endDate < request.startDate:
        raise ValueError(
            "endDate는 startDate보다 빠를 수 없습니다."
        )

    if request.weeklyAvailableHours <= 0:
        raise ValueError(
            "weeklyAvailableHours는 0보다 커야 합니다."
        )

    if not priority_items:
        raise ValueError(
            "학습 우선순위 항목이 없습니다."
        )

    # ---------------------------------------------
    # 2. 전체 학습 가능 주차 계산
    # ---------------------------------------------
    total_days = (
        request.endDate
        - request.startDate
    ).days + 1

    total_weeks = ceil(
        total_days / 7
    )

    # ---------------------------------------------
    # 3. Gemini에 전달할 우선순위 데이터
    # ---------------------------------------------
    priority_data = []

    for index, item in enumerate(
        priority_items,
        start=1,
    ):
        priority_data.append(
            {
                "priority": index,
                "name": item.name,
                "type": item.type.value,
                "totalJobCount": item.totalJobCount,
                "requiredCount": item.requiredCount,
                "preferredCount": item.preferredCount,
                "partialCount": item.partialCount,
                "noExperienceCount": (
                    item.noExperienceCount
                ),
                "notSatisfiedCount": (
                    item.notSatisfiedCount
                ),
            }
        )

    priority_json = json.dumps(
        priority_data,
        ensure_ascii=False,
        indent=2,
    )

    # ---------------------------------------------
    # 4. Gemini 프롬프트
    # ---------------------------------------------
    prompt = f"""
당신은 취업 준비 학습계획을 생성하는 AI 시스템입니다.

이미 채용공고 분석과 사용자 역량 비교를 통해
학습 우선순위가 결정되어 있습니다.

당신의 역할은 우선순위를 다시 판단하는 것이 아니라,
주어진 우선순위를 바탕으로
실행 가능한 주차별 학습계획을 만드는 것입니다.

반드시 다음 규칙을 지키세요.

1. 입력으로 제공된 priority 순서를 기본 학습 순서로 사용합니다.

2. priority를 임의로 다시 계산하거나 변경하지 않습니다.

3. 전체 학습 기간은 다음과 같습니다.

- 시작일: {request.startDate}
- 종료일: {request.endDate}
- 전체 주차 수: {total_weeks}
- 주당 학습 가능 시간: {request.weeklyAvailableHours}시간

4. weekNumber는 반드시 1부터 {total_weeks} 사이여야 합니다.

5. 각 주차의 totalEstimatedHours는
   {request.weeklyAvailableHours}시간을 초과하면 안 됩니다.

5-1. 주당 학습 가능 시간은 반드시 모두 채워야 하는 목표 시간이 아니라
     한 주에 사용할 수 있는 최대 시간입니다.

     학습할 필요가 없는 내용을 추가해서
     주당 학습 가능 시간을 억지로 채우지 마세요.

5-2. 학습 우선순위에 포함되지 않은 항목은
     절대로 학습계획에 추가하지 않습니다.

     모든 LearningPlanItem.name은
     입력된 학습 우선순위의 name 중 하나여야 합니다.

5-3. 전체 학습 가능 주차를 반드시 모두 사용할 필요는 없습니다.

     전체 주차 수는 사용할 수 있는 최대 기간입니다.

     학습 우선순위에 포함된 모든 항목을
     충분히 학습할 수 있다고 판단되면
     필요한 주차까지만 weeks에 생성합니다.

     예를 들어 전체 주차 수가 12주여도
     6주면 충분하다면 6주까지만 생성할 수 있습니다.

5-4. 남은 주차를 채우기 위해
     같은 내용을 불필요하게 반복하거나
     학습 범위를 과도하게 확장하지 않습니다.

6. estimatedHours는 실제 수행 가능한 수준으로 제안합니다.

7. 하나의 학습 항목을 여러 주차에 나누어 배치할 수 있습니다.

8. SKILL 항목은 다음과 같은 방식으로 학습계획을 구성합니다.

   - 핵심 개념 학습
   - 간단한 실습
   - 실제 코드 또는 프로젝트 적용
   - 필요하면 복습

9. SKILL의 activityType은 학습 내용에 따라 다음을 사용합니다.

   - TECH_STUDY
   - PROJECT

10. CERTIFICATE 항목은 자격증 시험 일정은 고려하지 않습니다.

11. CERTIFICATE 항목은 다음과 같은 방식으로 구성합니다.

   - 핵심 이론 학습
   - 주요 영역 정리
   - 문제 풀이
   - 취약 영역 복습

12. CERTIFICATE의 activityType은
    CERTIFICATE를 사용합니다.

13. 현재 학습 대상이 아닌 기술이나 자격증을
    절대로 새롭게 추가하지 않습니다.

    입력된 학습 우선순위에 존재하지 않는 name은
    어떠한 경우에도 생성하지 않습니다.

14. 채용공고 빈도나 필수/우대 횟수를 임의로 변경하지 않습니다.

15. LearningPlanItem의 다음 값은
    입력으로 전달된 값을 그대로 사용합니다.

    - priority
    - name
    - type
    - totalJobCount
    - requiredCount
    - preferredCount

16. taskName에는 사용자가 실제로 수행할 수 있는
    구체적인 학습 활동명을 작성합니다.

17. description에는
    해당 활동에서 무엇을 학습하거나 구현해야 하는지
    구체적으로 작성합니다.

18. 같은 주차에 여러 활동을 배치할 수 있지만
    주당 가능 시간을 넘으면 안 됩니다.

19. 학습 기간 안에 모든 항목을 배치할 수 있으면
    allItemsScheduled는 true로 설정합니다.

20. 학습 기간이 부족하여 모든 항목을
    현실적으로 배치하기 어렵다면
    allItemsScheduled는 false로 설정합니다.

21. allItemsScheduled가 false이면
    adjustmentMessage에
    학습 기간 또는 주당 학습 시간을 늘릴 필요가 있다는
    안내를 작성합니다.

22. 모든 항목을 배치할 수 있다면
    adjustmentMessage는 null로 설정합니다.

23. 각 WeeklyLearningPlan의 items는
    실제 수행 순서대로 배열합니다.

24. totalEstimatedHours는
    해당 주차 items의 estimatedHours 합계와 일치해야 합니다.

====================
학습 우선순위
====================

{priority_json}
"""

    # ---------------------------------------------
    # 5. Gemini 호출
    # ---------------------------------------------
    max_retries = 3

    for attempt in range(max_retries):

        try:
            response = client.models.generate_content(
                model="gemini-3.5-flash-lite",
                contents=prompt,
                config={
                    "response_mime_type": (
                        "application/json"
                    ),
                    "response_schema": (
                        LearningPlanResult
                    ),
                },
            )

            break

        except errors.ServerError:

            if attempt == max_retries - 1:
                raise

            wait_time = 2 ** attempt

            print(
                "Gemini 서버 오류 발생. "
                f"{wait_time}초 후 재시도합니다."
            )

            time.sleep(wait_time)

    # ---------------------------------------------
    # 6. 응답 변환
    # ---------------------------------------------
    if response.parsed is not None:
        result = response.parsed

    else:
        result = (
            LearningPlanResult
            .model_validate_json(
                response.text
            )
        )

    # ---------------------------------------------
    # 7. Python에서 고정값 보정
    # ---------------------------------------------
    result.startDate = request.startDate
    result.endDate = request.endDate
    result.weeklyAvailableHours = (
        request.weeklyAvailableHours
    )
    result.totalWeeks = total_weeks

    # ---------------------------------------------
    # 8. 허용된 학습 대상 검증
    # ---------------------------------------------
    allowed_names = {
        item.name
        for item in priority_items
    }

    for week in result.weeks:

        for item in week.items:

            if item.name not in allowed_names:
                raise ValueError(
                    f"학습 대상이 아닌 항목이 "
                    f"계획에 포함되었습니다: "
                    f"{item.name}"
                )

    # ---------------------------------------------
    # 9. 주차별 시간 검증 및 재계산
    # ---------------------------------------------
    for week in result.weeks:

        if (
            week.weekNumber < 1
            or week.weekNumber > total_weeks
        ):
            raise ValueError(
                f"잘못된 주차 번호입니다: "
                f"{week.weekNumber}"
            )

        calculated_hours = sum(
            item.estimatedHours
            for item in week.items
        )

        week.totalEstimatedHours = round(
            calculated_hours,
            2,
        )

        if (
            calculated_hours
            > request.weeklyAvailableHours
        ):
            raise ValueError(
                f"{week.weekNumber}주차의 "
                f"예상 학습시간 "
                f"{calculated_hours}시간이 "
                f"주당 가능 시간 "
                f"{request.weeklyAvailableHours}시간을 "
                f"초과했습니다."
            )

    return result