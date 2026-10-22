# PLN-04 기간 기반 생성 / PLN-05 저장된 계획 조회

## 화면 확인 순서

1. 직접 실행 중인 Spring Boot와 Python 서버를 새 코드로 재시작합니다. 별도 중복 서버를 켜지 않습니다.
2. 기존 Python 실행 명령은 `python -m uvicorn learning_main:app --host 127.0.0.1 --port 8000`입니다.
3. 로그인한 뒤 `/learning`에서 자체 공고와 가상 사용자 예시로 PLN-01~03 분석을 실행합니다.
4. 아래 **기간 기반 학습계획 생성**에서 시작일·종료일·주당 시간을 입력합니다.
5. 우선 **자체 테스트 계획**으로 생성·저장을 확인합니다. 테스트 계획의 활동/시간은 가상임을 표시합니다.
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
자체 공고(TEST/DATABASE)와 생성 방식(TEST/GEMINI)은 서로 별개입니다.
따라서 자체 가상 공고로도 실제 Gemini 생성이 가능합니다.

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
- 예상 소요 시간은 추정치임을 표시합니다. 자체 테스트 계획은 모든 대상에 고정 4시간을 사용하는 검증용 데이터입니다.

## API와 저장

- `POST /api/v1/learning/plans/me`: 세션 로그인·CSRF 보호. `source`, `assessments`, `startDate`, `endDate`, `weeklyAvailableHours`, `mode`를 입력받습니다.
- `GET /api/v1/learning/plans/me/latest`: 본인의 최신 ACTIVE 계획을 읽습니다. 계획 없음은 404이며 Gemini를 재호출하지 않습니다.
- Python 내부 `POST /api/v1/learning/plans/generate`: 검증된 준비 대상과 기간을 받아 계획을 생성합니다.

기존 `learning_plans`, `learning_plan_items`에 헤더·활동을 저장합니다.
생성 당시 전체 결과는 새 `learning_plan_snapshots` 테이블의 JSON으로 보존합니다.
세 테이블의 저장은 한 트랜잭션이므로 일부 활동 저장 실패 시 헤더까지 롤백됩니다.
조회 SQL에는 반드시 로그인 사용자의 user_id 조건이 들어갑니다.
같은 사용자도 새 계획 생성 시 기존 계획을 덮어쓰지 않고 새 plan_id를 저장합니다.

로컬 개발 DB에는 스냅샷 테이블을 추가했습니다. 다른 개발 환경은 다음 SQL을 한 번 실행해야 합니다.

`backend/src/main/resources/db/learning-plan-snapshots.sql`

기존 테이블이나 데이터의 삭제/변경은 없으며 `CREATE TABLE IF NOT EXISTS`를 사용합니다.
기존 계획 중 스냅샷이 없는 이전 버전 데이터는 이 새 조회 화면의 대상이 아닙니다.

Gemini 호출은 추가 SDK 설치 없이 Python 표준 라이브러리의 HTTPS 요청을 사용합니다.
[공식 generateContent REST 문서](https://ai.google.dev/api/generate-content)를 기준으로 연결했으며
모델 이름은 환경 변수로 바꿀 수 있습니다.

## 확인한 범위

- Python 전체 20개 테스트: 기존 분석 11개 + 기간 계획 9개.
- Java 전체 12개 테스트: 기존 분석 API 5개 + 계획 API 5개 + 실제 MySQL 저장/조회/롤백 2개.
- MySQL 테스트는 기존 사용자 식별자만 사용하며 검증용 계획·활동·스냅샷은 롤백했습니다.
- 프런트엔드 빌드와 변경 파일 ESLint 통과.
- 실제 Gemini 유료 호출과 사용자 브라우저에서의 로그인부터 계획 조회까지 전 과정은 이 검증에 포함되지 않습니다.
- 외부 공고 크롤링 및 저장된 사용자 문서 자동 분석 연결은 기존과 같이 후속 범위입니다.

```powershell
cd C:\CarrerPulse_AI_Agent\ai
python -m unittest test_learning_analysis test_period_learning_plan -v

cd C:\CarrerPulse_AI_Agent\backend
.\gradlew.bat test

cd C:\CarrerPulse_AI_Agent\frontend
npm run build
npx eslint src/components/LearningPlanPanel.jsx src/pages/LearningPage.jsx src/api/learningApi.js
```

MySQL 통합 테스트는 별도로 `LEARNING_DB_TEST=true`와 `DB_USERNAME`, `DB_PASSWORD`를 설정하면 실행합니다.
일반 테스트 실행에서는 실DB 테스트 2개가 생략됩니다.
