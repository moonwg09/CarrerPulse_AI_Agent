"""요청·응답 DTO 정의.

Spring(백엔드)과 주고받는 JSON 형식을 여기서 고정한다.
필드를 바꾸면 백엔드 연동 코드도 함께 바뀌므로, 변경은 팀 합의 후에 한다.
"""
from typing import List, Literal, Optional

from pydantic import BaseModel, Field

# 경험 상태 4단계 (SRS CMP-02). 이 네 가지 외의 값은 사용하지 않는다.
MatchStatus = Literal["경험 있음", "일부만 있음", "기록 부족", "경험 없음"]
DocType = Literal["이력서", "자기소개서", "포트폴리오"]
TriggerType = Literal["user_request", "schedule", "condition"]


class IndexDocumentRequest(BaseModel):
    """서류 색인 요청. 파일 추출(PAR-01)은 Spring이 수행하고 텍스트만 보낸다."""
    user_id: int
    document_id: int
    doc_type: DocType
    text: str = Field(min_length=1, description="추출된 서류 전문")
    consented: bool = Field(default=True, description="자료 이용 동의 여부 (DOC-01)")


class IndexDocumentResponse(BaseModel):
    document_id: int
    evidence_ids: List[str]
    evidence_count: int
    embedding_mode: str


class DeleteDocumentResponse(BaseModel):
    """DOC-09: 삭제 요청 처리 결과."""
    document_id: int
    deleted_evidence_count: int


class Requirement(BaseModel):
    """공고 요구 항목 하나 (5번 영역 REQ 분석 결과)."""
    requirement_id: str
    type: Literal["필수", "우대"]
    name: str
    text: str = Field(description="공고 원문 표현")
    needed_experience: str = Field(description="필요한 수행 경험 설명 (REQ-05)")


class EvidenceRef(BaseModel):
    """판정 근거. 화면의 '비교 근거 조회'(CMP-05)에 그대로 사용한다."""
    evidence_id: str
    doc_type: str
    section: str
    source_location: str = Field(description="원문 위치. 예: p.2 (PAR-07)")
    excerpt: str
    score: float


class ComparisonResult(BaseModel):
    """요구 항목별 비교 결과 (ERD EXPERIENCE_COMPARISONS 대응)."""
    requirement_id: str
    match_status: MatchStatus
    match_score: float = Field(description="검색 점수. 합격 가능성이 아니다")
    analysis_reason: str
    missing_part: str = ""
    evidences: List[EvidenceRef] = []


class CompareRequest(BaseModel):
    user_id: int
    job_posting_id: int
    requirements: List[Requirement]


class CompareResponse(BaseModel):
    user_id: int
    job_posting_id: int
    results: List[ComparisonResult]
    matched_count: int = Field(description="'경험 있음' 개수 (REC-02)")
    total_count: int
    match_rate: float
    recommended: bool = Field(description="일치율 50% 이상 여부 (REC-01)")


class GuideRequest(BaseModel):
    user_id: int
    job_posting_id: int
    requirements: List[Requirement]
    trigger_type: TriggerType = "user_request"
    run_key: Optional[str] = Field(
        default=None, description="중복 실행 방지 키 (AGT-01). 없으면 서버가 생성")


class GuideLine(BaseModel):
    text: str
    cited: List[str] = Field(description="인용한 근거 ID (GDE-09 검증 통과분)")


class DroppedLine(BaseModel):
    """검증에서 제외된 문장과 사유. 개발·점검용으로 함께 돌려준다."""
    text: str
    reason: str


class GuideResponse(BaseModel):
    user_id: int
    job_posting_id: int
    status: Literal["제공", "보류", "대상 없음", "중단"]
    requirement_id: Optional[str] = None
    lines: List[GuideLine] = []
    dropped: List[DroppedLine] = []
    reason: str = ""
    plan: List[str] = Field(default=[], description="Agent가 실행한 Tool 순서 (AGT-02)")


class ErrorResponse(BaseModel):
    """실패 응답. 실패를 정상 결과처럼 넘기지 않는다 (AGT-05)."""
    code: str
    message: str
