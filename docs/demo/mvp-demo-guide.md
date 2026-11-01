# CareerPulse MVP 시연 안내

## 1. 실행 전 확인

- 프로젝트 루트의 `.env`에 DB, Gemini, 메일 설정이 있어야 합니다.
- MySQL의 `careerpulse_mvp_dev`에 `docs/database/schema.sql`이 반영되어 있어야 합니다.
- 처음 Qwen 임베딩 모델을 내려받을 때는 인터넷 연결이 필요합니다.
- FastAPI, Spring Boot, React는 각각 별도 터미널에서 실행합니다.

## 2. 서버 실행

프로젝트 루트를 `D:\Lecture\CarrerPulse_AI_Agent\.tmp-integration-main`으로 두고 실행합니다.

FastAPI:

```powershell
conda activate careerpulse
Set-Location ai
python -m uvicorn main:app --reload --port 8000
```

Spring Boot:

```powershell
Set-Location backend
.\gradlew.bat bootRun
```

React:

```powershell
Set-Location frontend
npm run dev
```

브라우저에서 `http://127.0.0.1:5173`을 엽니다. FastAPI API 문서는
`http://127.0.0.1:8000/docs`에서 확인합니다.

## 3. 핵심 시연 순서

1. 회원가입 후 로그인합니다.
2. 관심 직무를 1개 이상 3개 이하로 선택합니다.
3. 문서 관리에서 이력서 또는 자기소개서를 업로드합니다.
4. 처리 상태가 `COMPLETED`인지 확인합니다.
5. 관심 직무 화면에서 지역과 수집 개수를 입력해 공고를 수집합니다.
6. 공고 상세에서 분석된 요구조건을 확인합니다.
7. 서류·공고 비교에서 공고를 선택해 분석합니다.
8. 분석 상세에서 전체 일치율, 요구조건별 판정, 인용 근거를 확인합니다.
9. 필요한 항목에는 경험 여부와 추가 설명을 입력해 재판정합니다.
10. 추천 공고에서 추천·성장 후보 분류를 확인합니다.
11. 준비 역량 분석에서 부족 역량을 확인하고 학습계획을 생성합니다.
12. 관심 공고 저장, 지원 상태 변경, 알림 메일을 확인합니다.

## 4. 시연 시 설명할 판정 기준

- `경험 있음`: 사용자의 수행 범위와 근거가 확인된 경우
- `일부만 있음`: 요구조건 일부만 근거로 확인된 경우
- `기록 부족`: 서류와 검색 근거만으로 판단할 수 없는 경우
- `경험 없음`: 사용자가 직접 경험 없음을 확인한 경우

RAG 검색 점수는 원문 근거를 찾기 위한 상대 점수이며 경험 충족률이 아닙니다.
전체 일치율은 요구조건별 최종 판정을 바탕으로 별도로 계산합니다.

## 5. 문제 발생 시 확인 순서

- FastAPI가 실행되지 않으면 `ai` 폴더에서 명령을 실행했는지 확인합니다.
- 문서가 `FAILED`이면 FastAPI 로그와 문서 화면의 처리 오류를 확인합니다.
- Spring이 실행되지 않으면 MySQL 연결, `.env`, Java 21 설정을 확인합니다.
- 화면에서 API 오류가 나면 Spring `8080`, FastAPI `8000`이 모두 실행 중인지 확인합니다.
- 학습계획이 생성되지 않으면 먼저 공고 비교를 완료하고 `기록 부족` 항목을 확인합니다.
