# Analysis 기능

## 기준

- 요청은 저장된 채용공고의 `jobId`만 받는다.
- 사용자 문서와 공고 내용은 로그인 사용자 및 DB 기준으로 서버가 조회한다.
- 별도 `analysis_results`, `analysis_items` 테이블을 만들지 않는다.
- `docs/database/schema.sql`의 `comparison_results`, `comparison_items`,
  `job_requirements`를 사용한다.
- 전체 일치율만 `comparison_results.match_rate`에 저장한다.
- 필수·우대·객관 조건 충족률과 평가 가능 비율은 비교 항목에서 계산한다.
- 같은 사용자와 공고를 다시 분석하면 기존 결과를 `PENDING`으로 되돌린 뒤
  최신 결과로 갱신한다.
- AI의 `NO_EXPERIENCE` 응답은 바로 확정하지 않고 `INSUFFICIENT`로 저장한다.
  사용자가 확인 API를 호출한 경우에만 `NO_EXPERIENCE`로 변경한다.

## API

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/v1/analysis/compare` | 저장된 공고와 내 문서 비교 |
| GET | `/api/v1/analysis` | 내 분석 결과 목록 |
| GET | `/api/v1/analysis/{analysisId}` | 요구조건별 분석 상세 |
| PATCH | `/api/v1/analysis/{analysisId}/items/{itemId}/no-experience` | 경험 없음 사용자 확인 |
| GET | `/api/v1/analysis/recommendations` | 추천·성장 후보·제외 조회 |
| GET | `/api/v1/analysis/ai-health` | AI 서버 연결 확인 |

비교 요청 예시:

```json
{
  "jobId": 1
}
```

## 내부 AI 계약

Spring은 로그인 사용자 ID, 저장된 공고 본문, 처리 완료된 문서 ID만 FastAPI에
전달한다. 클라이언트가 `userAnalysis`나 `embeddedChunks`를 직접 입력하지 않는다.

```text
POST /api/v1/career/analyze-indexed-job
```

이 경로는 `feature/ai-rag-v2-integration`에서 사용자별 RAG 저장소와 연결해야 한다.
FastAPI 구현이 합쳐지기 전에는 비교 실행이 완료되지 않으며, Spring은 해당 실패를
`comparison_results.analysis_status=FAILED`로 기록한다.

## 추천 규칙

- 전체 일치율 50% 이상: `RECOMMENDED`
- 30% 이상 50% 미만: `GROWTH_CANDIDATE`
- 30% 미만: `EXCLUDED`
- 추천은 최대 10개, 성장 후보는 최대 5개
- 마감 공고는 제외
- 전체 일치율 내림차순, 동률이면 마감일이 가까운 순

## 미완료 연동

- FastAPI `analyze-indexed-job` 구현 및 RAG v2 연결
- 대표 `evidence_chunk_id`와 Chroma Evidence 식별자 연결
- 사용자 추가 근거 제출 기능
- Repository 통합 테스트와 실제 DB 검증
