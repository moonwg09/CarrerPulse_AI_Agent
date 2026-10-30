import shutil
import uuid
from datetime import date
from pathlib import Path
from typing import Optional

from fastapi import APIRouter, File, Form, HTTPException, UploadFile
from google.genai import errors
from pydantic import BaseModel, Field, field_validator

from agents.career_agent import build_career_agent
from schemas.document_schema import DocumentAnalysisResult
from schemas.learning_plan_schema import LearningPlanRequest
from schemas.match_schema import JobMatchResult
from schemas.rag_schema import EmbeddedDocumentChunk
from schemas.recommendation_schema import JobRecommendationItem
from services.document_analyzer import analyze_documents
from services.document_parser import extract_document
from services.learning_candidate_service import build_learning_candidates
from services.learning_plan_generator import generate_learning_plan
from services.learning_priority import calculate_learning_priority
from services.recommendation_service import (
    build_growth_candidate_list,
    build_recommendation_list,
)
from services.requirement_frequency import calculate_requirement_frequency
from services.rag_validation_service import link_match_result_evidence_metadata
from services.gemini_client import GEMINI_MODEL, get_gemini_client
from rag.parser import ParseError
from rag.prompt.templates import build_guide_prompt
from rag.retriever.search import search as search_evidence
from rag.store import embedding_mode, get_scoped_store, get_store, index_file
from rag.verify.verifier import generate_and_verify


TEMP_DIR = Path("temp_uploads")
TEMP_DIR.mkdir(exist_ok=True)

router = APIRouter(
    prefix="/api/v1/career",
    tags=["career"],
)


class CareerJobAnalyzeRequest(BaseModel):
    jobText: str
    userAnalysis: DocumentAnalysisResult
    embeddedChunks: list[EmbeddedDocumentChunk]


