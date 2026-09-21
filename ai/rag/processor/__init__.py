"""processor — 추출한 텍스트를 정리하고 항목으로 나눠 근거로 만든다 (PAR-02~04, 06·07)."""
from .cleaner import clean_text
from .chunker import split_sections
from .metadata import Evidence, build_evidences

__all__ = ["clean_text", "split_sections", "Evidence", "build_evidences"]
