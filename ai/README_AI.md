# ai — AI 분석 서버

## 실행

```bash
cd ai
conda env create -f environment.yml    # 최초 1회
conda activate careerpulse
python -m uvicorn main:app --reload --port 8000
```

- API 문서: http://127.0.0.1:8000/docs
- 상태 확인: http://127.0.0.1:8000/analyze/health

실제 LLM을 쓰려면 환경변수를 설정합니다. 없으면 모의 응답으로 동작합니다.

```bash
set GEMINI_API_KEY=...        # Windows
export GEMINI_API_KEY=...     # macOS/Linux
```

## 구조

```
ai/
├── main.py                       FastAPI 앱 (analyze 라우터 등록)
├── routers/analyze.py            분석 API
├── schemas/analyze_schema.py     요청·응답 DTO
├── rag/
│   ├── processor/                항목 분할·근거 메타데이터 (PAR-02~04, 06·07)
│   ├── embedding/                임베딩·벡터 저장 (RAG-01, DOC-09)
│   ├── retriever/                하이브리드 검색 (RAG-01)
│   ├── prompt/                   프롬프트 구성·인젝션 방어 (RAG-02)
│   ├── judge/                    LLM 호출 / 상태 판정 규칙 (CMP-02, REC-02)
│   ├── verify/                   생성 결과 근거 검증 (GDE-09)
│   └── store.py                  색인 저장소
├── agent/                        Tool 허용 목록과 실행 흐름 (AGT-01~05)
├── examples/run_local.py         서버 없이 파이프라인 확인
└── docs/AI_API_연동규격.md        백엔드 연동 문서
```

## 확인

```bash
python -m examples.run_local
```

## 참고

- 서류 파일 파싱(PDF·DOCX)은 **Spring이 담당**합니다. 이 서버는 추출된 텍스트를 받습니다.
- 임계값 등 미정 항목은 `docs/AI_API_연동규격.md` 8절에 정리되어 있습니다.
