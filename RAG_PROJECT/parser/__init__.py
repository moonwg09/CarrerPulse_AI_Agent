"""parser — 업로드된 서류에서 텍스트를 꺼낸다 (PAR-01)."""
from typing import List, Dict, Optional, Tuple

from .pdf_parser import extract_pdf, is_scanned
from .docx_parser import extract_docx

SUPPORTED = ("pdf", "docx")


def extract_document(path: str) -> Tuple[Optional[List[Dict]], Optional[str]]:
    """확장자로 분기한다.

    반환: (pages, error). 실패 시 pages=None, error에 사유 (DOC-11 안내용).
    """
    ext = str(path).lower().rsplit(".", 1)[-1]
    if ext not in SUPPORTED:
        return None, f"지원하지 않는 형식입니다: .{ext}"
    try:
        if ext == "pdf":
            pages = extract_pdf(path)
            if is_scanned(pages):
                return None, "텍스트를 추출할 수 없는 문서입니다(스캔본으로 보입니다)."
            return pages, None
        return extract_docx(path), None
    except Exception as e:  # noqa: BLE001 - 사유를 사용자에게 보여주기 위함
        return None, f"추출 실패: {type(e).__name__}"


__all__ = ["extract_document", "extract_pdf", "extract_docx", "is_scanned"]
