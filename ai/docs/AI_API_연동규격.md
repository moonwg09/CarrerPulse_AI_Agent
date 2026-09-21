# AI 서버 API 연동 규격 (v0.1)

- **작성:** RAG·Agent 담당 · 2026-09-21
- **대상:** Spring Boot 백엔드 담당
- **기본 주소:** `ai.server.url` (기본 `http://127.0.0.1:8000`)
- **API 문서:** 서버 실행 후 http://127.0.0.1:8000/docs

## 0. 역할 분담 (A안 — 팀 확인 필요)

| 단계 | 담당 |
|---|---|
| 파일 업로드·저장·권한 확인 | **Spring** |
| PDF·DOCX 텍스트 추출 (PAR-01) | **Spring** |
| 추출 텍스트 전달 → 항목 분할·색인 | **FastAPI** |
| 근거 검색·상태 판정·작성 방향·검증 | **FastAPI** |
| 결과 저장(EXPERIENCE_COMPARISONS 등) | **Spring** |

FastAPI는 인증을 처리하지 않습니다. **Spring이 권한과 입력값을 검증한 뒤 호출**합니다.

## 1. 서류 색인 — `POST /analyze/documents`

서류를 등록하거나 교체할 때 호출합니다. 같은 `document_id`로 다시 보내면 이전 색인을 지우고 새로 만듭니다(PAR-08).

**요청**

```json
{
  "user_id": 1,
  "document_id": 100,
  "doc_type": "이력서",
  "text": "[기술]\nJava, Spring Boot ...",
  "consented": true
}
```

- `doc_type`: `이력서` | `자기소개서` | `포트폴리오`
- `consented`: 자료 이용 동의 여부(DOC-01). `false`면 검색 대상에서 제외됩니다.

**응답 200**

```json
{
  "document_id": 100,
  "evidence_ids": ["EV-0001", "EV-0002", "EV-0003"],
  "evidence_count": 3,
  "embedding_mode": "fallback"
}
```

## 2. 서류 삭제 — `DELETE /analyze/documents/{document_id}`

사용자가 삭제를 요청하면 호출합니다. 근거와 벡터를 함께 제거합니다(DOC-09).

**응답 200**

```json
{ "document_id": 100, "deleted_evidence_count": 3 }
```

## 3. 경험 비교 — `POST /analyze/compare`

공고 요구 항목과 사용자 서류를 비교해 상태를 판정합니다(CMP-02, REC-02).

**요청**

```json
{
  "user_id": 1,
  "job_posting_id": 77,
  "requirements": [
    {
      "requirement_id": "R1",
      "type": "필수",
      "name": "웹 서비스 API 개발",
      "text": "웹 서비스 API 개발",
      "needed_experience": "API 엔드포인트를 직접 설계하고 구현한 경험"
    }
  ]
}
```

**응답 200**

```json
{
  "user_id": 1,
  "job_posting_id": 77,
  "results": [
    {
      "requirement_id": "R1",
      "match_status": "경험 있음",
      "match_score": 0.425,
      "analysis_reason": "판단 근거 문장",
      "missing_part": "",
      "evidences": [
        {
          "evidence_id": "EV-0002",
          "doc_type": "이력서",
          "section": "프로젝트",
          "source_location": "p.1",
          "excerpt": "상품 API 개발 담당. 엔드포인트 4종을 직접 구현했습니다.",
          "score": 0.425
        }
      ]
    }
  ],
  "matched_count": 1,
  "total_count": 3,
  "match_rate": 0.333,
  "recommended": false
}
```

**판정 규칙**

- `match_status`는 `경험 있음` / `일부만 있음` / `기록 부족` / `경험 없음` 네 가지뿐입니다.
- `경험 없음`은 **사용자가 직접 확인한 경우에만** 부여됩니다. 근거를 못 찾은 경우는 `기록 부족`입니다(RAG-03).
- `matched_count`는 `경험 있음`만 셉니다. `recommended`는 일치율 50% **이상**(경계값 포함)일 때 true입니다.
- `match_score`는 검색 점수이며 **합격 가능성이 아닙니다.** 화면에 점수로 표시하지 마세요.

