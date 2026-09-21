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
    """메모리 기반 벡터 저장소.

    운영에서는 Chroma나 pgvector로 교체한다.
    delete_by_document가 DOC-09('관련 검색용 데이터 즉시 삭제')를 담당한다.
    """

    def __init__(self, embedder: Embedder):
        self.embedder = embedder
        self.evidences = []
        self.vectors = None

    def add(self, evidences):
        """근거를 벡터로 만들어 저장한다."""
        # embedding_ref를 남겨 두면 ERD의 DOCUMENT_CHUNKS.embedding_ref와 연결된다.
        if not evidences:
            return
        vecs = self.embedder.encode([e.evidence_text for e in evidences])
        for e in evidences:
            e.embedding_ref = f"vec:{e.evidence_id}"
        self.evidences.extend(evidences)
        self.vectors = vecs if self.vectors is None else np.vstack([self.vectors, vecs])

    def delete_by_document(self, document_id: int) -> int:
        """DOC-09: 삭제 요청된 서류의 근거와 벡터를 함께 제거한다."""
        # 원본만 지우고 벡터를 남기면 삭제한 서류가 계속 검색되므로 같이 지운다.
        keep = [i for i, e in enumerate(self.evidences) if e.document_id != document_id]
        removed = len(self.evidences) - len(keep)
        self.evidences = [self.evidences[i] for i in keep]
        self.vectors = self.vectors[keep] if self.vectors is not None and keep else None
        return removed
