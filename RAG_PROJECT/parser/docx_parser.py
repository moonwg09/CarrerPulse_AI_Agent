"""DOCX 텍스트 추출 (SRS PAR-01)."""
from typing import List, Dict


def extract_docx(path: str) -> List[Dict]:
    """반환: [{"page": None, "para": 1, "text": "..."}]"""
    from docx import Document

    doc = Document(path)
    return [
        {"page": None, "para": i, "text": p.text}
        for i, p in enumerate(doc.paragraphs, start=1)
        if p.text.strip()
    ]
