# 저장된 각 벡터를 해당 Chunk 텍스트로 다시 임베딩한 결과와 비교하는 테스트

import numpy as np

from rag import store
from rag.retriever import search

DOCX_PATH = r"D:\portfolio\이력서\하이테크_마스터이력서_심지우.docx"

# 1. Legacy Text 색인
store.index_document(
    user_id=1,
    document_id=200,
    doc_type="portfolio",
    text="Python FastAPI 기반 AI 추천 서비스를 직접 개발했습니다.",
)

# 2. DOCX v2 색인
store.index_file(
    user_id=1,
    document_id=100,
    doc_type="resume",
    source=DOCX_PATH,
    filename=DOCX_PATH,
)

# 3. 또 다른 Legacy Text 색인
store.index_document(
    user_id=1,
    document_id=201,
    doc_type="portfolio",
    text="Spring Boot 기반 REST API 서버를 개발했습니다.",
)

s = store.get_store()

# 4. 저장 개수 확인
vector_count = 0 if s.vectors is None else s.vectors.shape[0]

print("EVIDENCES:", len(s.evidences))
print("CHUNKS:", len(s.chunks))
print("VECTOR_IDS:", len(s.vector_chunk_ids))
print("VECTORS:", vector_count)

assert len(s.chunks) == len(s.vector_chunk_ids) == vector_count
assert len(set(s.vector_chunk_ids)) == vector_count

# 5. Chunk ID와 실제 Vector 행 검증
expected_vectors = s.embedder.encode(
    [s.get_chunk(chunk_id).text for chunk_id in s.vector_chunk_ids]
)

assert np.allclose(
    s.vectors,
    expected_vectors,
    atol=1e-5,
), "Chunk ID와 Vector 행이 일치하지 않습니다."

print("VECTOR ALIGNMENT: PASS")

# 6. 문서별 검색 가능 여부 확인
queries = [
    ("Legacy AI", "FastAPI 기반 AI 추천 서비스", 200),
    ("DOCX CareerPulse", "채용 공고 경험 매칭 AI Agent", 100),
    ("Legacy Spring", "Spring Boot REST API 서버 개발", 201),
]

for label, query, expected_document_id in queries:
    hits = search(
        s,
        query,
        user_id=1,
        top_k=5,
        min_score=0.0,
    )

    matched = [
        (score, evidence)
        for score, evidence in hits
        if evidence.document_id == expected_document_id
    ]

    print(
        label,
        "MATCH:",
        len(matched),
        "TOP:",
        matched[0][1].evidence_id if matched else None,
    )

    assert matched, f"{label} 검색 실패"

print("MIXED INDEX TEST: PASS")