"""CareerPulse AI - RAG/Agent 파이프라인 실행 예시.

실행: python main.py
API 키가 있으면 환경변수 GEMINI_API_KEY 를 설정한다.
"""
import os

from data.sample_data import SAMPLE_RESUME, SAMPLE_PORTFOLIO, JOB_REQUIREMENTS
from processor import split_sections, build_evidences
from embedding import Embedder, VectorStore
from judge import match_rate
from agent import build_graph

USER_ID = 1


def load_sample_store() -> VectorStore:
    """실제 서비스에서는 parser.extract_document() 결과를 넣는다."""
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
    store = load_sample_store()
    print(f"임베딩 방식: {store.embedder.mode} | 근거 {len(store.evidences)}건")
    for e in store.evidences:
        print(f"  {e.evidence_id} [{e.doc_type}/{e.section}] {e.source_location}")

    app, mode = build_graph()
    print(f"\nAgent 실행 방식: {mode}")

    state = {"trigger_type": "user_request",
             "run_key": "u1-job77-guide-20260920",
             "user_id": USER_ID, "job_posting_id": 77,
             "requirements": JOB_REQUIREMENTS, "store": store,
             "api_key": os.environ.get("GEMINI_API_KEY")}
    out = app.invoke(state)

    print("실행 계획:", out.get("plan"))
    if out.get("stopped_reason"):
        print("중단:", out["stopped_reason"])
        return
    if out.get("errors"):
        print("오류:", out["errors"])
        return

    print("\n[비교 결과]")
    comparisons = out["step_results"].get("경험비교", [])
    for c, req in zip(comparisons, JOB_REQUIREMENTS):
        print(f'  {c["requirement_id"]} {req["name"]:<18} {c["match_status"]:<8} '
              f'근거 {c["cited"]} ({c["analysis_reason"]})')

    m, t, rate, rec = match_rate(comparisons)
    print(f'\n[추천 판정] 일치 {m}/{t} = {rate:.0%} → {"추천" if rec else "추천 안 함"}')

    guide = out["step_results"].get("작성방향생성", {})
    print(f'\n[작성 방향] 상태: {guide.get("status")}')
    for line in guide.get("lines", []):
        print("  통과:", line)
    for line, why in out["step_results"].get("근거검증", {}).get("dropped", []):
        print("  제거:", line, "→", why)

    print("\n[중복 신호 재전달]")
    print("  ", app.invoke(state).get("stopped_reason"))


if __name__ == "__main__":
    main()
