# PLN-01~03 구현 및 자체 데이터 검증

## 실행

1. 기존 MySQL·Spring Boot·React 실행 환경을 사용합니다.
2. Gemini 키 없이 자체 데이터 분석만 실행하려면 PowerShell에서:

```powershell
cd C:\CarrerPulse_AI_Agent\ai
conda activate careerpulse
python -m uvicorn learning_main:app --host 127.0.0.1 --port 8000
```

기존 전체 AI 서버 `main:app`에도 같은 분석 라우터가 등록되어 있습니다.
두 진입점을 동시에 같은 포트로 실행하지 않습니다. 전체 서버는 기존 Gemini/RAG 설정이 필요합니다.

3. Spring Boot의 `AI_SERVER_URL`은 기본값 `http://127.0.0.1:8000`을 사용합니다.
4. 프런트엔드와 백엔드를 재시작한 뒤 로그인합니다.
5. 관심 직무 화면에서 `BACKEND`를 등록합니다. 이미 있으면 추가하지 않습니다.
6. 메뉴의 **준비 역량 분석** 또는 `http://localhost:5173/learning`에 접속합니다.
7. **자체 가상 공고 → 가상 사용자 예시 불러오기 → PLN-01~03 분석하기**를 선택합니다.

이 작업은 DB에 테스트 공고를 삽입하거나 기존 사용자 경험을 저장하지 않습니다.
가상 사용자 예시는 본인의 경험으로 저장되지 않으며 입력창에서 수정할 수 있습니다.

## SRS 적용 규칙

| 항목 | 구현 |
| --- | --- |
| PLN-01 | 관심 직무, OPEN 상태, 마감일이 없거나 기준일 이후(당일 포함)인 공고만 선택. 기간 하한 없이 게시일·수집시각·ID 내림차순 최대 10개. 10개 미만이면 실제 확보 수만 분석. 0개이면 안내 반환 |
| 빈도 단위 | 기술(SKILL), 자격증(CERTIFICATE), 수행 경험(EXPERIENCE). 한 공고에서 같은 종류·이름은 한 번만 계산하며 필수·우대 중복은 필수로 계산 |
| PLN-02 | 충분한 경험·보유는 제외. 일부 경험은 명시한 부족 세부 항목만 준비 대상으로 사용. 미기록·UNKNOWN·세부 항목 미확인은 확인/서류 보완으로 구분 |
| PLN-03 | 실제 준비 대상을 전체 등장 공고 수 내림차순으로 정렬. 자격증·프로젝트·기술 학습·서류 보완 분류 |
| 동률 | SRS에서 별도 확정 대상이므로 추가 가중치를 만들지 않음. 동빈도는 동순위(1, 1, 3). 이름·종류순은 화면 표시 안정성만을 위한 기준 |

사용자 상태는 현 단계에서 **사용자가 직접 확인한 입력**입니다. 저장된 이력서나 자격증을 자동으로 읽은 결과가 아닙니다.
EXPERIENCED/PARTIAL/NO_EXPERIENCE에는 근거를 입력해야 합니다. UNKNOWN은 근거 없이도 사용할 수 있습니다.
일부 경험의 세부 항목이 비어 있으면 역량 전체를 부족하다고 처리하지 않습니다.
이름은 앞뒤/연속 공백 및 영문 대소문자를 정리해 비교합니다. 동의어·유사한 경험 문장까지 자동으로 같다고 추론하지 않습니다.

## 자체 데이터의 예상 결과

`backend/src/main/resources/learning/sample-jobs.json`은 BACKEND 모집 중 공고 12건과
검증용 3건(종료, 마감, 다른 직무)을 포함합니다. 게시일·마감일은 실행일 기준으로 계산합니다.
BACKEND만 관심 직무로 등록한 경우 대상 ID는 **1~10**입니다.

| 역량 | 등장 공고 수 | 필수 | 우대 | 가상 사용자 결과 |
| --- | ---: | ---: | ---: | --- |
| Java | 10 | 10 | 0 | 충분한 경험으로 제외 |
| SQL | 5 | 5 | 0 | 일부 경험: 실행 계획 분석만 준비 |
| 정보처리기사 | 5 | 0 | 5 | 미보유 확인: 자격증 준비 |
| API 개발 | 3 | 3 | 0 | 경험 없음 확인: 프로젝트 준비 |
| Docker | 2 | 0 | 2 | 기록 없음: 확인 필요·서류 보완 |

SQL과 정보처리기사는 동순위 1, API 개발은 3순위입니다.
Java는 한 공고의 필수·우대에 중복 등장하지만 두 번 집계하지 않습니다.
경험 입력을 비우면 모든 역량은 확인 필요이고, 실제 준비 대상은 비어 있습니다.

## API와 역할 분리

### 로그인 웹 API

`POST /api/v1/learning/analysis/me`

기존 로그인 세션 및 CSRF 토큰이 필요합니다. 프런트엔드 `learningApi.js`가 이를 처리합니다.

