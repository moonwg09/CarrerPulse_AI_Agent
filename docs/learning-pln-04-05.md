# PLN-04 기간 기반 생성 / PLN-05 저장된 계획 조회

## 화면 확인 순서

1. 직접 실행 중인 Spring Boot와 Python 서버를 새 코드로 재시작합니다. 별도 중복 서버를 켜지 않습니다.
2. 기존 Python 실행 명령은 `python -m uvicorn learning_main:app --host 127.0.0.1 --port 8000`입니다.
3. 로그인한 뒤 `/learning`에서 관심 직무와 선택 지역의 저장된 분석 결과로 PLN-01~03 분석을 실행합니다.
4. 아래 **기간 기반 학습계획 생성**에서 시작일·종료일·주당 시간을 입력합니다.
5. 시작일·종료일·주당 학습 가능 시간을 입력해 Gemini 계획을 생성합니다.
6. 새로고침 후 **내 최신 계획 불러오기**를 누릅니다. 날짜·가능 시간·주차·순서·유형·예상 시간이 복원됩니다.
7. 실제 Gemini 생성은 Python 서버 환경에 `GEMINI_API_KEY`와 사용할 `GEMINI_MODEL`을 설정한 후 선택합니다.

Python 실행 PowerShell에 환경 변수를 설정하는 예시입니다. 키는 화면에 출력하거나 Git에 저장하지 않습니다.

```powershell
$env:GEMINI_API_KEY = [System.Net.NetworkCredential]::new(
    '', (Read-Host 'Gemini API 키' -AsSecureString)
).Password
# 기본 모델은 기존 프로젝트와 같은 gemini-3.5-flash-lite입니다.
# 계정에서 사용할 모델이 다르면 GEMINI_MODEL 환경 변수로 지정합니다.
python -m uvicorn learning_main:app --host 127.0.0.1 --port 8000
```

현재 `learning_main`은 .env를 자동으로 읽지 않습니다. 위 터미널의 환경 변수를 사용합니다.
운영 Spring API는 DB 공고와 저장된 최신 사용자 비교 결과만 사용하며 생성 방식은 Gemini로 고정합니다.

## SRS 적용

- 시작일·종료일·주당 학습 시간을 필수 입력받습니다. 종료일이 시작일보다 빠르거나 시간이 0 이하이면 거절합니다.
- 한 번의 생성 기간은 730일 이내, 주당 시간은 168시간 이하, 소수 둘째 자리까지 지원합니다.
- 계획 생성 시 서버가 본인의 관심 직무/역량 비교를 다시 계산합니다. 브라우저가 정한 우선순위를 신뢰하지 않습니다.
- 충분한 경험 및 근거 미확인 항목은 학습계획에 새로 끼워 넣지 않습니다. 일부 경험은 부족 세부 항목만 전달합니다.
- 실제 생성에서는 Gemini가 활동/산출물/추정 시간을 제안합니다. API 키/응답 오류 시 테스트 계획으로 몰래 대체하지 않습니다.
- Gemini는 원문 이력서·이메일·사용자 ID·비교 근거를 받지 않습니다. 준비 대상·세부 범위·순위·기간·시간만 받습니다.
- Python은 Gemini 응답의 허용 대상/활동 유형/양수 시간/대상 누락 여부를 검사합니다.
- 서버가 우선순위순으로 시간을 배치하므로 AI 응답 순서가 달라도 학습 순서는 유지됩니다.
- 주차는 시작일을 기준으로 7일씩 나눕니다. 마지막 부분 주차는 `주당 시간 × 남은 일수 ÷ 7`을 소수 둘째 자리 내림한 상한을 사용합니다.
- 시간이 남아도 활동을 추가하거나 빈 주차를 채우지 않습니다. 활동은 필요하면 여러 주차로 나눕니다.
- 기간 부족 시 미배치 활동과 남은 추정 시간을 표시합니다. 모든 활동을 배치한 것처럼 표시하지 않습니다.
- 조회에는 생성 당시 입력 기간과 주당 가능 시간, 주차별 범위, 준비 항목, 수행 순서, 활동 유형, 예상 시간을 함께 표시합니다.
- 계획이 없으면 시작일·종료일·주당 가능 시간을 입력하라는 안내를 표시합니다.
- 예상 소요 시간은 추정치임을 표시합니다.

## API와 저장

- `POST /api/v1/learning/plans/me`: 세션 로그인·CSRF 보호. `region`, `startDate`, `endDate`, `weeklyAvailableHours`를 입력받습니다.
- `GET /api/v1/learning/plans/me/latest`: 본인의 최신 ACTIVE 계획을 읽습니다. 계획 없음은 404이며 Gemini를 재호출하지 않습니다.
- Python 내부 `POST /api/v1/learning/plans/generate`: 검증된 준비 대상과 기간을 받아 계획을 생성합니다.

승인된 `learning_plans`, `learning_plan_items`에 헤더·활동을 저장합니다.
두 테이블의 저장은 한 트랜잭션이므로 일부 활동 저장 실패 시 헤더까지 롤백됩니다.
조회 SQL에는 반드시 로그인 사용자의 user_id 조건이 들어갑니다.
같은 사용자도 새 계획 생성 시 기존 계획을 덮어쓰지 않고 새 plan_id를 저장합니다.

최신 계획 조회는 계획 헤더와 활동을 다시 조합합니다. 생성 직후 응답에는 기간 안에
배치하지 못한 활동도 표시하지만, 승인된 테이블에 해당 항목을 저장할 컬럼이 없으므로
재조회 화면은 실제 저장된 주차별 활동만 보여 줍니다.

Gemini 호출은 다른 분석 기능과 같은 `google-genai` 클라이언트와 `GEMINI_MODEL` 설정을 사용합니다.
API 키 누락이나 호출 실패를 가상 계획 성공으로 대체하지 않습니다.

## 최종 회귀 테스트 범위

- Python 분석·기간 계획 테스트
- `careerpulse_test`를 사용하는 Spring Repository 및 API 테스트
- 프런트엔드 빌드와 ESLint
- 실제 Gemini 호출과 로그인부터 계획 저장·조회까지의 시연 흐름

```powershell
cd C:\CarrerPulse_AI_Agent\ai
python -m unittest test_learning_analysis test_period_learning_plan -v

cd C:\CarrerPulse_AI_Agent\backend
.\gradlew.bat test

cd C:\CarrerPulse_AI_Agent\frontend
npm run build
npx eslint src/components/LearningPlanPanel.jsx src/pages/LearningPage.jsx src/api/learningApi.js
```

MySQL 통합 테스트는 `test` 프로필과 `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`를 사용해
`careerpulse_test`에서 실행합니다. 개발 DB는 조회하거나 변경하지 않습니다.
