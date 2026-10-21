"""항목 단위 분할 (SRS PAR-02~04)."""
import re
from typing import List, Dict, Optional

from .cleaner import clean_text

# 제목 줄을 알아보는 방법은 두 가지다.
# (1) 머리표가 붙은 줄: "■ 수상내역", "[기술]", "1. 학력"
#     머리표 뒤 30자 이내이고 그 줄이 거기서 끝나야 제목으로 본다.
#     표 머리글("■ 수상내역  시기  상세 내용")처럼 뒤에 내용이 더 붙으면 본문으로 둔다.
SECTION_PATTERN = re.compile(
    r"^\s*(?:[\[■◆●▪▶◇□○※]\s*|\d{1,2}\s*[.)]\s+)"
    r"(?P<name>[가-힣A-Za-z][^\]\n]{0,29}?)\s*\]?\s*$"
)

# (2) 머리표 없이 제목만 있는 줄: "경력기술서", "지원 동기"
#     아무 짧은 줄이나 제목으로 보면 본문이 잘게 부서지므로,
#     서류에서 실제로 쓰이는 제목만 아래 목록으로 한정한다.
KNOWN_HEADINGS = {
    "이력서", "자기소개서", "포트폴리오",
    "인적사항", "학력", "학력사항", "경력", "경력사항", "경력기술",
    "수상내역", "수상경력", "자격증", "자격사항", "교육이수", "교육사항",
    "보유기술", "기술스택", "프로젝트", "프로젝트이력", "병역", "병역사항",
    "지원동기", "입사후포부", "성장과정", "성격의장단점", "직무역량",
}

MAX_CHARS = 1200  # 한 항목이 지나치게 길 때만 문단 경계로 한 번 더 나눈다

_SPACES = re.compile(r"\s+")


def _squeeze(text: str) -> str:
    """공백을 모두 지운다.

    PDF가 자간을 벌려 놓으면 "이 력 서"처럼 글자 사이에 공백이 들어온다.
    이 상태로는 "이력서"와 문자열 비교가 되지 않으므로, 제목 대조 전에 공백을 턴다.
    """
    return _SPACES.sub("", text)


def _section_title(line: str) -> Optional[str]:
    """이 줄이 제목이면 제목 이름을, 아니면 None을 돌려준다."""
    stripped = line.strip()
    if not stripped:
        return None

    # 머리표가 붙은 제목
    m = SECTION_PATTERN.match(stripped)
    if m:
        name = _squeeze(m.group("name"))
        if name:
            return name

    # 머리표 없는 알려진 제목
    key = _squeeze(stripped)
    if key in KNOWN_HEADINGS:
        return key

    return None


def _split_long(text: str) -> List[str]:
    """너무 긴 항목을 문단 경계에서 나눈다(문장 중간에서 자르지 않는다)."""
    if len(text) <= MAX_CHARS:
        return [text]

    parts, buf = [], []
    for para in text.split("\n\n"):
        if sum(len(p) for p in buf) + len(para) > MAX_CHARS and buf:
            parts.append("\n\n".join(buf))
            buf = []
        buf.append(para)
    if buf:
        parts.append("\n\n".join(buf))
    return parts


def merge_paragraphs(pages: List[Dict]) -> List[Dict]:
    """DOCX처럼 문단 단위로 들어온 입력을 한 덩어리로 합친다.

    문단마다 따로 자르면 제목과 본문이 분리되어 항목 분할이 되지 않는다.
    위치 정보는 각 항목의 첫 문단 번호를 쓰기 위해 줄 단위로 보존한다.
    """
    if not pages or pages[0].get("para") is None:
        return pages
    merged, buf, first_para = [], [], None
    for p in pages:
        if first_para is None:
            first_para = p.get("para")
        buf.append(p["text"])
    merged.append({"page": None, "para": first_para, "text": "\n".join(buf)})
    return merged


def split_sections(pages: List[Dict], doc_type: str) -> List[Dict]:
    """제목을 기준으로 항목을 나눈다.

    글자 수로 자르지 않는 이유: 항목이 중간에서 잘리면 '어디까지 수행했는지'가
    서로 다른 조각으로 흩어져 CMP-02의 수행 범위 판정이 불가능해진다.
    """
    chunks: List[Dict] = []
    current = "미분류"   # 제목이 나오기 전 내용은 미분류로 둔다
    buf: List[str] = []

    # 모아 둔 줄을 하나의 항목으로 확정한다. 제목을 만났을 때와 페이지가 끝날 때 호출한다.
    def flush(page, para):
        nonlocal buf
        if buf:
            body = clean_text("\n".join(buf))
            for part in _split_long(body):
                if part.strip():
                    chunks.append({"doc_type": doc_type, "section": current,
                                   "text": part, "page": page, "para": para})
        buf = []

    # 한 줄씩 읽어 제목이면 항목을 바꾸고, 아니면 본문으로 모은다.
    # PDF는 page, DOCX는 para 가 위치 정보로 들어온다(PAR-07).
    for p in pages:
        page_no, para_no = p.get("page"), p.get("para")
        for line in p["text"].splitlines():
            title = _section_title(line)
            if title:
                flush(page_no, para_no)
                current = title
            else:
                buf.append(line)
        flush(page_no, para_no)

    return chunks