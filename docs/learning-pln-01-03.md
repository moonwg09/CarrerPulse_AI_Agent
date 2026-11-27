# PLN-01~03 학습 분석

## 운영 흐름

1. 사용자는 관심 직무를 1개 이상 등록한다.
2. 학습 분석 요청 시 지역을 반드시 입력한다. `전국`도 사용자가 직접 선택한 값이다.
3. Spring은 관심 직무명과 지역을 모두 만족하는 모집 중 공고를 최신순 최대 10개 조회한다.
4. 구조화 요구사항 또는 최신 비교 결과가 없는 공고만 `AnalysisService`로 분석한다.
5. `job_requirements.evaluation_rule`의 기술·자격증·수행 경험과 최신 `COMPLETED` 비교 결과를 결합한다.
6. Python은 공고별 중복 제거, 필수 우선 집계, 부족 역량 및 공동 순위를 계산한다.

브라우저는 사용자 ID, 경험 판정, Evidence 또는 공고 요구사항을 전달하지 않는다.
가상 공고와 가상 사용자 입력은 운영 화면 및 Spring API에서 사용하지 않는다.

## API

`POST /api/v1/learning/analysis/me`

로그인 세션과 CSRF 토큰이 필요하다.

```json
{
  "region": "서울특별시"
}
```

- 관심 직무는 로그인 사용자의 `user_jobs`에서 읽는다.
- 지역이 `전국`이면 지역 필터만 생략한다.
- 이미 완료된 공고 분석은 DB에서 재사용하고 미분석 공고만 분석하여 중복 LLM 호출을 줄인다.
- 일부 공고 분석이 실패해도 성공한 공고로 계속 진행하고 실패 건수를 반환한다.
- Python 입력 검증 실패는 422, 연결 실패는 503, 처리 실패는 502로 구분한다.
- 요구사항이 없는 공고는 빈 요구사항으로 성공 처리하지 않고 제외 건수로 표시한다.

## SRS 적용 규칙

| 항목 | 적용 규칙 |
| --- | --- |
| PLN-01 | OPEN 상태이고 마감되지 않은 관심 직무·지역 공고를 최신순 최대 10개 사용한다. 실제 확보 수만 분석한다. |
| 빈도 | 기술, 자격증, 수행 경험을 공고별 1회 집계한다. 같은 항목이 필수와 우대에 모두 있으면 필수로 집계한다. |
| PLN-02 | 경험 있음은 제외하고, 일부 경험은 부족 범위가 확인된 경우만 학습 대상으로 사용한다. 기록 부족은 경험 없음으로 바꾸지 않는다. |
| PLN-03 | 필수 빈도, 전체 빈도, 사용자 부족 상태를 적용한다. 동점은 공동 순위로 유지한다. |

`NO_EXPERIENCE`는 `comparison_items.user_confirmed=true`인 경우에만 사용한다.
객관 조건의 `UNMET`은 자격증 미충족으로 변환할 수 있지만 학력·경력 조건은 학습 후보에서 제외한다.
일부 경험인데 부족 세부 항목이 저장되지 않은 경우에는 서류 보완·추가 확인 대상으로 분리한다.

## 저장 계약

새 테이블을 추가하지 않는다.

- `job_requirements`: 공고의 정규화된 요구조건과 학습 메타데이터 캐시
- `comparison_results`: 사용자·공고별 최신 비교 결과
- `comparison_items`: 요구조건별 판정과 대표 Evidence
- `learning_plans`, `learning_plan_items`: 확정된 기간 학습계획

요구사항 메타데이터는 기존 `evaluation_rule`에 JSON으로 저장한다.

```json
{
  "version": 1,
  "skills": ["Java", "Spring"],
  "experience": "REST API 개발 경험"
}
```

자격증 요구조건은 `certificates` 배열을 사용한다. 공고 내용이 변경되어 요구조건을 다시 분석하면 같은 요구조건 키의 메타데이터를 갱신한다.

## 현재 제한

- PARTIAL 판정의 구체적인 부족 기술은 현재 비교 테이블에 별도 컬럼이 없어, 확인 전에는 학습 대상으로 단정하지 않는다.
- 실제 Gemini 기간 계획 호출과 로그인부터 저장·조회까지의 브라우저 전체 흐름은 별도로 검증한다.

## 검증

```powershell
cd ai
python -m unittest test_learning_analysis -v

cd backend
.\gradlew.bat test

cd frontend
npm run build
npx eslint src/pages/LearningPage.jsx src/components/LearningPlanPanel.jsx src/api/learningApi.js
```
