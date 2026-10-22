from typing import Any, Dict, List

from schemas.rag_schema import DocumentChunk


def create_document_chunks(
    parsed_items: List[Dict[str, Any]],
    document_type: str,
    chunk_size: int = 700,
    overlap: int = 100,
) -> List[DocumentChunk]:

    chunks: List[DocumentChunk] = []

    chunk_counter = 0

    for item in parsed_items:

        text = (
            item.get("text")
            or ""
        ).strip()

        if not text:
            continue

        page = item.get("page")

        paragraph_index = item.get(
            "paragraphIndex"
        )

        # 짧은 텍스트는 그대로 하나의 chunk로 사용
        if len(text) <= chunk_size:

            chunks.append(
                DocumentChunk(
                    chunkId=(
                        f"{document_type}_"
                        f"{chunk_counter}"
                    ),
                    documentType=document_type,
                    text=text,
                    page=page,
                    paragraphIndex=(
                        paragraph_index
                    ),
                )
            )

            chunk_counter += 1

            continue

        # 긴 텍스트는 일정 길이로 분할
        start = 0

        while start < len(text):

            end = start + chunk_size

            chunk_text = text[
                start:end
            ].strip()

            if chunk_text:

                chunks.append(
                    DocumentChunk(
                        chunkId=(
                            f"{document_type}_"
                            f"{chunk_counter}"
                        ),
                        documentType=(
                            document_type
                        ),
                        text=chunk_text,
                        page=page,
                        paragraphIndex=(
                            paragraph_index
                        ),
                    )
                )

                chunk_counter += 1

            start += (
                chunk_size
                - overlap
            )

    return chunks