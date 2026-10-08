"""임베딩과 벡터 보관 (SRS RAG-01)."""
import hashlib
from typing import List

import numpy as np

DEFAULT_MODEL = "BAAI/bge-m3"
FALLBACK_DIM = 512


class Embedder:
    """문장을 벡터로 바꾼다.

    sentence-transformers가 설치되어 있으면 bge-m3를 쓰고, 없으면 해시 기반
    대체 임베딩으로 내려간다. 대체 임베딩은 흐름 확인용이며 검색 품질이 낮다.
    """

    def __init__(self, model_name: str = DEFAULT_MODEL):
        # 실제 모델 적재를 시도하고, 실패하면 대체 모드로 남는다(설치 없이도 실행 가능).
        self.model_name = model_name
        self.mode = "fallback"
        self.model = None
        try:
            from sentence_transformers import SentenceTransformer

            self.model = SentenceTransformer(model_name)
            self.mode = "bge-m3"
        except Exception:
            pass

    def encode(self, texts: List[str]) -> np.ndarray:
        """문장 목록을 벡터 배열로 변환한다."""
        if self.model is not None:
            return np.asarray(self.model.encode(texts, normalize_embeddings=True))

        # 대체 구현: 단어를 해시해 고정 길이 벡터의 자리에 세고, 길이를 1로 맞춘다.
        # 코사인 유사도를 내적만으로 계산할 수 있게 하려는 정규화다.
        vecs = np.zeros((len(texts), FALLBACK_DIM), dtype="float32")
        for i, t in enumerate(texts):
            for tok in t.replace("\n", " ").split():
                h = int(hashlib.md5(tok.encode()).hexdigest(), 16) % FALLBACK_DIM
                vecs[i, h] += 1.0
            vecs[i] /= (np.linalg.norm(vecs[i]) or 1.0)
        return vecs


class VectorStore:
    """메모리 기반 Evidence / RetrievalChunk / Vector 저장소

    Evidence:
        사용자 문서에서 추출한 원문 근거

    RetrievalChunk:
        검색을 위해 여러 Evidence와 문맥을 묶은 단위

    Vector:
        RetrievalChunk.text를 임베딩한 값

    운영 환경에서는 각각 DB + pgvector/Chroma 등으로 교체 가능
    """

    def __init__(self, embedder: Embedder):
        self.embedder = embedder

        # 원문 근거 저장소
        # evidence_id -> Evidence
        # 기존의 list -> dict 구조로 바꿔서, Evidence를 이용해 원문을 바로 조회할 수 있게 함
        self.evidence_by_id = {}

        # 검색용 chunk 저장소
        # chunk_id -> RetrievalChunk
        self.chunk_by_id = {}

        # vector의 각 행과 대응하는 chunk_id
        self.vector_chunk_ids = []

        # RetrievalChunk.text embedding
        self.vectors = None

    @property
    def evidences(self):
        """기존 코드 호환용 Evidence 목록"""
        return list(self.evidence_by_id.values())

    @property
    def chunks(self):
        """현재 저장된 RetrievalChunk 목록"""
        return list(self.chunk_by_id.values())

    def add_evidences(self, evidences):
        """Evidence를 원문 근거 저장소에 보관"""
        for evidence in evidences:
            self.evidence_by_id[evidence.evidence_id] = evidence

    def add_chunks(self, chunks):
        """RetrievalChunk를 임베딩하고 검색 저장소에 보관"""
        if not chunks:
            return

        texts = [chunk.text for chunk in chunks]
        vecs = self.embedder.encode(texts)

        for chunk in chunks:
            chunk.embedding_ref = f"vec:{chunk.chunk_id}"
            self.chunk_by_id[chunk.chunk_id] = chunk
            self.vector_chunk_ids.append(chunk.chunk_id)

        self.vectors = (
            vecs
            if self.vectors is None
            else np.vstack([self.vectors, vecs])
        )

    def add_document(self, evidences, chunks): # 새 v2 경로
        """한 문서에서 생성된 Evidence와 RetrievalChunk를 함께 저장"""
        self.add_evidences(evidences)
        self.add_chunks(chunks)

    def add(self, evidences): # legacy 임시 경로
        """Legacy 색인 경로 호환용

        기존 PDF / text / analysis-json 경로는 아직 Evidence 자체를
        검색 단위로 사용함. 새 DOCX 파이프라인 연결 전까지 유지
        """
        if not evidences:
            return

        self.add_evidences(evidences)

        # 기존 search.py가 store.vectors[i]와 store.evidences[i]를
        # 같은 위치라고 가정하므로 legacy vector도 기존 방식으로 만든다.
        vecs = self.embedder.encode(
            [evidence.evidence_text for evidence in evidences]
        )

        for evidence in evidences:
            evidence.embedding_ref = f"vec:{evidence.evidence_id}"

        self.vectors = (
            vecs
            if self.vectors is None
            else np.vstack([self.vectors, vecs])
        )

    def get_evidence(self, evidence_id):
        """evidence_id로 원문 Evidence를 조회"""
        return self.evidence_by_id.get(evidence_id)

    def get_chunk(self, chunk_id):
        """chunk_id로 RetrievalChunk를 조회"""
        return self.chunk_by_id.get(chunk_id)

    def resolve_evidences(self, evidence_ids):
        """검색된 RetrievalChunk의 evidence_ids를 실제 Evidence로 복원"""
        return [
            self.evidence_by_id[evidence_id]
            for evidence_id in evidence_ids
            if evidence_id in self.evidence_by_id
        ]

    def delete_by_document(self, document_id: int) -> int:
        """문서에 속한 Evidence, RetrievalChunk, Vector를 함께 삭제"""

        # 삭제 전 Evidence 개수.
        removed_evidence_count = sum(
            1
            for evidence in self.evidence_by_id.values()
            if evidence.document_id == document_id
        )

        # Evidence 삭제
        self.evidence_by_id = {
            evidence_id: evidence
            for evidence_id, evidence in self.evidence_by_id.items()
            if evidence.document_id != document_id
        }

        # 삭제 대상 chunk 식별
        removed_chunk_ids = {
            chunk_id
            for chunk_id, chunk in self.chunk_by_id.items()
            if chunk.document_id == document_id
        }

        # Chunk 삭제
        self.chunk_by_id = {
            chunk_id: chunk
            for chunk_id, chunk in self.chunk_by_id.items()
            if chunk.document_id != document_id
        }

        # Chunk vector 삭제
        if removed_chunk_ids and self.vectors is not None:
            keep_indices = [
                i
                for i, chunk_id in enumerate(self.vector_chunk_ids)
                if chunk_id not in removed_chunk_ids
            ]

            self.vector_chunk_ids = [
                self.vector_chunk_ids[i]
                for i in keep_indices
            ]

            self.vectors = (
                self.vectors[keep_indices]
                if keep_indices
                else None
            )

        return removed_evidence_count
