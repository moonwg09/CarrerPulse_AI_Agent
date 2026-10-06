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

# 3) 요구 항목 세 개로 판정해 본다.
#    R1·R2는 근거가 있어야 하고, R3는 '기록 부족'이 나와야 정상이다.
#    R3가 '경험 있음'으로 나오면 없는 경험을 지어낸 것이므로 즉시 잡아야 한다.
REQS = [
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