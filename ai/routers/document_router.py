from pathlib import Path
from typing import Optional
from google.genai import errors
import shutil
import uuid

from fastapi import APIRouter, File, HTTPException, UploadFile

from schemas.document_schema import DocumentAnalysisResult
from services.document_analyzer import analyze_documents
from services.document_parser import extract_document


router = APIRouter(
    prefix="/api/v1/documents",
    tags=["documents"]
)


TEMP_DIR = Path("temp_uploads")
TEMP_DIR.mkdir(exist_ok=True)


def save_temp_file(file: UploadFile) -> Path:
    extension = Path(file.filename).suffix.lower()

    if extension not in [".pdf", ".docx"]:
        raise HTTPException(
            status_code=400,
            detail="PDF 또는 DOCX 파일만 업로드할 수 있습니다."
        )

    temp_filename = f"{uuid.uuid4()}{extension}"
    temp_path = TEMP_DIR / temp_filename

    with open(temp_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    return temp_path


@router.post(
    "/analyze",
    response_model=DocumentAnalysisResult
)
def analyze_document_files(
    resume: UploadFile = File(...),
    selfIntro: UploadFile = File(...),
    portfolio: Optional[UploadFile] = File(None)
):
    saved_files = []

    try:
        resume_path = save_temp_file(resume)
        saved_files.append(resume_path)

        self_intro_path = save_temp_file(selfIntro)
        saved_files.append(self_intro_path)

        resume_contents = extract_document(
            str(resume_path)
        )

        self_intro_contents = extract_document(
            str(self_intro_path)
        )

        portfolio_contents = None

        if portfolio is not None:
            portfolio_path = save_temp_file(portfolio)
            saved_files.append(portfolio_path)

            portfolio_contents = extract_document(
                str(portfolio_path)
            )

        result = analyze_documents(
            resume_contents=resume_contents,
            self_intro_contents=self_intro_contents,
            portfolio_contents=portfolio_contents
        )

        return result

    except errors.ServerError as e:
        raise HTTPException(
            status_code=503,
            detail="AI 분석 서버가 현재 혼잡합니다. 잠시 후 다시 시도해주세요"
        )

    except ValueError as e:
        raise HTTPException(
            status_code=400,
            detail=str(e)
        )

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"문서 분석 중 오류가 발생했습니다: {str(e)}"
        )

    finally:
        for path in saved_files:
            if path.exists():
                path.unlink()