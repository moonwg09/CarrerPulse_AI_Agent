"""PLN-01~03: 수집 방식과 독립적인 학습 분석 규칙.

공고 원문을 임의의 키워드로 추측하지 않는다. 수집/AI 분석 단계가 만든
구조화된 requirements를 입력받으므로 자체 데이터와 향후 크롤링 데이터에
똑같은 집계 및 비교 규칙을 적용할 수 있다. 이 모듈 자체는 외부 API를 쓰지 않는다.
"""
from datetime import date
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


class StrictModel(BaseModel):
    # 필드 오타를 조용히 무시하면 경험이 누락되어 잘못된 분석으로 이어진다.
    model_config = ConfigDict(extra="forbid")


class Requirement(StrictModel):
    name: str = Field(min_length=1, max_length=200)
    type: Literal["SKILL", "CERTIFICATE", "EXPERIENCE"]
    category: Literal["REQUIRED", "PREFERRED"]

    @field_validator("name")
    @classmethod
    def clean_name(cls, value):
        value = " ".join(value.split())
        if not value:
            raise ValueError("역량명은 비어 있을 수 없습니다.")
        return value


class Assessment(StrictModel):
    name: str = Field(min_length=1, max_length=200)
    type: Literal["SKILL", "CERTIFICATE", "EXPERIENCE"]
    status: Literal["EXPERIENCED", "PARTIAL", "NO_EXPERIENCE", "UNKNOWN"]
    # PARTIAL은 원래 역량 전체가 아니라 명시한 세부 항목만 준비 대상으로 삼는다.
    missingDetails: list[str] = Field(default_factory=list, max_length=50)
    evidence: str = Field(default="", max_length=4000)

    @model_validator(mode="after")
    def validate_evidence(self):
        self.name = " ".join(self.name.split())
        self.missingDetails = list(dict.fromkeys(x.strip() for x in self.missingDetails if x.strip()))
        if not self.name:
            raise ValueError("역량명이 필요합니다.")
        # 기록이 없다는 이유로 NO_EXPERIENCE를 만들지 않는다. 명시적인 비교 근거가 필요하다.
        if self.status != "UNKNOWN" and not self.evidence.strip():
            raise ValueError("확정한 경험 상태에는 근거 또는 본인 확인 내용이 필요합니다.")
        return self


class Posting(StrictModel):
    jobId: str
    jobCategory: str
    postedDate: date
    collectedAt: str = ""
    deadline: date | None = None
    status: Literal["OPEN", "CLOSED"] = "OPEN"
    requirements: list[Requirement] = Field(max_length=300)


class AnalysisRequest(StrictModel):
    jobs: list[Posting] = Field(max_length=1000)
    interests: list[str] = Field(max_length=30)
    assessments: list[Assessment] = Field(default_factory=list, max_length=500)
    referenceDate: date

    @model_validator(mode="after")
    def unique_inputs(self):
        if len({job.jobId for job in self.jobs}) != len(self.jobs):
            raise ValueError("공고 ID는 중복될 수 없습니다.")
        keys = [(x.type, x.name.casefold()) for x in self.assessments]
        if len(set(keys)) != len(keys):
            raise ValueError("같은 역량의 사용자 상태를 중복 지정할 수 없습니다.")
        return self


def analyze(request: AnalysisRequest) -> dict:
    # PLN-01: 기간 하한은 두지 않는다. 모집 중이며 마감되지 않은 관심 직무 공고만
    # 최신순으로 최대 10개 선택한다. 부족한 공고 수를 가상의 공고로 채우지 않는다.
    jobs = [j for j in request.jobs if j.jobCategory in request.interests
            and j.status == "OPEN" and (j.deadline is None or j.deadline >= request.referenceDate)]
    jobs.sort(key=lambda j: (j.postedDate, j.collectedAt,
                            int(j.jobId) if j.jobId.isdigit() else 0, j.jobId), reverse=True)
    jobs = jobs[:10]
    result = {"referenceDate": request.referenceDate.isoformat(), "targetJobCount": len(jobs),
              "targetJobIds": [j.jobId for j in jobs], "frequency": [], "preparation": [],
              "needsConfirmation": [], "excluded": [],
              "message": "분석 완료" if jobs else "관심 직무의 모집 중 공고가 없어 분석할 수 없습니다."}
    frequency = {}
    for job in jobs:
        # 한 공고에서 같은 역량이 여러 번 나와도 1회이다. 필수/우대가 겹치면 필수로 센다.
        unique = {}
        for req in job.requirements:
            key = (req.type, req.name.casefold())
            if key not in unique or req.category == "REQUIRED":
                unique[key] = req
        for key, req in unique.items():
            row = frequency.setdefault(key, {"name": req.name, "type": req.type,
                "totalJobCount": 0, "requiredCount": 0, "preferredCount": 0, "jobIds": []})
            row["totalJobCount"] += 1
            row["requiredCount" if req.category == "REQUIRED" else "preferredCount"] += 1
            row["jobIds"].append(job.jobId)
    # PLN-03의 동일 빈도 처리 기준은 SRS에서 미정이다. 이름/종류순은 표시 안정성만
    # 위한 기준이며 같은 빈도의 항목에는 같은 순위를 부여해 추가 우선순위를 주장하지 않는다.
    rows = sorted(frequency.values(), key=lambda r: (-r["totalJobCount"], r["name"].casefold(), r["type"]))
    result["frequency"] = rows
    states = {(a.type, a.name.casefold()): a for a in request.assessments}
    for row in rows:
        assessment = states.get((row["type"], row["name"].casefold()))
        state = assessment.status if assessment else "UNKNOWN"
        entry = dict(row, userStatus=state, evidence=assessment.evidence if assessment else "")
        # PLN-02: 충분한 경험은 제외. 미기록/일부 경험의 세부 항목 미확인은 서류 보완으로 분리.
        if state == "EXPERIENCED":
            result["excluded"].append(dict(entry, reason="충분한 경험 또는 보유 확인"))
        elif state == "UNKNOWN" or (state == "PARTIAL" and not assessment.missingDetails):
            result["needsConfirmation"].append(dict(entry, preparationType="DOCUMENT",
                reason="경험·보유 여부 또는 부족한 세부 항목을 확인하고 서류를 보완하세요."))
        else:
            kind = {"SKILL": "TECHNICAL", "CERTIFICATE": "CERTIFICATE", "EXPERIENCE": "PROJECT"}[row["type"]]
            result["preparation"].append(dict(entry, preparationType=kind,
                targets=assessment.missingDetails if state == "PARTIAL" else [row["name"]]))
    # 제외·확인 항목을 제거한 실제 준비 대상 사이에서 경쟁 순위를 계산한다.
    for entry in result["preparation"]:
        entry["rank"] = 1 + sum(x["totalJobCount"] > entry["totalJobCount"] for x in result["preparation"])
    return result
