# 청킹이 실제 문서에서 어떻게 잘리는지 눈으로 확인하는 점검용 스크립트.
# 운영 코드가 아니라 개발 중 확인용이므로, 결과를 보고 나면 지워도 된다.
import sys
from rag.parser import extract
from rag.processor import split_sections

path = sys.argv[1]
doc_type = sys.argv[2] if len(sys.argv) > 2 else "이력서"

pages, method = extract(path, path)
chunks = split_sections(pages, doc_type)

print("추출 방식:", method)
print("페이지 수:", len(pages))
print("청크 수  :", len(chunks))
print("-" * 60)
for i, c in enumerate(chunks, 1):
    section = c.get("section") or "(섹션 없음)"
    page = c.get("page")
    head = c.get("text", "")[:70].replace("\n", " ")
    print(f"{i:>2}. [{section}] p.{page} | {head}")