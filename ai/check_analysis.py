# 분석 JSON이 RAG 경로를 제대로 타는지 확인하는 점검용 스크립트.
# 색인 → 검색 → 판정까지 한 번에 돌려서, 근거가 제대로 붙는지 눈으로 본다.
import json
import sys

from rag import store
from rag.processor import analysis_to_chunks
from agent.tools import compare_requirement

path = sys.argv[1]
with open(path, encoding="utf-8") as f:
    data = json.load(f)

# 1) 어떤 덩어리로 나뉘는지 먼저 본다. 여기서 이상하면 뒤는 볼 필요가 없다.
chunks = analysis_to_chunks(data)
print(f"청크 수: {len(chunks)}")
for i, c in enumerate(chunks, 1):
    head = c["text"][:60].replace("\n", " ")
    print(f"{i:>2}. [{c['section']}] ({c['doc_type']}) 문단{c['para']} | {head}")

# 2) 색인한다. 임베딩 모델 적재에 시간이 좀 걸린다.
print("\n색인 중...")
result = store.index_analysis(user_id=2, document_id=2, data=data)
print(f"근거 수: {len(result['evidence_ids'])}, 방식: {result['extraction_method']}")

# 요구 항목은 원래 공고 분석 결과로 들어오는 값이다(REQ-05).
# 시험할 때는 JSON 파일로 바꿔 끼울 수 있게 해서, 코드를 고치지 않아도 되게 한다.
# 두 번째 인자로 파일 경로를 주면 그것을 쓰고, 없으면 아래 기본값을 쓴다.
DEFAULT_REQS = [
    {"requirement_id": "R1", "type": "필수", "name": "Spring 기반 REST API 개발",
     "text": "Java/Spring으로 REST API를 설계·구현한 경험",
     "needed_experience": "인증·권한 처리와 CRUD API 구현"},
    {"requirement_id": "R2", "type": "우대", "name": "컨테이너 기반 배포",
     "text": "Docker 기반 배포 환경 구성 경험",
     "needed_experience": "이미지 빌드와 배포 자동화"},
    {"requirement_id": "R3", "type": "필수", "name": "대용량 트래픽 처리",
     "text": "대규모 트래픽 환경에서의 성능 튜닝 경험",
     "needed_experience": "캐시·부하분산 설계와 병목 개선"},
]

if len(sys.argv) > 2:
    with open(sys.argv[2], encoding="utf-8") as f:
        REQS = json.load(f)
    print(f"\n요구 항목: {sys.argv[2]} ({len(REQS)}건)")
else:
    REQS = DEFAULT_REQS
    print(f"\n요구 항목: 기본값 ({len(REQS)}건)")

s = store.get_store()
print("\n판정 결과")
print("-" * 60)
for r in REQS:
    out = compare_requirement(s, r, user_id=2)
    print(f"[{r['requirement_id']}] {r['name']}")
    print(f"  상태: {out['match_status']}  점수: {out['match_score']:.2f}")
    print(f"  근거: {out['cited']}")
    print(f"  사유: {out['analysis_reason']}")
    print(f"  부족: {out['missing_part']}")
    print()