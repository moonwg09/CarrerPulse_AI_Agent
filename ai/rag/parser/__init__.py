"""parser - 업로드 된 서류 파일에서 택스트를 꺼낸다 (SRS PAR-01, DOC-03, DOC-11)."""

from typing import Dict, List, Optional, Tuple

from .docx_parser import extract_docx
from .pdf_parser import extract_pdf, is_scanned

SUPPORTED_EXTENSIONS = ("pdf", "docx")
MAX_FILE_BYTES = 10 * 1024 * 1024

class ParseError(Exception):
    """추출 실패. 사용자에게 보여 줄 사유를 담는다 (DOC-11)."""

    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message

def detect_extension(filename: str) -> str:
    """파일 이름에서 확장자만 소문자로 뽑는다."""
    return str(filename).lower().rsplit(".", 1)[-1] if "." in str(filename) else ""

def validate(filename: str, size_bytes: Optional[int] = None) -> None:
    """형식과 크기를 확인한다. 조건에 맞지 않으면 ParseError를 던진다."""
    # 실제 형식 검사는 추출 단계에서 한번 더 이루어진다. 확장자만 바꾼 파일은 
    # pymupdf.python-docx 가 열지 못하므로 그때 걸린다(DOC-03).
    ext = detect_extension(filename)
    if ext not in SUPPORTED_EXTENSIONS:
        raise ParseError("UNSUPPORTED_FORMAT", f"지원하지 않는 형식입니다: .{ext or '확장자 없음'} (PDF, DOCX 만 가능)")
    if size_bytes is not None and size_bytes > MAX_FILE_BYTES:
        raise ParseError("FILE_TOO_LARGE", f"파일이 너무 큽니다. {size_bytes / 1024 / 1024:.1f}MB (최대 10MB)")

def extract(source, filename: str) -> Tuple[List[Dict], str]:
    """확장자로 분기해 텍스트를 추출한다.
    source: 파일 경로(str) 또는 파일 내용(bytes)
    반환: (pages, extraction_method)
    """

    ext = detect_extension(filename)
    try:
        if ext == "pdf":
            pages = extract_pdf(source)
            # 스캔본은 텍스트가 없어 분석할 수 없다. OCR은 후속 구현이다.
            if is_scanned(pages):
                raise ParseError("NO_TEXT",
                                 "텍스트를 추출할 수 없는 문서입니다(스캔본으로 보입니다).")
            return pages, "pymupdf"
        pages = extract_docx(source)
        if not pages:
            raise ParseError("NO_TEXT", "문서에서 읽을 수 있는 텍스트가 없습니다.")
        return pages, "python-docx"

    except ParseError:
        raise
    except Exception as e:  # noqa: BLE001 — 사용자에게 사유를 보여 주기 위해 넓게 잡는다
        raise ParseError("EXTRACT_FAILED", f"파일을 읽지 못했습니다: {type(e).__name__}")


__all__ = ["extract", "validate", "detect_extension", "ParseError",
           "SUPPORTED_EXTENSIONS", "MAX_FILE_BYTES", "extract_pdf", "extract_docx", "is_scanned"]