## 4. 작성 방향 — `POST /analyze/guide`

사용자가 [작성 방향 보기]를 누를 때 호출합니다(GDE-08·09, AGT-01~05).

**요청**

```json
{
  "user_id": 1,
  "job_posting_id": 77,
  "requirements": [ ... 3번과 동일 ... ],
  "trigger_type": "user_request",
  "run_key": "u1-job77-guide-20260921"
}
```

- `run_key`: 중복 실행 방지 키(AGT-01). 생략하면 서버가 `u{user}-job{posting}-guide-{날짜}`로 만듭니다.
- `trigger_type`: `user_request` | `schedule` | `condition` — 신호별로 호출되는 Tool이 다릅니다(AGT-02).

**응답 200**

```json
{
  "user_id": 1,
  "job_posting_id": 77,
  "status": "제공",
  "lines": [
    { "text": "상품 API 부분의 수행 범위를 덧붙이십시오. (EV-0002)", "cited": ["EV-0002"] }
  ],
  "dropped": [
    { "text": "Docker 배포 경험을 강조하십시오.", "reason": "인용 없음" }
  ],
  "plan": ["공고요구사항분석", "근거검색", "경험비교", "작성방향생성", "근거검증"]
}
```

- `status`: `제공` | `보류` | `대상 없음` | `중단`
  - `보류`: 생성 결과가 근거 검증을 통과하지 못함 → 사용자에게 안내만 표시(GDE-09)
  - `중단`: 중복 신호이거나 실행 조건 미충족(AGT-01·03). `reason`에 사유가 담깁니다
- `lines`만 화면에 표시하세요. `dropped`는 점검용입니다.

## 5. 상태 확인 — `GET /analyze/health`

```json
{ "status": "ok", "embedding_mode": "fallback", "agent_mode": "langgraph", "gemini": "mock" }
```

`gemini`가 `mock`이면 API 키가 설정되지 않아 모의 응답이 나오는 상태입니다.

## 6. 오류 응답

| 코드 | 상황 | 처리 |
|---|---|---|
| 409 `NO_DOCUMENT` | 분석할 서류가 없음(DOC-12) | 사용자에게 서류 등록 안내 |
| 422 | 요청 형식 오류 | 필드 확인 |
| 502 `TOOL_FAILED` | 분석 단계 실패(AGT-05) | 실패를 정상 결과로 저장하지 말 것 |

```json
{ "detail": { "code": "NO_DOCUMENT", "message": "분석에 사용할 서류가 없습니다..." } }
```

## 7. Spring 호출 예시

```java
@Service
public class AiAnalyzeClient {

    private final RestClient restClient;

    public AiAnalyzeClient(@Value("${ai.server.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public CompareResponse compare(CompareRequest request) {
        return restClient.post()
                .uri("/analyze/compare")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(CompareResponse.class);
    }
}
```

## 8. 아직 정하지 못한 값

| 항목 | 현재 값 | 위치 |
|---|---|---|
| 검색 임계값 `min_score` | 0.15 | `rag/retriever/search.py` |
| 검색 개수 `top_k` | 3 | `rag/retriever/search.py` |
| 근거 대조 기준 | 0.2 | `rag/verify/verifier.py` |
| 재생성 횟수 | 1회 | `rag/verify/verifier.py` |
| 학력·자격증·경력 연수 일치율 반영 | 미구현 | `rag/judge/rules.py` |
| 벡터 저장소 | 메모리(재시작 시 초기화) | `rag/store.py` |

**메모리 저장소 주의:** 현재는 FastAPI를 재시작하면 색인이 사라집니다. 시연 전에는 서류를 다시 등록하거나, Chroma로 교체해야 합니다.
