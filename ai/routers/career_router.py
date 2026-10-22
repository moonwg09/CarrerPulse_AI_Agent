import shutil
import uuid
from datetime import date
from pathlib import Path
from typing import Optional

from fastapi import APIRouter, File, Form, HTTPException, UploadFile
from google.genai import errors
from pydantic import BaseModel

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
from services.rag_chunk_service import create_document_chunks
from services.rag_embedding_service import embed_document_chunks
from services.recommendation_service import (
    build_growth_candidate_list,
    build_recommendation_list,
)
from services.requirement_frequency import calculate_requirement_frequency


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
):
    saved_files: list[Path] = []

    try:
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

        # 4. RAG chunk 생성
        print("[4] RAG chunk 생성 시작")

        resume_chunks = create_document_chunks(
            parsed_items=resume_contents,
            document_type="RESUME",
            chunk_size=700,
            overlap=100,
        )

        self_intro_chunks = create_document_chunks(
            parsed_items=self_intro_contents,
            document_type="SELF_INTRO",
            chunk_size=700,
            overlap=100,
        )

        rag_chunks = resume_chunks + self_intro_chunks

        if portfolio_contents is not None:
            portfolio_chunks = create_document_chunks(
                parsed_items=portfolio_contents,
                document_type="PORTFOLIO",
                chunk_size=700,
                overlap=100,
            )
            rag_chunks += portfolio_chunks

        print(f"[4] RAG chunk 생성 완료: {len(rag_chunks)}개")

        # 5. Embedding 생성
        print("[5] RAG embedding 시작")

        embedded_chunks = embed_document_chunks(rag_chunks)

        print(f"[5] RAG embedding 완료: {len(embedded_chunks)}개")

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
                "embeddedChunks": embedded_chunks,
                "jobAnalysis": None,
                "matchResult": None,
            }

            final_state = career_agent.invoke(state)

            job_analysis = final_state["jobAnalysis"]
            match_result = final_state["matchResult"]

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
