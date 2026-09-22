"""PDF 텍스트 추출 (SRS PAR-01)."""
from typing import Dict, List


def extract_pdf(source) -> List[Dict]:
    """PDF에서 페이지 단위 텍스트를 추출한다.

    source: 파일 경로(str) 또는 파일 내용(bytes)
    반환: [{"page": 1, "text": "..."}]
    """
    import pymupdf

    # 페이지 번호를 함께 담는다. PAR-07(원문 근거 연결)과 CMP-05(근거 조회)에서
    # "이력서 p.2"처럼 출처를 보여 주려면 위치 정보가 있어야 한다.
    pages = []
    doc = pymupdf.open(stream=source, filetype="pdf") if isinstance(source, (bytes, bytearray)) \
        else pymupdf.open(source)
    with doc:
        for i, page in enumerate(doc, start=1):
            pages.append({"page": i, "text": page.get_text()})
    return pages


def is_scanned(pages: List[Dict], min_chars: int = 30) -> bool:
    """스캔본(이미지) PDF 판별."""
    # 글자가 거의 없으면 스캔본으로 본다. OCR은 SRS에서 후속 구현으로 분리했으므로
    # 여기서는 처리 불가로 판단해 실패 사유를 돌려준다(DOC-11).
    return sum(len(p["text"].strip()) for p in pages) < min_chars
"""PDF 텍스트 추출 (SRS PAR-01)."""
from typing import Dict, List


def extract_pdf(source) -> List[Dict]:
    """PDF에서 페이지 단위 텍스트를 추출한다.

    source: 파일 경로(str) 또는 파일 내용(bytes)
    반환: [{"page": 1, "text": "..."}]
    """
    import pymupdf

    # 페이지 번호를 함께 담는다. PAR-07(원문 근거 연결)과 CMP-05(근거 조회)에서
    # "이력서 p.2"처럼 출처를 보여 주려면 위치 정보가 있어야 한다.
    pages = []
    doc = pymupdf.open(stream=source, filetype="pdf") if isinstance(source, (bytes, bytearray)) \
        else pymupdf.open(source)
    with doc:
        for i, page in enumerate(doc, start=1):
            pages.append({"page": i, "text": page.get_text()})
    return pages


def is_scanned(pages: List[Dict], min_chars: int = 30) -> bool:
    """스캔본(이미지) PDF 판별."""
    # 글자가 거의 없으면 스캔본으로 본다. OCR은 SRS에서 후속 구현으로 분리했으므로
    # 여기서는 처리 불가로 판단해 실패 사유를 돌려준다(DOC-11).
    return sum(len(p["text"].strip()) for p in pages) < min_chars
