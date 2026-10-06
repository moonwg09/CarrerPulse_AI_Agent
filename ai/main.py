from fastapi import FastAPI

from routers import analyze, agent      # agent 추

app = FastAPI(title="CareerPulse AI", version="0.1.0")

# 분석 API 등록. 경로는 /analyze/... 로 붙는다.
app.include_router(analyze.router)
app.include_router(agent.router)        # 추가

@app.get("/")
def home():
    return {"message": "Careerpulse ai 서버 실행 성공"}