```json
{
  "source": "TEST",
  "assessments": [
    {
      "name": "SQL",
      "type": "SKILL",
      "status": "PARTIAL",
      "evidence": "기본 조회 경험은 있지만 실행 계획 분석 경험은 없음",
      "missingDetails": ["실행 계획 분석"]
    }
  ]
}
```

`source`는 TEST 또는 DATABASE입니다. 사용자 식별자는 로그인 정보에서,
관심 직무는 UserService에서, 기준일은 서버의 Asia/Seoul 날짜에서 가져옵니다.
공고·사용자 ID·분석 기준일을 브라우저에서 바꿔서 다른 사용자 데이터에 접근하는 경로는 없습니다.
DB 조회는 기존 Repository의 바인딩 쿼리를 사용합니다.

### Python 내부 분석 API

`POST /api/v1/learning/analyze`

`jobs`, `interests`, `assessments`, `referenceDate`를 받습니다.
Python 진입점은 내부 개발 서비스이므로 로컬호스트에 바인딩합니다.
응답은 다음 필드로 구분합니다.

- `referenceDate`, `targetJobCount`, `targetJobIds`: PLN-01 분석 범위
- `frequency`: 전체 요구 역량과 공고별 출처, 필수/우대/전체 빈도
- `preparation`: 학습/준비 대상, 부족 세부 항목, 유형, 근거, 순위
- `needsConfirmation`: 확인 필요·서류 보완 (학습 부족으로 확정하지 않음)
- `excluded`: 충분한 경험/보유로 제외된 항목과 근거

FastAPI는 잘못된 상태값, 중복 공고 ID, 중복 사용자 상태, 필드 오타를 422로 거절합니다.
Spring은 Python 연결 실패 503, 처리 실패 502, 입력 검증 실패 422를 구분합니다.

## DB 데이터와 향후 크롤링 연결

현재 `job_postings.description`이 단순 안내문이면 **DATABASE 모드에서는 422 안내**를 반환합니다.
비구조화 본문을 요구 역량 0개로 간주해서 잘못된 성공을 반환하지 않습니다.
현재 지원하는 DB description 계약은 다음 JSON입니다.

```json
{
  "requirements": [
    { "name": "Java", "type": "SKILL", "category": "REQUIRED" },
    { "name": "API 개발", "type": "EXPERIENCE", "category": "REQUIRED" },
    { "name": "정보처리기사", "type": "CERTIFICATE", "category": "PREFERRED" }
  ]
}
```

크롤러 자체는 이번 범위에 포함하지 않습니다. 이후에는 **수집 → 원문 보존 → 요구 조건 구조화 → 동일 분석기**로 연결합니다.
기존 `JobAnalysisResult`를 그대로 위 계약으로 사용할 수는 없습니다. 어댑터에서 기술 목록·자격증 항목·수행 경험을
개별 Requirement로 변환하고, 모호한 조건을 임의로 필수로 바꾸지 않아야 합니다.
실제 수집 단계에서는 원문은 별도 보존하고 구조화 결과를 별도 컬럼/테이블로 저장하는 방식을 팀과 결정한 뒤,
컨트롤러의 description 읽기 부분만 그 저장소를 읽도록 교체하면 됩니다.
여러 기술이 결합된 경험은 필요한 세부 조건으로 나누어야 비교 시 일부 경험을 정확히 구분할 수 있습니다.

`jobCode == job_category`는 현재의 연결 기준입니다. 실제 수집 직무 분류가 다르면 수집 어댑터에서 매핑해야 합니다.
사용자 문서 자동 분석 결과도 비교 근거와 함께 Assessment 계약으로 연결할 수 있으며, 근거 없음은 UNKNOWN으로 전달합니다.
현재 분류는 수동 확인 데이터를 사용하므로 문서 의미 분석 정확도를 검증한 것으로 간주하지 않습니다.

## 검증 명령

```powershell
cd C:\CarrerPulse_AI_Agent\ai
python -m unittest test_learning_analysis -v

cd C:\CarrerPulse_AI_Agent\backend
.\gradlew.bat test

cd C:\CarrerPulse_AI_Agent\frontend
npm run build
npx eslint src/pages/LearningPage.jsx src/api/learningApi.js src/App.jsx src/components/NavBar.jsx
```

Python 11개: 공고 선정/10개 미만/마감 당일/0개/공고별 중복 제거/근거 미기록/
일부 경험 세부 항목/동순위/유형 분류/입력 오류/실제 FastAPI 요청 검증.
Java 5개: 본인 관심 직무 전달, TEST에서 DB 공고 미조회, DB 구조화 공고 전달,
비구조화 본문 거절, 익명·잘못된 출처 거절, Python 검증 오류 전달을 검사합니다.
프런트엔드는 빌드와 변경 파일 린트를 검사했습니다. 실제 MySQL 로그인 세션을 사용하는 브라우저 통합 검증은 별도입니다.

PLN-04 기간 기반 계획 생성과 PLN-05 계획 조회/저장이 후속 구현되었습니다.
실행 방법과 검증 범위는 [PLN-04·05 안내서](learning-pln-04-05.md)를 참고하세요.
외부 크롤링과 사용자 문서 자동 연결은 후속 범위입니다.
