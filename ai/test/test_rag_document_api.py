from io import BytesIO
from unittest.mock import patch

from fastapi import HTTPException, UploadFile

from routers.rag_document_router import (
    delete_rag_document,
    index_rag_document,
)
from schemas.document_schema import DocumentType


def test_indexes_document_with_spring_contract():
    upload = UploadFile(
        filename="resume.pdf",
        file=BytesIO(b"%PDF-1.4 test"),
    )

    with patch(
        "routers.rag_document_router.index_file",
        return_value={
            "evidence_ids": ["EV-0001", "EV-0002"],
            "extraction_method": "pymupdf",
            "page_count": 1,
            "extracted_text": "FastAPI 개발 경험",
        },
    ) as mocked:
        response = index_rag_document(
            document_id=41,
            file=upload,
            userId=7,
            documentType=DocumentType.RESUME,
            consented=True,
        )

    assert response.documentId == 41
    assert response.documentType == DocumentType.RESUME
    assert response.evidenceCount == 2
    assert response.extractedText == "FastAPI 개발 경험"
    mocked.assert_called_once_with(
        user_id=7,
        document_id=41,
        doc_type="resume",
        source=b"%PDF-1.4 test",
        filename="resume.pdf",
        consented=True,
        size_bytes=13,
    )


def test_deletes_document_with_owner_id():
    with patch(
        "routers.rag_document_router.delete_document",
        return_value=3,
    ) as mocked:
        response = delete_rag_document(document_id=41, userId=7)

    assert response.documentId == 41
    assert response.deletedEvidenceCount == 3
    mocked.assert_called_once_with(user_id=7, document_id=41)


def test_rejects_invalid_user_id():
    upload = UploadFile(
        filename="resume.pdf",
        file=BytesIO(b"%PDF-1.4 test"),
    )

    try:
        index_rag_document(
            document_id=41,
            file=upload,
            userId=0,
            documentType=DocumentType.RESUME,
            consented=True,
        )
    except HTTPException as exception:
        assert exception.status_code == 400
    else:
        raise AssertionError("잘못된 userId가 거부되지 않았습니다.")


if __name__ == "__main__":
    test_indexes_document_with_spring_contract()
    test_deletes_document_with_owner_id()
    test_rejects_invalid_user_id()
    print("RAG DOCUMENT API TEST: PASS")
