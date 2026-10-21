from pathlib import Path

from rag import store
from rag.embedding.embedding import VectorStore
from rag.retriever.search import search
from test.test_user_isolation import DeterministicEmbedder


def test_uploaded_docx_is_indexed_and_searchable():
    fixture = Path(__file__).parents[1] / "test_files" / "test_resume_new.docx"
    assert fixture.exists()

    embedder = DeterministicEmbedder()
    store._embedder = embedder
    store._store = VectorStore(embedder)
    store._next_index.clear()
    store._next_chunk_index.clear()

    result = store.index_file(
        user_id=11,
        document_id=501,
        doc_type="resume",
        source=str(fixture),
        filename=fixture.name,
        consented=True,
        size_bytes=fixture.stat().st_size,
    )

    assert result["evidence_ids"]
    assert store.count_evidences(user_id=11) == len(result["evidence_ids"])

    hits = search(
        store.get_store(),
        query="프로젝트 개발 경험",
        user_id=11,
        top_k=3,
        min_score=0.0,
    )
    assert hits
    assert all(evidence.user_id == 11 for _, evidence in hits)
    assert all(evidence.document_id == 501 for _, evidence in hits)


if __name__ == "__main__":
    test_uploaded_docx_is_indexed_and_searchable()
    print("DOCUMENT RAG INDEX TEST: PASS")
