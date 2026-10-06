# processor 패키지의 공개 창구다.
# 바깥(store.py 등)에서는 하위 모듈 경로를 일일이 알 필요 없이
# rag.processor 한 곳만 import 하면 되도록, 실제 구현 함수를 여기로 모아서 내보낸다.
from .cleaner import clean_text
from .chunker import split_sections, merge_paragraphs, SECTION_PATTERN, MAX_CHARS
from .metadata import Evidence, build_evidences

__all__ = [
    "clean_text",
    "split_sections",
    "merge_paragraphs",
    "SECTION_PATTERN",
    "MAX_CHARS",
    "Evidence",
    "build_evidences",
]