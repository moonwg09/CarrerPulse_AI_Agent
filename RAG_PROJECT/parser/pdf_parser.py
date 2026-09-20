"""PDF 텍스트 추출 (SRS PAR-01).

페이지 번호를 함께 돌려주는 것이 중요하다. PAR-07(원문 근거 연결)과
CMP-05(비교 근거 조회)에서 출처를 표시하려면 위치 정보가 필요하다.
"""
from typing import List, Dict


def extract_pdf(path: str) -> List[Dict]:
    """반환: [{"page": 1, "text": "..."}]"""
    import fitz  # pymupdf

    pages = []
    with fitz.open(path) as doc:
        for i, page in enumerate(doc, start=1):
            pages.append({"page": i, "text": page.get_text()})
    return pages


def is_scanned(pages: List[Dict], min_chars: int = 30) -> bool:
    """텍스트가 거의 없으면 스캔 PDF로 본다. OCR은 SRS에서 후속 구현."""
    return sum(len(p["text"].strip()) for p in pages) < min_chars
