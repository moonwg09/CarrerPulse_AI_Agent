"""오프라인 분석 서버: python -m uvicorn learning_main:app --port 8000

기존 main은 시작 시 Gemini/RAG 의존성을 불러오므로 API 키 없는 자체 데이터
검증을 위해 독립 진입점도 제공한다. 실제 서버 main에도 같은 router를 등록한다.
"""
from fastapi import FastAPI
from routers.learning_router import router

app = FastAPI(title="CareerPulse PLN-01~03")
app.include_router(router)
