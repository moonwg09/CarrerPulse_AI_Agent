from fastapi import FastAPI

from routers.document_router import router as document_router

from routers.career_router import router as career_router


app = FastAPI()

app.include_router(document_router)

app.include_router(career_router)

@app.get("/")
def home():
    return {
        "message": "Careerpulse ai 서버 실행 성공"
    }