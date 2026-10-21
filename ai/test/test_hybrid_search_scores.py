from math import sqrt

import numpy as np

from rag.embedding.embedding import VectorStore
from rag.processor.metadata import Evidence
from rag.processor.retrieval_chunk import RetrievalChunk
from rag.retriever.search import _hybrid_search_chunks, search


class ControlledEmbedder:
    """검색 점수 경계 조건을 재현하는 테스트 임베더."""

    def encode_documents(self, texts):
        vectors = []
        for text in texts:
            similarity = 0.7 if "relevant" in text else 0.2
            vectors.append(
                [similarity, sqrt(1.0 - similarity ** 2)]
            )
        return np.asarray(vectors, dtype="float32")

    def encode_queries(self, texts):
        return np.asarray([[1.0, 0.0] for _ in texts], dtype="float32")

    def encode(self, texts):
        return self.encode_documents(texts)


def _add_document(store, index, text):
    evidence_id = f"EV-{index:04d}"
    chunk_id = f"CH-{index:04d}"
    evidence = Evidence(
        evidence_id=evidence_id,
        user_id=1,
        document_id=index,
        doc_type="resume",
        section="project",
        source_type="document",
        source_location="p.1",
        evidence_text=text,
    )
    chunk = RetrievalChunk(
        chunk_id=chunk_id,
        user_id=1,
        document_id=index,
        doc_type="resume",
        semantic_unit_id=f"UNIT-{index}",
        category="PROJECT",
        section="project",
        title="프로젝트",
        text=text,
        evidence_ids=[evidence_id],
    )
    store.add_document([evidence], [chunk])


def test_equal_irrelevant_scores_do_not_become_one():
    store = VectorStore(ControlledEmbedder())
    _add_document(store, 1, "unrelated customer support")
    _add_document(store, 2, "unrelated inventory management")

    results = _hybrid_search_chunks(
        store=store,
        query="Kubernetes cluster operation",
        user_id=1,
        top_k=2,
    )

    assert len(results) == 2
    assert all(result.dense_score == 0.2 for result in results)
    assert all(result.score == 0.1 for result in results)
    assert all(result.raw_dense_score == 0.2 for result in results)

    assert search(
        store=store,
        query="Kubernetes cluster operation",
        user_id=1,
        top_k=2,
        min_score=0.0,
    ) == []


def test_absolute_dense_signal_can_pass_relevance_gate():
    store = VectorStore(ControlledEmbedder())
    _add_document(store, 1, "relevant backend implementation")
    _add_document(store, 2, "unrelated customer support")

    hits = search(
        store=store,
        query="API design experience",
        user_id=1,
        top_k=2,
        min_score=0.0,
    )

    assert len(hits) == 1
    assert hits[0][1].document_id == 1
    assert hits[0][0] == 0.35


if __name__ == "__main__":
    test_equal_irrelevant_scores_do_not_become_one()
    test_absolute_dense_signal_can_pass_relevance_gate()
    print("HYBRID SEARCH SCORE TEST: PASS")
