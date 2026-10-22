"""학습 분석 전용 진입점. 자체 데이터 테스트에 Gemini 키가 필요하지 않다."""
from fastapi import APIRouter, HTTPException
from services.period_learning_plan import PeriodRequest, generate_period_plan, GenerationUnavailable
from services.learning_analysis import AnalysisRequest, analyze

router = APIRouter(prefix="/api/v1/learning", tags=["learning"])


@router.post("/analyze")
def analyze_learning(request: AnalysisRequest):
    return analyze(request)


@router.post("/plans/generate")
def generate_plan(request: PeriodRequest):
    try:
        return generate_period_plan(request)
    except GenerationUnavailable as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
