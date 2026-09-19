from fastapi import FastAPI

app = FastAPI()

@app.get("/")
def home():
    return {"message": "Careerpulse ai 서버 실행 성공"}