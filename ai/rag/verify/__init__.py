"""verify — 생성된 문장이 근거에 있는 내용인지 확인한다 (GDE-09)."""
from .verifier import verify_guide, generate_and_verify, MIN_OVERLAP, MAX_REGENERATE

__all__ = ["verify_guide", "generate_and_verify", "MIN_OVERLAP", "MAX_REGENERATE"]
