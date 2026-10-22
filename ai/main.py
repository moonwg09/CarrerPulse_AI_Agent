from fastapi import FastAPI

from routers.document_router import router as document_router

from routers.career_router import router as career_router
from routers.job_router import router as job_router
from routers.rag_document_router import router as rag_document_router


app = FastAPI()

app.include_router(document_router)

app.include_router(career_router)

app.include_router(job_router)

app.include_router(rag_document_router)

@app.get("/")
def home():
    return {
        "message": "Careerpulse ai 서버 실행 성공"
    }
