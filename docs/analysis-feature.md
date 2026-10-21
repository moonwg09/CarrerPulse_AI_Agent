# analysis 기능 (서류-공고 비교 / 추천 조회)

브랜치: `feature/analysis` (origin/main 기준)

## 기능 목록과 구현 위치

| 기능 | 구현 |
| --- | --- |
| 서류와 공고비교 | `AnalysisService.compare` |
| fast api 호출 | `AiAnalysisClient` |
| 결과저장 | `AnalysisResult`, `AnalysisItem`, `AnalysisResultRepository` |
| 추천조회 | `AnalysisRecommendationService` |

## 선행 작업 (DB)

`spring.jpa.hibernate.ddl-auto=validate` 이므로 테이블은 자동으로
생성되지 않는다. 서버를 띄우기 전에 아래 스크립트를
`careerpulse_mvp_dev` 스키마에 한 번 실행한다.

```
backend/src/main/resources/db/analysis_schema.sql
```

생성 테이블

- `analysis_results` : 공고 1건에 대한 비교 결과(일치율, 추천 구분, AI 응답 원본)
- `analysis_items` : 요구사항 1건에 대한 판정과 근거 원문

`(user_id, job_key)` 유니크 제약이 있어 같은 공고를 다시 비교하면
기존 결과를 갱신한다(중복 저장 없음).

## API

로그인 필요. POST 는 `/api/v1/auth/csrf` 로 받은 CSRF 토큰을 헤더에 넣는다.

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| POST | `/api/v1/analysis/compare` | 비교 실행 + 저장 |
| GET | `/api/v1/analysis` | 내 분석 결과 목록 |
| GET | `/api/v1/analysis/{analysisId}` | 상세 (요구사항별 판정 + 근거) |
| GET | `/api/v1/analysis/{analysisId}/raw` | AI 응답 원본 JSON |
| GET | `/api/v1/analysis/recommendations` | 추천조회 (`status`, `limit`) |
| GET | `/api/v1/analysis/ai-health` | AI 서버 연결 확인 |

### POST /api/v1/analysis/compare 요청

```json
{
  "jobKey": "job-001",
  "jobTitle": "백엔드 개발자",
  "companyName": "A회사",
  "closeDate": "2026-11-30",
  "jobText": "공고 전문 ...",
  "userAnalysis": { "basicProfile": { }, "skills": [] },
  "embeddedChunks": []
}
```

`userAnalysis` 는 FastAPI `POST /api/v1/documents/analyze` 응답,
`embeddedChunks` 는 RAG 임베딩 결과를 그대로 넣는다. 백엔드는 이 두 값을
해석하지 않고 통과시킨다(`JsonNode`). 문서 도메인이 main 에 합쳐지면
이 자리를 `documentId` 조회로 바꾸면 된다.

## AI 서버 호출

`AiAnalysisClient` 가 `POST /api/v1/career/analyze-job` 을 호출한다.
주소는 `application.properties` 의 `ai.server.url` 을 그대로 쓴다.

Gemini 호출이 포함되어 응답이 느릴 수 있으므로 타임아웃을 길게 뒀고,
필요하면 아래 속성으로 조정한다.

```
ai.server.connect-timeout-seconds=10
ai.server.read-timeout-seconds=300
```

AI 서버가 꺼져 있으면 503, FastAPI 가 429/503 을 주면 그대로 전달,
그 밖의 오류는 502 로 바꿔 내려준다.

## 판정 상태

AI 가 돌려준 문자열을 그대로 신뢰하지 않고 `MatchStatus.from` 을 통과시켜
4가지 중 하나로 정한다. 알 수 없는 값은 `INSUFFICIENT_EVIDENCE`(기록 부족)로
내린다. 근거 없이 '경험 있음'으로 올라가는 경로를 만들지 않기 위한 것.

| 값 | 화면 표기 |
| --- | --- |
| `EXPERIENCED` | 경험 있음 |
| `PARTIAL` | 일부만 있음 |
| `INSUFFICIENT_EVIDENCE` | 기록 부족 |
| `NO_EXPERIENCE` | 경험 없음 |

근거 원문(`userEvidence`, `jobEvidence`)은 가공하지 않고 JSON 그대로
저장하고, 상세 화면에서 인용 형태로 보여준다.

## 추천조회 규칙

`ai/services/recommendation_service.py` 와 같은 규칙을 저장된 결과에
다시 적용한다.

- 마감일이 지난 공고 제외
- 추천 공고: 전체 일치율 내림차순 → 마감일 가까운 순, 기본 10개
- 성장 후보 공고: 전체 일치율 → 필수 충족률 → 평가 가능 비율 내림차순
  → 마감일 가까운 순, 기본 5개

## 프론트엔드

| 경로 | 화면 |
| --- | --- |
| `/analysis` | 비교 실행 + 저장된 결과 목록 |
| `/analysis/:analysisId` | 요구사항별 판정과 근거 원문 |
| `/recommendations` | 추천 공고 / 성장 후보 공고 조회 |

`userAnalysis`, `embeddedChunks` 는 JSON 파일 업로드 또는 붙여넣기로
입력한다. 테스트용 파일은 `ai/test_files/user_analysis.json` 을 쓰면 된다.

## 남은 작업

- 문서 도메인(`feature/document`)이 main 에 합쳐지면 `userAnalysis` 입력을
  `documentId` 기반 조회로 교체
- `job_postings` 테이블이 생기면 `job_key` 문자열을 FK 로 교체
- 비교 실행이 오래 걸리므로 비동기 처리(큐 또는 Agent 스케줄) 검토
