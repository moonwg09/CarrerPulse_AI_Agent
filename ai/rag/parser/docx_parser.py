"""DOCX 구조 추출."""

import io
from typing import List

from docx import Document
from docx.table import Table
from docx.text.paragraph import Paragraph

from .models import DocumentBlock, DocumentLocation


def _clean_cell_text(text: str) -> str:
    return " ".join(text.split())


def _table_to_rows(table: Table) -> List[List[str]]:
    rows = []

    for row in table.rows:
        values = [_clean_cell_text(cell.text) for cell in row.cells]

        if any(values):
            rows.append(values)

    return rows


def _table_to_text(rows: List[List[str]]) -> str:
    """표 구조를 검색 가능한 텍스트로 변환"""

    return "\n".join(
        " | ".join(cell for cell in row)
        for row in rows
    )


def extract_docx_blocks(source) -> List[DocumentBlock]:
    """DOCX의 문단과 표를 실제 문서 순서대로 추출"""

    doc = (
        Document(io.BytesIO(source))
        if isinstance(source, (bytes, bytearray))
        else Document(source)
    )

    blocks: List[DocumentBlock] = []

    paragraph_no = 0
    table_no = 0
    block_no = 0

    for child in doc.element.body.iterchildren():

        # -------------------------
        # Paragraph
        # -------------------------
        if child.tag.endswith("}p"):

            paragraph_no += 1
            paragraph = Paragraph(child, doc)

            text = paragraph.text.strip()

            if not text:
                continue

            block_no += 1

            style_name = (
                paragraph.style.name
                if paragraph.style is not None
                else None
            )

            is_heading = bool(
                style_name
                and style_name.lower().startswith("heading")
            )

            blocks.append(
                DocumentBlock(
                    block_id=f"B-{block_no:04d}",
                    block_type="paragraph",
                    text=text,
                    location=DocumentLocation(
                        paragraph=paragraph_no,
                    ),
                    style=style_name,
                    is_heading=is_heading,
                )
            )

        # -------------------------
        # Table
        # -------------------------
        elif child.tag.endswith("}tbl"):

            table_no += 1
            table = Table(child, doc)

            rows = _table_to_rows(table)

            if not rows:
                continue

            block_no += 1

            blocks.append(
                DocumentBlock(
                    block_id=f"B-{block_no:04d}",
                    block_type="table",
                    text=_table_to_text(rows),
                    location=DocumentLocation(
                        table=table_no,
                    ),
                    rows=rows,
                )
            )

    return blocks


def extract_docx(source) -> List[dict]:
    """기존 processor 호환용 API"""

    blocks = extract_docx_blocks(source)

    return [
        block.to_legacy_dict()
        for block in blocks
    ]