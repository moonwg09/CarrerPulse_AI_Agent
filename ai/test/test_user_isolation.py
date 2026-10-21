import hashlib

import numpy as np

from rag.embedding.embedding import VectorStore
from rag.processor.metadata import Evidence
from rag.processor.retrieval_chunk import RetrievalChunk
from rag.retriever.search import search


class DeterministicEmbedder:
    """외부 모델 없이 저장·검색 격리만 검증하는 테스트 임베더."""

    def encode(self, texts):
        vectors = np.zeros((len(texts), 16), dtype="float32")
        for row, text in enumerate(texts):
            for token in text.lower().split():
                index = int(hashlib.md5(token.encode()).hexdigest(), 16) % 16
                vectors[row, index] += 1.0
            vectors[row] /= np.linalg.norm(vectors[row]) or 1.0
        return vectors


def _evidence(user_id, document_id, text):
    return Evidence(
        evidence_id="EV-0001",
        user_id=user_id,
        document_id=document_id,
        doc_type="resume",
        section="project",
        source_type="document",
        source_location="p.1",
        evidence_text=text,
    )


def _chunk(user_id, document_id, text):
    return RetrievalChunk(
        chunk_id="CH-0001",
        user_id=user_id,
        document_id=document_id,
        doc_type="resume",
        semantic_unit_id=f"UNIT-{user_id}",
        category="PROJECT",
        section="project",
        title="프로젝트",
        text=text,
        evidence_ids=["EV-0001"],
    )


def test_same_display_ids_are_isolated_by_user():
    vector_store = VectorStore(DeterministicEmbedder())

    user1_evidence = _evidence(1, 100, "Python FastAPI API 개발")
    user2_evidence = _evidence(2, 100, "Java Spring 서버 개발")

    vector_store.add_document(
        [user1_evidence],
        [_chunk(1, 100, user1_evidence.evidence_text)],
    )
    vector_store.add_document(
        [user2_evidence],
        [_chunk(2, 100, user2_evidence.evidence_text)],
    )

    assert len(vector_store.evidences) == 2
    assert len(vector_store.chunks) == 2
    assert len(vector_store.vector_chunk_keys) == 2
    assert vector_store.get_evidence("EV-0001", user_id=1).evidence_text.startswith("Python")
    assert vector_store.get_evidence("EV-0001", user_id=2).evidence_text.startswith("Java")

    user1_hits = search(
        vector_store,
        "Python FastAPI",
        user_id=1,
        top_k=3,
        min_score=0.0,
    )
    assert user1_hits
    assert all(evidence.user_id == 1 for _, evidence in user1_hits)

    removed = vector_store.delete_by_document(user_id=1, document_id=100)

    assert removed == 1
    assert vector_store.get_evidence("EV-0001", user_id=1) is None
    assert vector_store.get_evidence("EV-0001", user_id=2) is user2_evidence
    assert len(vector_store.vector_chunk_keys) == 1


if __name__ == "__main__":
    test_same_display_ids_are_isolated_by_user()
    print("USER ISOLATION TEST: PASS")
