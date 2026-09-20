"""항목 단위 분할 (SRS PAR-02~04).

글자 수로 자르지 않는다. 항목이 중간에서 잘리면 '어디까지 수행했는지'가
서로 다른 조각으로 흩어져 CMP-02의 수행 범위 판정이 불가능해진다.
"""
import re
from typing import List, Dict

from .cleaner import clean_text

SECTION_PATTERN = re.compile(r"^\s*\[(?P<name>[^\]]+)\]\s*$")
MAX_CHARS = 1200  # 한 항목이 지나치게 길면 문단 경계로 한 번 더 나눈다


def _split_long(text: str) -> List[str]:
    if len(text) <= MAX_CHARS:
        return [text]
    parts, buf = [], []
    for para in text.split("\n\n"):
        if sum(len(p) for p in buf) + len(para) > MAX_CHARS and buf:
            parts.append("\n\n".join(buf))
            buf = []
        buf.append(para)
    if buf:
        parts.append("\n\n".join(buf))
    return parts


def split_sections(pages: List[Dict], doc_type: str) -> List[Dict]:
    """제목([기술], [프로젝트] 등)을 기준으로 항목을 나눈다."""
    chunks: List[Dict] = []
    current = "미분류"
    buf: List[str] = []

    def flush(page):
        nonlocal buf
        if buf:
            body = clean_text("\n".join(buf))
            for part in _split_long(body):
                if part.strip():
                    chunks.append(
                        {"doc_type": doc_type, "section": current,
                         "text": part, "page": page}
                    )
        buf = []

    for p in pages:
        page_no = p.get("page")
        for line in p["text"].splitlines():
            m = SECTION_PATTERN.match(line)
            if m:
                flush(page_no)
                current = m.group("name")
            else:
                buf.append(line)
        flush(page_no)
    return chunks
