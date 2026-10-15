# 문서 색인, 검색, 삭제, 재색인 관련 전반적인 테스트

import numpy as np

from rag import store
from rag.retriever import search

DOCX_PATH = r"D:\portfolio\이력서\하이테크_마스터이력서_심지우.docx"


def check_alignment(s):
    vector_count = 0 if s.vectors is None else s.vectors.shape[0]

    assert len(s.chunks) == len(s.vector_chunk_ids) == vector_count
    assert len(set(s.vector_chunk_ids)) == vector_count

    if vector_count:
        expected = s.embedder.encode([
            s.get_chunk(chunk_id).text
            for chunk_id in s.vector_chunk_ids
        ])

        assert np.allclose(s.vectors, expected, atol=1e-5)

    print("VECTOR ALIGNMENT: PASS")


# 1. 최초 색인
store.index_document(
    1, 200, "portfolio",
    "Python FastAPI 기반 AI 추천 서비스를 개발했습니다.",
)

store.index_file(
    1, 100, "resume",
    DOCX_PATH, DOCX_PATH,
)

store.index_document(
    1, 201, "portfolio",
    "Spring Boot REST API 서버를 개발했습니다.",
)

s = store.get_store()

print("BEFORE DELETE:", len(s.evidences), len(s.chunks))
check_alignment(s)

# 2. DOCX 삭제
removed_count = store.delete_document(100)

print("REMOVED EVIDENCES:", removed_count)
print("AFTER DELETE:", len(s.evidences), len(s.chunks))

assert removed_count == 69
assert len(s.evidences) == 2
assert len(s.chunks) == 2

assert all(e.document_id != 100 for e in s.evidences)
assert all(c.document_id != 100 for c in s.chunks)

check_alignment(s)

# 3. 남아 있는 Legacy 문서 검색
for document_id, query in [
    (200, "FastAPI AI 추천 서비스 개발"),
    (201, "Spring Boot REST API 서버 개발"),
]:
    hits = search(
        s,
        query,
        user_id=1,
        top_k=5,
        min_score=0.0,
    )

    assert any(
        evidence.document_id == document_id
        for _, evidence in hits
    )

print("REMAINING DOCUMENT SEARCH: PASS")

# 4. DOCX 재색인
store.index_file(
    1, 100, "resume",
    DOCX_PATH, DOCX_PATH,
)

print("AFTER REINDEX:", len(s.evidences), len(s.chunks))

assert len(s.evidences) == 71
assert len(s.chunks) == 15

check_alignment(s)

# 5. 재색인된 DOCX 검색
hits = search(
    s,
    "채용 공고 경험 매칭 AI Agent",
    user_id=1,
    top_k=5,
    min_score=0.0,
)

assert any(
    evidence.document_id == 100
    for _, evidence in hits
)

print("REINDEX SEARCH: PASS")
print("DELETE / REINDEX TEST: PASS")