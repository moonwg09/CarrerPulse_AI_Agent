# CareerPulse AI — RAG / Agent 모듈

담당 범위: **RAG-01~03, AGT-01~05** (연결 항목 PAR-06·07, CMP-02·05, GDE-08·09)

## 실행

```bash
python -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate
pip install -r requirements.txt
python main.py
```

API 키·선택 패키지가 없어도 동작합니다(모의 응답과 대체 구현). 실제 호출은 다음과 같이 설정합니다.

```bash
export GEMINI_API_KEY="..."        # Windows: set GEMINI_API_KEY=...
pip install sentence-transformers langgraph
```

## 폴더 구성

```
RAG_PROJECT/
├── data/sample_data.py        실습용 서류·공고 샘플
├── parser/                    PAR-01  PDF·DOCX 텍스트 추출 (+위치 정보)
│   ├── pdf_parser.py
│   └── docx_parser.py
├── processor/                 PAR-02~04, 06·07
│   ├── cleaner.py             공백·개행 정리
│   ├── chunker.py             항목 단위 분할 (글자 수로 자르지 않음)
│   └── metadata.py            Evidence — ERD EVIDENCE_SOURCES 대응
├── embedding/embedding.py     RAG-01  임베딩(bge-m3/대체), 벡터 저장·삭제
├── retriever/search.py        RAG-01  하이브리드 검색 + 동의·삭제 필터
├── prompt/templates.py        RAG-02  구역 분리 + 인젝션 방어 규칙
├── judge/
│   ├── llm_client.py          Gemini 호출 (키 없으면 모의 응답)
│   └── rules.py               CMP-02 상태 판정 규칙, REC-02 일치율
├── verify/verifier.py         GDE-09  인용 검사·근거 대조·재생성
├── agent/
│   ├── tools.py               AGT-02  Tool 정의와 트리거별 허용 목록
│   └── graph.py               AGT-01·03·04·05  LangGraph 흐름
└── main.py                    전체 실행 예시
```

## 설계에서 지킨 규칙

| SRS | 구현 위치 | 내용 |
|---|---|---|
| RAG-01 | `retriever/search.py` | `user_id` · `consented` · `deleted` 필터를 검색 전에 적용 |
| RAG-02 | `prompt/templates.py` | 요구 항목과 사용자 근거를 구역으로 분리, 근거 속 지시문 무시 |
| RAG-03 | `agent/tools.py` | 근거 0건이면 LLM을 호출하지 않고 '기록 부족' |
| CMP-02 | `judge/rules.py` | LLM은 사실만 출력, 상태는 `decide_status()`가 결정 |
| CMP-05 | `processor/metadata.py` | `source_location`으로 근거 출처 표시 |
| DOC-09 | `embedding/embedding.py` | `delete_by_document()`로 벡터까지 삭제 |
| GDE-09 | `verify/verifier.py` | 인용 없는 문장 제거, 근거 대조 실패 시 재생성 1회 후 보류 |
| AGT-01 | `agent/graph.py` | `run_key`로 중복 신호 차단 |
| AGT-02 | `agent/tools.py` | 트리거별 `ALLOWED_TOOLS` 밖의 Tool은 계획에서 제외 |
| AGT-03 | `agent/graph.py` | `node_precheck()` 통과 전에는 Tool 미실행 |
| AGT-04·05 | `agent/graph.py` | 실패 시 다음 Tool로 넘기지 않고 사유 기록 |

## 팀 확정이 필요한 값 (SRS 미정)

| 항목 | 현재 값 | 위치 |
|---|---|---|
| 검색 임계값 `min_score` | 0.15 | `retriever/search.py` |
| 검색 개수 `top_k` | 3 | `retriever/search.py` |
| 근거 대조 기준 `MIN_OVERLAP` | 0.2 | `verify/verifier.py` |
| 재생성 횟수 | 1회 | `verify/verifier.py` |
| 학력·자격증·경력 연수 일치율 반영 | 미구현 | `judge/rules.py` |

## 주의

- 기본 실행은 **대체 임베딩 + 모의 응답**입니다. 이 상태의 판정 결과를 품질 근거로 쓰지 마십시오.
- 대체 임베딩에서는 R2(DB 설계·쿼리 작성)의 "테이블 설계에 참여했습니다" 근거가 임계값에 걸려 제외됩니다.
  실제 모델을 붙인 뒤 정답 세트로 `min_score`를 조정해야 합니다.
- 벡터 저장소는 메모리 구현입니다. 운영에서는 Chroma 또는 pgvector로 교체하고,
  `DOCUMENT_CHUNKS.embedding_ref`에 벡터 ID를 저장하십시오.
