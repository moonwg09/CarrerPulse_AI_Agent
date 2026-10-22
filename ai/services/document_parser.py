from pathlib import Path
from typing import List, Dict

import pymupdf
from docx import Document
from docx.table import Table
from docx.text.paragraph import Paragraph
from docx.oxml.table import CT_Tbl
from docx.oxml.text.paragraph import CT_P


def extract_pdf(file_path: str) -> List[Dict]:
    """
    PDF 파일에서 페이지별 텍스트 추출
    """

    contents = []

    with pymupdf.open(file_path) as pdf:
        for page_index, page in enumerate(pdf):
            text = page.get_text("text").strip()

            if not text:
                continue

            contents.append({
                "page": page_index + 1,
                "text": text
            })

    return contents


def iter_docx_blocks(document):
    """
    DOCX 내부의 문단과 표를 원래 문서 순서대로 반환
    """

    body = document.element.body

    for child in body.iterchildren():

        if isinstance(child, CT_P):
            yield Paragraph(child, document)

        elif isinstance(child, CT_Tbl):
            yield Table(child, document)


def extract_docx(file_path: str) -> List[Dict]:
    """
    DOCX 파일에서 일반 문단 + 표 내부 텍스트 추출
    """

    try:
        document = Document(file_path)

    except Exception as e:
        raise ValueError(
            f"DOCX 파일을 읽을 수 없습니다: {str(e)}"
        )

    contents = []
    index = 0

    for block in iter_docx_blocks(document):

        # 일반 문단
        if isinstance(block, Paragraph):

            text = block.text.strip()

            if not text:
                continue

            contents.append({
                "paragraphIndex": index,
                "text": text
            })

            index += 1

        # 표
        elif isinstance(block, Table):

            for row in block.rows:

                row_texts = []

                for cell in row.cells:

                    text = cell.text.strip()

                    if text and (not row_texts or row_texts[-1] != text):
                        row_texts.append(text)

                if row_texts:

                    contents.append({
                        "paragraphIndex": index,
                        "text": " | ".join(row_texts)
                    })

                    index += 1

    return contents


def extract_document(file_path: str) -> List[Dict]:
    """
    파일 확장자를 확인하고 PDF 또는 DOCX 추출
    """

    path = Path(file_path)

    if not path.exists():
        raise FileNotFoundError(
            f"파일을 찾을 수 없습니다: {file_path}"
        )

    extension = path.suffix.lower()

    if extension == ".pdf":
        return extract_pdf(file_path)

    if extension == ".docx":
        return extract_docx(file_path)

    raise ValueError(
        f"지원하지 않는 파일 형식입니다: {extension}"
    )

