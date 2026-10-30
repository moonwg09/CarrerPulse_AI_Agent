"""임베딩과 벡터 보관 (SRS RAG-01)."""
import hashlib
import json
import os
from dataclasses import asdict
from pathlib import Path
from typing import Dict, List, Tuple

import numpy as np
from dotenv import load_dotenv


load_dotenv()

DEFAULT_MODEL = "Qwen/Qwen3-Embedding-0.6B"
FALLBACK_DIM = 512
QUERY_PROMPT_NAME = "query"


def _env_flag(name: str, default: bool = False) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


class Embedder:
    """문장을 벡터로 바꾼다.

    기본 모델은 Qwen3-Embedding-0.6B다. 운영·시연 환경에서는 모델 적재에
    실패하면 명시적으로 중단한다. 해시 임베딩은 테스트에서 환경변수로
    허용한 경우에만 사용한다.
    """

    def __init__(
        self,
        model_name: str | None = None,
        device: str | None = None,
        allow_fallback: bool | None = None,
        model=None,
        model_loader=None,
    ):
        self.model_name = model_name or os.getenv(
            "RAG_EMBEDDING_MODEL",
            DEFAULT_MODEL,
        )
        self.device = device or os.getenv("RAG_EMBEDDING_DEVICE", "cpu")
        self.allow_fallback = (
            _env_flag("RAG_ALLOW_HASH_FALLBACK")
            if allow_fallback is None
            else allow_fallback
        )
        self.mode = "fallback"
        self.model = model

        if self.model is not None:
            self.mode = "qwen3"
            return

        try:
            if model_loader is None:
                from sentence_transformers import SentenceTransformer

                model_loader = SentenceTransformer

            self.model = model_loader(
                self.model_name,
                device=self.device,
            )
            self.mode = "qwen3"
        except Exception as exc:
            if not self.allow_fallback:
                raise RuntimeError(
                    f"임베딩 모델을 불러오지 못했습니다: {self.model_name}"
                ) from exc

    def encode_documents(self, texts: List[str]) -> np.ndarray:
        """색인 대상 문서를 Qwen document 방식으로 임베딩한다."""
        return self._encode(texts, is_query=False)

    def encode_queries(self, texts: List[str]) -> np.ndarray:
        """검색 질의에 Qwen의 query instruction을 적용해 임베딩한다."""
        return self._encode(texts, is_query=True)

    def encode(self, texts: List[str]) -> np.ndarray:
        """기존 호출 호환용 문서 임베딩."""
        return self.encode_documents(texts)

    def _encode(self, texts: List[str], is_query: bool) -> np.ndarray:
        if self.model is not None:
            options = {"normalize_embeddings": True}
            if is_query:
                options["prompt_name"] = QUERY_PROMPT_NAME
            return np.asarray(self.model.encode(texts, **options))

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

    def __init__(self, embedder: Embedder, persistence_path: str | None = None):
        self.embedder = embedder
        self.persistence_path = persistence_path
        self._collection = None

        # Evidence/Chunk의 표시용 ID는 사용자마다 EV-0001/CH-0001부터 시작한다.
        # 따라서 저장 키에는 user_id를 포함해야 다른 사용자의 자료가 덮어써지지 않는다.
        self.evidence_by_key: Dict[Tuple[int, str], object] = {}

        self.chunk_by_key: Dict[Tuple[int, str], object] = {}

        # vector의 각 행과 대응하는 (user_id, chunk_id)
        self.vector_chunk_keys: List[Tuple[int, str]] = []

        # RetrievalChunk.text embedding
        self.vectors = None

        if persistence_path:
            self._open_persistent_collection(persistence_path)
            self._restore_persistent_data()

    def _open_persistent_collection(self, persistence_path: str) -> None:
        """Chroma 영속 컬렉션을 연다. 설정된 운영 저장소는 실패 시 중단한다."""
        try:
            import chromadb
        except ImportError as exc:
            raise RuntimeError(
                "RAG_CHROMA_PATH가 설정됐지만 chromadb가 설치되지 않았습니다."
            ) from exc

        path = str(Path(persistence_path).resolve())
        client = chromadb.PersistentClient(path=path)
        self._collection = client.get_or_create_collection(
            name="careerpulse_rag_chunks",
            metadata={"hnsw:space": "cosine"},
        )

    @staticmethod
    def _record_id(user_id: int, document_id: int, chunk_id: str) -> str:
        return f"u{user_id}:d{document_id}:{chunk_id}"

    def _restore_persistent_data(self) -> None:
        if self._collection is None:
            return

        records = self._collection.get(include=["embeddings", "metadatas"])
        embeddings = records.get("embeddings")
        if embeddings is None or len(embeddings) == 0:
            return

        from ..processor.metadata import Evidence
        from ..processor.retrieval_chunk import RetrievalChunk

        restored_vectors = []
        for metadata, embedding in zip(records["metadatas"], embeddings):
            chunk = RetrievalChunk(**json.loads(metadata["chunk_json"]))
            key = (chunk.user_id, chunk.chunk_id)
            self.chunk_by_key[key] = chunk
            self.vector_chunk_keys.append(key)
            restored_vectors.append(embedding)

            for row in json.loads(metadata["evidences_json"]):
                evidence = Evidence(**row)
                self.evidence_by_key[(evidence.user_id, evidence.evidence_id)] = evidence

        self.vectors = np.asarray(restored_vectors, dtype="float32")

    def _persist_chunks(self, chunks, vecs) -> None:
        if self._collection is None or not chunks:
            return

        ids = []
        documents = []
        metadatas = []
        embeddings = []
        for chunk, vector in zip(chunks, vecs):
            evidences = self.resolve_evidences(chunk.evidence_ids, chunk.user_id)
            ids.append(self._record_id(chunk.user_id, chunk.document_id, chunk.chunk_id))
            documents.append(chunk.text)
            metadatas.append({
                "user_id": chunk.user_id,
                "document_id": chunk.document_id,
                "chunk_json": json.dumps(asdict(chunk), ensure_ascii=False),
                "evidences_json": json.dumps(
                    [asdict(evidence) for evidence in evidences],
                    ensure_ascii=False,
                ),
            })
            embeddings.append(np.asarray(vector, dtype="float32").tolist())

        self._collection.upsert(
            ids=ids,
            documents=documents,
            metadatas=metadatas,
            embeddings=embeddings,
        )

    @property
    def evidences(self):
        """기존 코드 호환용 Evidence 목록"""
        return list(self.evidence_by_key.values())

    @property
    def chunks(self):
        """현재 저장된 RetrievalChunk 목록"""
        return list(self.chunk_by_key.values())

    @property
    def vector_chunk_ids(self):
        """기존 단일 사용자 테스트와 출력 형식을 위한 Chunk ID 목록."""
        return [chunk_id for _, chunk_id in self.vector_chunk_keys]

    def add_evidences(self, evidences):
        """Evidence를 원문 근거 저장소에 보관"""
        for evidence in evidences:
            key = (evidence.user_id, evidence.evidence_id)
            self.evidence_by_key[key] = evidence

    def add_chunks(self, chunks):
        """RetrievalChunk를 임베딩하고 검색 저장소에 보관"""
        if not chunks:
            return

        texts = [chunk.text for chunk in chunks]
        encode_documents = getattr(
            self.embedder,
            "encode_documents",
            self.embedder.encode,
        )
        vecs = encode_documents(texts)

        for chunk in chunks:
            key = (chunk.user_id, chunk.chunk_id)
            chunk.embedding_ref = f"vec:{chunk.user_id}:{chunk.chunk_id}"
            self.chunk_by_key[key] = chunk
            self.vector_chunk_keys.append(key)

        self.vectors = (
            vecs
            if self.vectors is None
            else np.vstack([self.vectors, vecs])
        )
        self._persist_chunks(chunks, vecs)

    def add_document(self, evidences, chunks): # 새 v2 경로
        """한 문서에서 생성된 Evidence와 RetrievalChunk를 함께 저장"""
        self.add_evidences(evidences)
        self.add_chunks(chunks)

    def add(self, evidences):  # legacy 임시 경로
        """Legacy Evidence 저장 호환용.

        Legacy PDF / text / analysis-json 경로의 Evidence는 보관하지만,
        Evidence 자체를 RetrievalChunk vector 저장소에 넣지는 않는다.

        검색 vector는 RetrievalChunk만 관리한다.
        """
        if not evidences:
            return

        self.add_evidences(evidences)

    def get_evidence(self, evidence_id: str, user_id: int):
        """현재 사용자의 evidence_id로 원문 Evidence를 조회한다."""
        return self.evidence_by_key.get((user_id, evidence_id))

    def get_chunk(self, chunk_id: str, user_id: int):
        """현재 사용자의 chunk_id로 RetrievalChunk를 조회한다."""
        return self.chunk_by_key.get((user_id, chunk_id))

    def resolve_evidences(self, evidence_ids, user_id: int):
        """검색된 RetrievalChunk의 evidence_ids를 실제 Evidence로 복원"""
        return [
            self.evidence_by_key[(user_id, evidence_id)]
            for evidence_id in evidence_ids
            if (user_id, evidence_id) in self.evidence_by_key
        ]

    def delete_by_document(self, user_id: int, document_id: int) -> int:
        """사용자가 소유한 문서의 Evidence, Chunk, Vector만 삭제한다."""

        # 삭제 전 Evidence 개수.
        removed_evidence_count = sum(
            1
            for evidence in self.evidence_by_key.values()
            if evidence.user_id == user_id and evidence.document_id == document_id
        )

        # Evidence 삭제
        self.evidence_by_key = {
            key: evidence
            for key, evidence in self.evidence_by_key.items()
            if not (
                evidence.user_id == user_id
                and evidence.document_id == document_id
            )
        }

        # 삭제 대상 chunk 식별
        removed_chunk_keys = {
            key
            for key, chunk in self.chunk_by_key.items()
            if chunk.user_id == user_id and chunk.document_id == document_id
        }

        if self._collection is not None:
            records = self._collection.get(
                where={
                    "$and": [
                        {"user_id": {"$eq": user_id}},
                        {"document_id": {"$eq": document_id}},
                    ]
                }
            )
            if records["ids"]:
                self._collection.delete(ids=records["ids"])

        # Chunk 삭제
        self.chunk_by_key = {
            key: chunk
            for key, chunk in self.chunk_by_key.items()
            if key not in removed_chunk_keys
        }

        # Chunk vector 삭제
        if removed_chunk_keys and self.vectors is not None:
            keep_indices = [
                i
                for i, key in enumerate(self.vector_chunk_keys)
                if key not in removed_chunk_keys
            ]

            self.vector_chunk_keys = [
                self.vector_chunk_keys[i]
                for i in keep_indices
            ]

            self.vectors = (
                self.vectors[keep_indices]
                if keep_indices
                else None
            )

        return removed_evidence_count
