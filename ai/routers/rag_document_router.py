from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from rag.parser import ParseError
from rag.store import delete_document, index_file
from schemas.document_schema import DocumentType
from schemas.rag_schema import (
    RagDocumentDeleteResponse,
    RagDocumentIndexResponse,
)


router = APIRouter(
    prefix="/api/v1/rag/documents",
    tags=["rag-documents"],
)


@router.put(
    "/{document_id}",
    response_model=RagDocumentIndexResponse,
)
def index_rag_document(
    document_id: int,
    file: UploadFile = File(...),
    userId: int = Form(...),
    documentType: DocumentType = Form(...),
    consented: bool = Form(...),
):
    if document_id <= 0:
        raise HTTPException(status_code=400, detail="documentId는 1 이상이어야 합니다.")
    if userId <= 0:
        raise HTTPException(status_code=400, detail="userId는 1 이상이어야 합니다.")
    if not file.filename:
        raise HTTPException(status_code=400, detail="파일 이름을 확인할 수 없습니다.")

    content = file.file.read()

    try:
        result = index_file(
            user_id=userId,
            document_id=document_id,
            doc_type=documentType.value.lower(),
            source=content,
            filename=file.filename,
            consented=consented,
            size_bytes=len(content),
        )
    except ParseError as exception:
        raise HTTPException(
            status_code=400,
            detail={"code": exception.code, "message": exception.message},
        ) from exception

    return RagDocumentIndexResponse(
        documentId=document_id,
        documentType=documentType,
        evidenceCount=len(result["evidence_ids"]),
        extractionMethod=result["extraction_method"],
        pageCount=result["page_count"],
        extractedText=result["extracted_text"],
    )


@router.delete(
    "/{document_id}",
    response_model=RagDocumentDeleteResponse,
)
def delete_rag_document(
    document_id: int,
    userId: int,
):
    if document_id <= 0:
        raise HTTPException(status_code=400, detail="documentId는 1 이상이어야 합니다.")
    if userId <= 0:
        raise HTTPException(status_code=400, detail="userId는 1 이상이어야 합니다.")

    deleted_count = delete_document(
        user_id=userId,
        document_id=document_id,
    )

    return RagDocumentDeleteResponse(
        documentId=document_id,
        deletedEvidenceCount=deleted_count,
    )
