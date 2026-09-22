"""항목 단위 분할 (SRS PAR-02~04)."""
import re
from typing import List, Dict

from .cleaner import clean_text

SECTION_PATTERN = re.compile(r"^\s*\[(?P<name>[^\]]+)\]\s*$")
MAX_CHARS = 1200  # 한 항목이 지나치게 길 때만 문단 경계로 한 번 더 나눈다


def _split_long(text: str) -> List[str]:
    """너무 긴 항목을 문단 경계에서 나눈다(문장 중간에서 자르지 않는다)."""
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


def merge_paragraphs(pages: List[Dict]) -> List[Dict]:
    """DOCX처럼 문단 단위로 들어온 입력을 한 덩어리로 합친다.

    문단마다 따로 자르면 제목과 본문이 분리되어 항목 분할이 되지 않는다.
    위치 정보는 각 항목의 첫 문단 번호를 쓰기 위해 줄 단위로 보존한다.
    """
    if not pages or pages[0].get("para") is None:
        return pages
    merged, buf, first_para = [], [], None
    for p in pages:
        if first_para is None:
            first_para = p.get("para")
        buf.append(p["text"])
    merged.append({"page": None, "para": first_para, "text": "\n".join(buf)})
    return merged


def split_sections(pages: List[Dict], doc_type: str) -> List[Dict]:
    """제목([기술], [프로젝트] 등)을 기준으로 항목을 나눈다.

    글자 수로 자르지 않는 이유: 항목이 중간에서 잘리면 '어디까지 수행했는지'가
    서로 다른 조각으로 흩어져 CMP-02의 수행 범위 판정이 불가능해진다.
    """
    chunks: List[Dict] = []
    current = "미분류"   # 제목이 나오기 전 내용은 미분류로 둔다
    buf: List[str] = []

    # 모아 둔 줄을 하나의 항목으로 확정한다. 제목을 만났을 때와 페이지가 끝날 때 호출한다.
    def flush(page, para):
        nonlocal buf
        if buf:
            body = clean_text("\n".join(buf))
            for part in _split_long(body):
                if part.strip():
                    chunks.append({"doc_type": doc_type, "section": current,
                                   "text": part, "page": page, "para": para})
        buf = []

    # 한 줄씩 읽어 제목이면 항목을 바꾸고, 아니면 본문으로 모은다.
    # PDF는 page, DOCX는 para 가 위치 정보로 들어온다(PAR-07).
    for p in pages:
        page_no, para_no = p.get("page"), p.get("para")
        for line in p["text"].splitlines():
            m = SECTION_PATTERN.match(line)
            if m:
                flush(page_no, para_no)
                current = m.group("name")
            else:
                buf.append(line)
        flush(page_no, para_no)

    return chunks
