"""CareerPulse AI - RAG/Agent 파이프라인 실행 예시 (서버 없이 확인용).

실행: ai 폴더에서  python -m examples.run_local
실제 LLM을 쓰려면 환경변수 GEMINI_API_KEY 를 설정한다.
"""
import os

from examples.sample_data import SAMPLE_RESUME, SAMPLE_PORTFOLIO, JOB_REQUIREMENTS
from rag.processor import split_sections, build_evidences
from rag.embedding import Embedder, VectorStore
from rag.judge import match_rate
from agent import build_graph

USER_ID = 1


def load_sample_store() -> VectorStore:
    """샘플 서류를 항목으로 나누고 근거로 만들어 벡터 저장소에 넣는다."""
    # 실제 서비스에서는 parser.extract_document()의 결과를 여기에 넣는다.
    # 이력서와 포트폴리오의 document_id를 다르게 두어야 DOC-09 삭제를 서류 단위로 할 수 있다.
    resume_pages = [{"page": 1, "text": SAMPLE_RESUME}]
    portfolio_pages = [{"page": 1, "text": SAMPLE_PORTFOLIO}]

    evidences = (
        build_evidences(split_sections(resume_pages, "이력서"),
                        user_id=USER_ID, document_id=100, start=1)
        + build_evidences(split_sections(portfolio_pages, "포트폴리오"),
                          user_id=USER_ID, document_id=101, start=100)
    )

    store = VectorStore(Embedder())
    store.add(evidences)
    return store


def main() -> None:
    # 1) 근거 준비 — 어떤 항목이 어떤 출처에서 나왔는지 확인한다.
    store = load_sample_store()
    print(f"임베딩 방식: {store.embedder.mode} | 근거 {len(store.evidences)}건")
    for e in store.evidences:
        print(f"  {e.evidence_id} [{e.doc_type}/{e.section}] {e.source_location}")

    # 2) Agent 실행 — 사용자가 '작성 방향 보기'를 누른 상황을 가정한다.
    app, mode = build_graph()
    print(f"\nAgent 실행 방식: {mode}")

    state = {"trigger_type": "user_request",
             "run_key": "u1-job77-guide-20260920",
             "user_id": USER_ID, "job_posting_id": 77,
             "requirements": JOB_REQUIREMENTS, "store": store,
             "api_key": os.environ.get("GEMINI_API_KEY")}
    out = app.invoke(state)

    # 중단·실패는 결과를 보여 주지 않고 사유만 알린다(AGT-03·05).
    print("실행 계획:", out.get("plan"))
    if out.get("stopped_reason"):
        print("중단:", out["stopped_reason"])
        return
    if out.get("errors"):
        print("오류:", out["errors"])
        return

    # 3) 비교 결과와 추천 판정 (CMP-02, REC-02)
    print("\n[비교 결과]")
    comparisons = out["step_results"].get("경험비교", [])
    for c, req in zip(comparisons, JOB_REQUIREMENTS):
        print(f'  {c["requirement_id"]} {req["name"]:<18} {c["match_status"]:<8} '
              f'근거 {c["cited"]} ({c["analysis_reason"]})')

    m, t, rate, rec = match_rate(comparisons)
    print(f'\n[추천 판정] 일치 {m}/{t} = {rate:.0%} → {"추천" if rec else "추천 안 함"}')

    # 4) 작성 방향과 검증 결과 (GDE-08·09) — 제거된 문장과 사유까지 보여 준다.
    guide = out["step_results"].get("작성방향생성", {})
    print(f'\n[작성 방향] 상태: {guide.get("status")}')
    for line in guide.get("lines", []):
        print("  통과:", line)
    for line, why in out["step_results"].get("근거검증", {}).get("dropped", []):
        print("  제거:", line, "→", why)

    # 5) 같은 신호를 다시 보내 중복 실행이 막히는지 확인한다(AGT-01).
    print("\n[중복 신호 재전달]")
    print("  ", app.invoke(state).get("stopped_reason"))


if __name__ == "__main__":
    main()