class IndexedJobAnalyzeRequest(BaseModel):
    userId: int = Field(gt=0)
    jobId: int = Field(gt=0)
    jobText: str = Field(min_length=1)
    documentIds: list[int] = Field(min_length=1)

    @field_validator("jobText")
    @classmethod
    def validate_job_text(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("jobText는 비어 있을 수 없습니다.")
        return value

    @field_validator("documentIds")
    @classmethod
    def validate_document_ids(cls, values: list[int]) -> list[int]:
        if any(value <= 0 for value in values):
            raise ValueError("documentIds는 모두 1 이상이어야 합니다.")
        if len(values) != len(set(values)):
            raise ValueError("documentIds는 중복될 수 없습니다.")
        return values


class GuidanceRequest(BaseModel):
    userId: int = Field(gt=0)
    documentIds: list[int] = Field(min_length=1)
    requirement: str = Field(min_length=1)
    requirementType: str = Field(min_length=1)
    status: str = Field(min_length=1)
    missingPart: str = ""


class GuidanceResponse(BaseModel):
    status: str
    lines: list[str]
    evidenceCount: int


def save_temp_file(file: UploadFile) -> Path:
    if not file.filename:
        raise HTTPException(
            status_code=400,
            detail="파일 이름을 확인할 수 없습니다.",
        )

    extension = Path(file.filename).suffix.lower()

    if extension not in [".pdf", ".docx"]:
        raise HTTPException(
            status_code=400,
            detail="PDF 또는 DOCX 파일만 업로드할 수 있습니다.",
        )

    temp_filename = f"{uuid.uuid4()}{extension}"
    temp_path = TEMP_DIR / temp_filename

    with open(temp_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    return temp_path


def _indexed_document_contents(rag_store):
    """색인된 원문 근거를 문서 분석기가 받는 위치 포함 형식으로 되돌린다."""
    contents = {
        "resume": [],
        "self_intro": [],
        "portfolio": [],
    }

    for evidence in sorted(
        rag_store.evidences,
        key=lambda item: (item.document_id, item.evidence_id),
    ):
        if evidence.doc_type not in contents:
            continue

        item = {"text": evidence.evidence_text}
        location = evidence.source_location or ""

        if location.startswith("p.") and location[2:].isdigit():
            item["page"] = int(location[2:])
        elif location.startswith("문단 ") and location[3:].isdigit():
            item["paragraphIndex"] = int(location[3:])

        contents[evidence.doc_type].append(item)

    return contents


@router.post("/analyze-job-files")
def analyze_job_with_files(
    jobText1: str = Form(...),
    jobText2: str = Form(...),
    closeDate1: date = Form(...),
    closeDate2: date = Form(...),
    startDate: date = Form(...),
    endDate: date = Form(...),
    weeklyAvailableHours: float = Form(...),
    resume: UploadFile = File(...),
    selfIntro: UploadFile = File(...),
    portfolio: Optional[UploadFile] = File(None),
    resumeDocumentId: int = Form(...),
    selfIntroDocumentId: int = Form(...),
    portfolioDocumentId: Optional[int] = Form(None),
    userId: int = Form(1),
    ragConsent: bool = Form(True),
):
    saved_files: list[Path] = []

    try:
        if userId <= 0:
            raise HTTPException(status_code=400, detail="userId는 1 이상이어야 합니다.")

        document_ids = [resumeDocumentId, selfIntroDocumentId]
        if any(document_id <= 0 for document_id in document_ids):
            raise HTTPException(
                status_code=400,
                detail="문서 ID는 1 이상이어야 합니다.",
            )

        if (portfolio is None) != (portfolioDocumentId is None):
            raise HTTPException(
                status_code=400,
                detail="포트폴리오 파일과 documentId를 함께 전달해야 합니다.",
            )

        if portfolioDocumentId is not None:
            if portfolioDocumentId <= 0:
                raise HTTPException(
                    status_code=400,
                    detail="문서 ID는 1 이상이어야 합니다.",
                )
            document_ids.append(portfolioDocumentId)

        if len(set(document_ids)) != len(document_ids):
            raise HTTPException(
                status_code=400,
                detail="문서별 documentId는 서로 달라야 합니다.",
            )

        print("\n" + "=" * 70)
        print("Career Agent 파일 분석 시작")
        print("=" * 70)

        # 1. 파일 임시 저장
        print("[1] 파일 임시 저장 시작")

        resume_path = save_temp_file(resume)
        saved_files.append(resume_path)

        self_intro_path = save_temp_file(selfIntro)
        saved_files.append(self_intro_path)

        portfolio_path = None
        if portfolio is not None:
            portfolio_path = save_temp_file(portfolio)
            saved_files.append(portfolio_path)

        print("[1] 파일 임시 저장 완료")

        # 2. 문서 파싱
        print("[2] 문서 파싱 시작")

        resume_contents = extract_document(str(resume_path))
        self_intro_contents = extract_document(str(self_intro_path))

        portfolio_contents = None
        if portfolio_path is not None:
            portfolio_contents = extract_document(str(portfolio_path))

        print("[2] 문서 파싱 완료")

        # 3. 사용자 문서 AI 분석
        print("[3] Gemini 사용자 문서 분석 시작")

        user_analysis = analyze_documents(
            resume_contents=resume_contents,
            self_intro_contents=self_intro_contents,
            portfolio_contents=portfolio_contents,
        )

        print("[3] Gemini 사용자 문서 분석 완료")

        # 4~5. RAG v2 Evidence / RetrievalChunk / Embedding 생성
        print("[4-5] RAG v2 문서 색인 시작")

        upload_documents = [
            (resume, resume_path, "resume", resumeDocumentId),
            (selfIntro, self_intro_path, "self_intro", selfIntroDocumentId),
        ]
        if portfolio is not None and portfolio_path is not None:
            upload_documents.append(
                (portfolio, portfolio_path, "portfolio", portfolioDocumentId)
            )

        indexed_documents = []
        for upload, path, document_type, document_id in upload_documents:
            index_result = index_file(
                user_id=userId,
                document_id=document_id,
                doc_type=document_type,
                source=str(path),
                filename=upload.filename or path.name,
                consented=ragConsent,
                size_bytes=path.stat().st_size,
            )
            indexed_documents.append(
                {
                    "documentId": document_id,
                    "documentType": document_type,
                    "evidenceCount": len(index_result["evidence_ids"]),
                    "extractionMethod": index_result["extraction_method"],
                }
            )

        rag_store = get_store()
        print(
            f"[4-5] RAG v2 문서 색인 완료: "
            f"{sum(item['evidenceCount'] for item in indexed_documents)}개 Evidence"
        )

        # 6. Agent 생성
        print("[6] Career Agent 생성 시작")
        career_agent = build_career_agent()
        print("[6] Career Agent 생성 완료")

        # 7. 공고 2개 Agent 실행
        print("[7] Career Agent 실행 시작")

        jobs = [
            {
                "jobId": "job-001",
                "closeDate": closeDate1,
                "text": jobText1,
            },
            {
                "jobId": "job-002",
                "closeDate": closeDate2,
                "text": jobText2,
            },
        ]

        match_results = []
        recommendation_items = []
        analyzed_jobs = []

        for index, job in enumerate(jobs, start=1):
            print()
            print("=" * 70)
            print(f"[7-{index}] 공고 {index} Agent 실행")
            print("=" * 70)
            print(f"[7-{index}] jobText 길이: {len(job['text'])}")

            state = {
                "jobText": job["text"],
                "userAnalysis": user_analysis,
                "embeddedChunks": [],
                "userId": userId,
                "ragStore": rag_store,
                "jobAnalysis": None,
                "matchResult": None,
            }

            final_state = career_agent.invoke(state)

            job_analysis = final_state["jobAnalysis"]
            match_result = final_state["matchResult"]
            match_result = link_match_result_evidence_metadata(
                match_result=match_result,
                rag_store=rag_store,
                user_id=userId,
            )

            match_results.append(match_result)

            analyzed_jobs.append(
                (
                    job["jobId"],
                    job_analysis,
                )
            )

            recommendation_items.append(
                JobRecommendationItem(
                    jobId=job["jobId"],
                    closeDate=job["closeDate"],
                    matchResult=match_result,
                )
            )

            print(f"[7-{index}] 공고 {index} Agent 완료")

        print("[7] Career Agent 전체 실행 완료")

        # 8. 추천 목록 생성
        print("[8] 추천 공고 계산 시작")

        recommendations = build_recommendation_list(recommendation_items)
        growth_candidates = build_growth_candidate_list(recommendation_items)

        print(f"[8] 추천 공고: {len(recommendations)}개")
        print(f"[8] 성장 후보 공고: {len(growth_candidates)}개")

        # 9. 요구 역량 빈도 계산
        print("[9] 요구 역량 빈도 계산 시작")

        frequency_result = calculate_requirement_frequency(analyzed_jobs)

        print("[9] 요구 역량 빈도 계산 완료")

        # 10. 학습 후보 생성
        print("[10] 학습 후보 생성 시작")

        learning_candidates = build_learning_candidates(
            frequency_result=frequency_result,
            jobs=recommendation_items,
        )

        print("[10] 학습 후보 생성 완료")

        # 11. 학습 우선순위 계산
        print("[11] 학습 우선순위 계산 시작")

        learning_priority = calculate_learning_priority(learning_candidates)

        print(f"[11] 학습 우선순위 {len(learning_priority)}개")

        # 12. 학습계획 생성
        learning_plan = None

        if len(learning_priority) > 0:
            print("[12] Gemini 학습계획 생성 시작")

            learning_plan_request = LearningPlanRequest(
                startDate=startDate,
                endDate=endDate,
                weeklyAvailableHours=weeklyAvailableHours,
            )

            learning_plan = generate_learning_plan(
                priority_items=learning_priority,
                request=learning_plan_request,
            )

            print("[12] Gemini 학습계획 생성 완료")
        else:
            print("[12] 학습 대상이 없어 학습계획 생성을 건너뜁니다.")

        # 13. 최종 API 응답
        print()
        print("=" * 70)
        print("Career Pipeline 전체 완료")
        print("=" * 70)
        print(f"MatchResult: {len(match_results)}개")
        print(f"추천 공고: {len(recommendations)}개")
        print(f"성장 후보: {len(growth_candidates)}개")

        return {
            "ragIndex": {
                "embeddingMode": embedding_mode(),
                "documents": indexed_documents,
            },
            "matchResults": match_results,
            "recommendations": recommendations,
            "growthCandidates": growth_candidates,
            "requirementFrequency": frequency_result,
            "learningCandidates": learning_candidates,
            "learningPriority": learning_priority,
            "learningPlan": learning_plan,
        }

    except errors.ServerError as e:
        print("\n[ERROR] Gemini ServerError 발생")
        print(f"[ERROR DETAIL] {str(e)}")

        raise HTTPException(
            status_code=503,
            detail="AI 분석 서버가 현재 혼잡합니다. 잠시 후 다시 시도해주세요.",
        )

    except errors.ClientError as e:
        print("\n[ERROR] Gemini ClientError 발생")
        print(f"[ERROR DETAIL] {str(e)}")

        status_code = (
            getattr(e, "code", None)
            or getattr(e, "status_code", None)
        )

        if status_code == 429:
            raise HTTPException(
                status_code=429,
                detail="AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.",
            )

        raise HTTPException(
            status_code=500,
            detail=str(e),
        )

    except ValueError as e:
        print("\n[ERROR] ValueError 발생")
        print(f"[ERROR DETAIL] {str(e)}")

        raise HTTPException(
            status_code=400,
            detail=str(e),
        )

    except ParseError as e:
        print("\n[ERROR] 문서 파싱 실패")
        print(f"[ERROR DETAIL] {e.code}: {e.message}")

        raise HTTPException(
            status_code=400,
            detail={"code": e.code, "message": e.message},
        )

    except HTTPException:
        raise

    except Exception as e:
        print("\n[ERROR] 알 수 없는 오류 발생")
        print(f"[ERROR TYPE] {type(e).__name__}")
        print(f"[ERROR DETAIL] {str(e)}")

        raise HTTPException(
            status_code=500,
            detail=f"Career Agent 실행 중 오류가 발생했습니다: {str(e)}",
        )

    finally:
        print("[FINALLY] 임시 파일 삭제 시작")

        for path in saved_files:
            if path.exists():
                path.unlink()

        print("[FINALLY] 임시 파일 삭제 완료")


@router.post(
    "/analyze-indexed-job",
    response_model=JobMatchResult,
)
def analyze_indexed_job(request: IndexedJobAnalyzeRequest):
    """Spring이 저장·색인 완료한 사용자 문서로 공고 한 건을 분석한다."""
    try:
        rag_store = get_scoped_store(
            user_id=request.userId,
            document_ids=request.documentIds,
        )
        indexed_document_ids = {
            evidence.document_id
            for evidence in rag_store.evidences
        }
        missing_document_ids = sorted(
            set(request.documentIds) - indexed_document_ids
        )
        if missing_document_ids:
            raise HTTPException(
                status_code=409,
                detail={
                    "code": "RAG_INDEX_NOT_READY",
                    "message": "요청한 문서의 RAG 색인이 없거나 사용할 수 없습니다.",
                    "documentIds": missing_document_ids,
                },
            )

        contents = _indexed_document_contents(rag_store)
        user_analysis = analyze_documents(
            resume_contents=contents["resume"],
            self_intro_contents=contents["self_intro"],
            portfolio_contents=contents["portfolio"],
        )

        career_agent = build_career_agent()
        final_state = career_agent.invoke({
            "jobText": request.jobText,
            "userAnalysis": user_analysis,
            "embeddedChunks": [],
            "userId": request.userId,
            "ragStore": rag_store,
            "jobAnalysis": None,
            "matchResult": None,
        })

        return link_match_result_evidence_metadata(
            match_result=final_state["matchResult"],
            rag_store=rag_store,
            user_id=request.userId,
        )

    except HTTPException:
        raise
    except errors.ServerError as exception:
        raise HTTPException(
            status_code=503,
            detail="AI 분석 서버가 현재 혼잡합니다. 잠시 후 다시 시도해주세요.",
        ) from exception
    except errors.ClientError as exception:
        status_code = (
            getattr(exception, "code", None)
            or getattr(exception, "status_code", None)
        )
        if status_code == 429:
            raise HTTPException(
                status_code=429,
                detail="AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.",
            ) from exception
        raise HTTPException(status_code=502, detail=str(exception)) from exception
    except Exception as exception:
        raise HTTPException(
            status_code=500,
            detail=f"색인 문서 기반 공고 분석 중 오류가 발생했습니다: {exception}",
        ) from exception


@router.post(
    "/guidance",
    response_model=GuidanceResponse,
)
def generate_document_guidance(request: GuidanceRequest):
    """검색된 사용자 근거만 사용해 서류 보완 방향을 생성하고 검증한다."""
    rag_store = get_scoped_store(request.userId, request.documentIds)
    hits = search_evidence(
        store=rag_store,
        query=request.requirement,
        user_id=request.userId,
        top_k=3,
    )
    if not hits:
        raise HTTPException(
            status_code=409,
            detail="관련 사용자 근거가 없어 작성 방향을 생성할 수 없습니다.",
        )

    requirement = {
        "text": request.requirement,
        "type": request.requirementType,
    }
    result = {
        "match_status": request.status,
        "missing_part": request.missingPart,
    }
    client = get_gemini_client()

    def build_prompt():
        return build_guide_prompt(requirement, result, hits)

    def call_model(prompt: str) -> str:
        response = client.models.generate_content(
            model=GEMINI_MODEL,
            contents=prompt,
        )
        text = response.text or ""
        if not text.strip():
            raise RuntimeError("Gemini가 작성 방향을 반환하지 않았습니다.")
        return text

    verified = generate_and_verify(build_prompt, call_model, hits)
    if not verified["lines"]:
        raise HTTPException(
            status_code=422,
            detail="생성 결과의 근거를 검증하지 못해 제공을 보류했습니다.",
        )

    return GuidanceResponse(
        status=verified["status"],
        lines=verified["lines"],
        evidenceCount=len(hits),
    )


@router.post(
    "/analyze-job",
    response_model=JobMatchResult,
)
def analyze_job_with_agent(
    request: CareerJobAnalyzeRequest,
):
    try:
        print("\n" + "=" * 70)
        print("Career Agent JSON 분석 시작")
        print("=" * 70)

        career_agent = build_career_agent()

        state = {
            "jobText": request.jobText,
            "userAnalysis": request.userAnalysis,
            "embeddedChunks": request.embeddedChunks,
            "userId": None,
            "ragStore": None,
            "jobAnalysis": None,
            "matchResult": None,
        }

        print("[Agent] 실행 시작")
        final_state = career_agent.invoke(state)
        print("[Agent] 실행 완료")

        return final_state["matchResult"]

    except Exception as e:
        print(f"[ERROR] {type(e).__name__}: {str(e)}")

        raise HTTPException(
            status_code=500,
            detail=f"공고 Agent 분석 중 오류가 발생했습니다: {str(e)}",
        )
