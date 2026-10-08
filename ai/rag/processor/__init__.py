# processor 패키지의 공개 창구다.
# 바깥(store.py 등)에서는 하위 모듈 경로를 일일이 알 필요 없이
# rag.processor 한 곳만 import 하면 되도록, 실제 구현 함수를 여기로 모아서 내보낸다.
from .cleaner import clean_text
from .chunker import split_sections, merge_paragraphs, SECTION_PATTERN, MAX_CHARS
from .metadata import Evidence, build_evidences
from .user_analysis import to_chunks as analysis_to_chunks

# v2 추가
from .semantic_unit import SemanticUnit
from .semantic_parser import build_semantic_units, embedding_units
from .evidence_builder import build_evidences as build_semantic_evidences
from .retrieval_chunk import RetrievalChunk
from .retrieval_builder import build_retrieval_chunks


# 패키지 밖으로 공개할 이름을 명시한다.
# 여기에 없는 이름은 내부 구현으로 보고, 바깥에서 직접 쓰지 않는다는 약속이다.
__all__ = [
    "clean_text",
    "split_sections",
    "merge_paragraphs",
    "SECTION_PATTERN",
    "MAX_CHARS",
    "Evidence",
    "build_evidences",
    "analysis_to_chunks",

    # v2 추가
    "SemanticUnit",
    "build_semantic_units",
    "embedding_units",
    "build_semantic_evidences",
    "RetrievalChunk",
    "build_retrieval_chunks",
]

# TODO: skill 항목에서 하나의 행에 두개의 열이 있는 경우 해결 -> 일반적이지 않은 경우