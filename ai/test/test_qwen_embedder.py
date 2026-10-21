import numpy as np

from rag.embedding.embedding import DEFAULT_MODEL, Embedder


class RecordingModel:
    def __init__(self):
        self.calls = []

    def encode(self, texts, **options):
        self.calls.append((texts, options))
        return np.ones((len(texts), 4), dtype="float32")


def test_qwen_is_default_and_query_uses_instruction():
    model = RecordingModel()
    embedder = Embedder(model=model)

    document_vectors = embedder.encode_documents(["FastAPI 서버 개발"])
    query_vectors = embedder.encode_queries(["FastAPI 경험"])

    assert DEFAULT_MODEL == "Qwen/Qwen3-Embedding-0.6B"
    assert embedder.mode == "qwen3"
    assert document_vectors.shape == (1, 4)
    assert query_vectors.shape == (1, 4)
    assert model.calls[0][1] == {"normalize_embeddings": True}
    assert model.calls[1][1] == {
        "normalize_embeddings": True,
        "prompt_name": "query",
    }


def test_hash_fallback_requires_explicit_opt_in():
    class BrokenLoader:
        def __call__(self, *args, **kwargs):
            raise OSError("model unavailable")

    try:
        Embedder(
            model_name="invalid",
            allow_fallback=False,
            model_loader=BrokenLoader(),
        )
    except RuntimeError as exc:
        assert "임베딩 모델" in str(exc)
    else:
        raise AssertionError("운영 모드에서는 모델 적재 실패를 숨기면 안 됩니다.")

    fallback = Embedder(
        model_name="invalid",
        allow_fallback=True,
        model_loader=BrokenLoader(),
    )
    assert fallback.mode == "fallback"


if __name__ == "__main__":
    test_qwen_is_default_and_query_uses_instruction()
    test_hash_fallback_requires_explicit_opt_in()
    print("QWEN EMBEDDER TEST: PASS")
