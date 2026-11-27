import tempfile
import unittest
from pathlib import Path

import numpy as np

from rag.embedding.embedding import VectorStore
from rag.processor.metadata import Evidence
from rag.processor.retrieval_chunk import RetrievalChunk


class FakeEmbedder:
    def encode_documents(self, texts):
        return np.asarray([[1.0, float(index + 1)] for index, _ in enumerate(texts)])

    encode = encode_documents


class ChromaPersistenceTest(unittest.TestCase):
    def test_restart_restores_and_delete_removes_document(self):
        test_root = Path(__file__).resolve().parent / ".chroma"
        test_root.mkdir(exist_ok=True)
        # Chroma의 HNSW 파일은 Windows 프로세스가 끝날 때 잠금이 풀린다.
        # 테스트 저장소는 gitignore 대상 아래에 두고 다음 실행에서 교체한다.
        with tempfile.TemporaryDirectory(
            dir=test_root,
            ignore_cleanup_errors=True,
        ) as path:
            evidence = Evidence(
                evidence_id="EV-0001",
                user_id=7,
                document_id=11,
                doc_type="resume",
                section="경력",
                source_type="document",
                source_location="p.1",
                evidence_text="FastAPI로 REST API를 구현했습니다.",
            )
            chunk = RetrievalChunk(
                chunk_id="CH-0001",
                user_id=7,
                document_id=11,
                doc_type="resume",
                semantic_unit_id="UNIT-1",
                category="EXPERIENCE",
                section="경력",
                title="API 구현",
                text=evidence.evidence_text,
                evidence_ids=[evidence.evidence_id],
            )

            first = VectorStore(FakeEmbedder(), persistence_path=path)
            first.add_document([evidence], [chunk])

            restarted = VectorStore(FakeEmbedder(), persistence_path=path)
            self.assertEqual(evidence.evidence_text, restarted.get_evidence("EV-0001", 7).evidence_text)
            self.assertEqual("CH-0001", restarted.get_chunk("CH-0001", 7).chunk_id)
            self.assertEqual((1, 2), restarted.vectors.shape)

            self.assertEqual(1, restarted.delete_by_document(7, 11))
            after_delete = VectorStore(FakeEmbedder(), persistence_path=path)
            self.assertEqual([], after_delete.evidences)
            self.assertEqual([], after_delete.chunks)

            # Windows에서도 SQLite 파일 잠금이 해제된 뒤 임시 폴더를 지운다.
            from chromadb.api.client import SharedSystemClient
            del first, restarted, after_delete
            SharedSystemClient.clear_system_cache()


if __name__ == "__main__":
    unittest.main()
