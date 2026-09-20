"""간단한 자체 점검: python test_pipeline.py"""
from data.sample_data import JOB_REQUIREMENTS
from judge import decide_status, match_rate, STATUS_HAVE, STATUS_UNKNOWN, STATUS_NONE
from retriever import search, build_query
from verify import verify_guide
from agent import build_graph
from main import load_sample_store, USER_ID

def check(name, cond):
    print(("  통과  " if cond else "  실패  ") + name)
    return cond

def run():
    ok = True
    store = load_sample_store()

    # RAG-01: 삭제한 서류는 검색되지 않는다 (DOC-09)
    removed = store.delete_by_document(101)
    hits = search(store, build_query(JOB_REQUIREMENTS[0]), user_id=USER_ID)
    ok &= check("삭제한 서류 제외", all(e.document_id != 101 for _, e in hits))

    # RAG-01: 다른 사용자의 자료는 검색되지 않는다
    ok &= check("사용자 격리", search(store, "API", user_id=999) == [])

    # RAG-03 / CMP-02
    ok &= check("근거 없음 → 기록 부족", decide_status({}) == STATUS_UNKNOWN)
    ok &= check("사용자 확인 → 경험 없음",
                decide_status({"has_evidence": True, "scope_specified": True},
                              user_confirmed_no=True) == STATUS_NONE)

    # REC-02: 경계값 50%는 추천에 포함
    half = [{"match_status": STATUS_HAVE}] * 3 + [{"match_status": STATUS_UNKNOWN}] * 3
    ok &= check("일치율 50% 추천 포함", match_rate(half)[3] is True)

    # GDE-09
    class E:  # 최소 더미
        evidence_id, evidence_text = "EV-0002", "상품 API 엔드포인트 4종을 직접 구현"
    kept, dropped = verify_guide("근거 없는 문장입니다.\n존재하지 않는 근거입니다. (EV-9999)",
                                 [(0.9, E())])
    ok &= check("인용 없음·허위 인용 제거", kept == [] and len(dropped) == 2)

    # AGT-03
    app, _ = build_graph()
    s = app.invoke({"trigger_type": "user_request", "run_key": "t1", "user_id": 1,
                    "requirements": JOB_REQUIREMENTS, "store": store})
    ok &= check("필수 입력값 누락 시 중단", bool(s.get("stopped_reason")))

    # AGT-02
    s2 = app.invoke({"trigger_type": "schedule", "run_key": "t2", "user_id": 1,
                     "requirements": JOB_REQUIREMENTS, "store": store})
    ok &= check("schedule 트리거는 작성방향생성 미포함",
                "작성방향생성" not in s2.get("plan", []))

    print("\n결과:", "모두 통과" if ok else "실패 항목 있음")

if __name__ == "__main__":
    run()
