"""실습·테스트용 샘플.

실제 서비스에서는 사용자가 올린 파일(parser 결과)과 사람인 API 응답으로 대체된다.
자료의 '민수' 사례를 그대로 사용해, 판정 결과를 눈으로 검증할 수 있게 했다.
"""

SAMPLE_RESUME = """
[기술]
Java, Spring Boot, MySQL, Git

[프로젝트]
중고거래 웹 서비스 (2026.03 ~ 2026.05, 팀 4명)
- 상품 API 개발 담당. 상품 등록·조회·수정·삭제 엔드포인트 4종을 직접 구현했습니다.
- 상품 테이블과 카테고리 테이블 설계에 참여했습니다.
- 프론트엔드 화면 연동 테스트를 함께 진행했습니다.

[교육]
AI 소프트웨어 개발 과정 수료 예정 (2026.12)
"""

SAMPLE_PORTFOLIO = """
[프로젝트]
중고거래 웹 서비스
- 담당: 상품 도메인 백엔드
- 사용 기술: Java, Spring Boot, JPA, MySQL
- 수행 내용: REST API 설계 및 구현, 상품 검색 기능 구현
- 팀 전체 작업: 배포는 다른 팀원이 담당했습니다.
"""

# 5번 영역(REQ)의 공고 요구사항 분석 결과를 가정한 입력.
# needed_experience(REQ-05)는 검색 질의문을 만들 때 함께 사용한다.
JOB_REQUIREMENTS = [
    {"requirement_id": "R1", "type": "필수", "name": "웹 서비스 API 개발",
     "text": "웹 서비스 API 개발",
     "needed_experience": "API 엔드포인트를 직접 설계하고 구현한 경험"},
    {"requirement_id": "R2", "type": "필수", "name": "DB 설계·쿼리 작성",
     "text": "DB 설계 및 쿼리 작성",
     "needed_experience": "테이블을 설계하고 조회 쿼리를 직접 작성한 경험"},
    {"requirement_id": "R3", "type": "우대", "name": "Docker·AWS 배포",
     "text": "Docker, AWS 배포 경험 우대",
     "needed_experience": "이미지를 만들어 서버에 올리고 실행 로그를 확인한 경험"},
]
