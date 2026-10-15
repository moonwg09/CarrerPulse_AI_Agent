import sys

from rag import store
from rag.retriever import search

if len(sys.argv) < 2:
    raise SystemExit("사용법: python test_pdf_index.py <PDF 파일 경로>")

pdf_path = sys.argv[1]

# 1. PDF 색인
result = store.index_file(
    user_id=1,
    document_id=300,
    doc_type="portfolio",
    source=pdf_path,
    filename=pdf_path,
)

s = store.get_store()

pdf_evidences = [
    e for e in s.evidences
    if e.document_id == 300
]

pdf_chunks = [
    c for c in s.chunks
    if c.document_id == 300
]

vector_count = 0 if s.vectors is None else s.vectors.shape[0]

print("EXTRACTION METHOD:", result["extraction_method"])
print("PAGE COUNT:", result["page_count"])
print("EVIDENCES:", len(pdf_evidences))
print("CHUNKS:", len(pdf_chunks))
print("VECTORS:", vector_count)

# 2. Evidence와 Chunk 연결 검증
assert pdf_evidences, "PDF에서 Evidence가 생성되지 않았습니다."
assert pdf_chunks, "PDF에서 RetrievalChunk가 생성되지 않았습니다."

for chunk in pdf_chunks:
    assert chunk.evidence_ids
    for evidence_id in chunk.evidence_ids:
        evidence = s.get_evidence(evidence_id)
        assert evidence is not None
        assert evidence.document_id == 300

assert len(s.vector_chunk_ids) == vector_count
print("PDF INDEX: PASS")

# 3. 검색 검증
query = "AI 모델을 활용한 서비스 개발 경험"

hits = search(
    s,
    query,
    user_id=1,
    top_k=5,
    min_score=0.0,
)

print("SEARCH HITS:", len(hits))

for score, evidence in hits[:5]:
    print(
        round(score, 4),
        evidence.evidence_id,
        evidence.section,
        evidence.evidence_text[:150],
    )

assert hits, "PDF 검색 결과가 없습니다."
assert all(e.document_id == 300 for _, e in hits)

print("PDF SEARCH: PASS")