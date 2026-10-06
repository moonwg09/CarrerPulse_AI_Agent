"""구조화된 분석 결과(JSON)를 근거 단위로 바꾼다 (SRS PAR-02~04, RAG-01).

서류가 이미 항목별로 분석된 JSON을 받아 검색 가능한 근거 덩어리로 만든다.
PDF·DOCX를 직접 여는 경로(parser → chunker)와 입력만 다를 뿐,
이후 단계(근거 생성 → 임베딩 → 검색 → 판정)는 완전히 같은 것을 쓴다.
"""
from typing import Dict, List, Optional

# 분석 JSON의 source 값을 프로젝트에서 쓰는 서류 종류 이름으로 바꾼다.
# 이름이 어긋나면 검색 결과의 doc_type이 화면 표기와 달라진다.
_DOC_TYPE = {"RESUME": "이력서", "SELF_INTRO": "자기소개서", "PORTFOLIO": "포트폴리오"}


def _first_evidence(item: Dict) -> Optional[Dict]:
    """항목에 붙은 근거 중 첫 번째를 돌려준다(위치 정보 기준으로 쓴다)."""
    ev = item.get("evidence") or []
    return ev[0] if ev else None


def _meta(item: Dict) -> Dict:
    """항목의 서류 종류와 원문 위치를 뽑는다 (PAR-07).

    JSON에는 page가 비어 있고 paragraphIndex만 들어 있으므로 문단 번호를 위치로 쓴다.
    """
    ev = _first_evidence(item)
    if not ev:
        return {"doc_type": "이력서", "page": None, "para": None}
    return {
        "doc_type": _DOC_TYPE.get(ev.get("source"), "이력서"),
        "page": ev.get("page"),
        "para": ev.get("paragraphIndex"),
    }


def _chunk(section: str, lines: List[str], meta: Dict) -> Optional[Dict]:
    """근거 한 덩어리를 만든다. 내용이 비면 만들지 않는다."""
    text = "\n".join(s for s in lines if s)
    if not text.strip():
        return None
    return {"doc_type": meta["doc_type"], "section": section,
            "text": text, "page": meta["page"], "para": meta["para"]}


def to_chunks(data: Dict) -> List[Dict]:
    """분석 JSON 전체를 근거 덩어리 목록으로 바꾼다.

    덩어리를 나누는 기준은 '수행 범위를 판단할 수 있는 최소 단위'다 (CMP-02).
    프로젝트와 역량은 하나씩 따로 두고, 기술·자격증처럼 짧은 항목은 묶는다.
    너무 잘게 나누면 '어디까지 했는지'가 조각나고, 묶으면 검색 정밀도가 떨어진다.
    """
    chunks: List[Dict] = []

    def add(c):
        if c:
            chunks.append(c)

    # 기본 정보 — 지원 분야·경력 수준·전공. 경력 연수 판정에 쓰인다.
    bp = data.get("basicProfile") or {}
    if bp:
        add(_chunk("기본정보", [
            f"지원 분야: {bp.get('targetJob', '')}",
            f"경력 구분: {bp.get('careerLevel', '')}",
            f"전공: {bp.get('major', '')}",
        ] + [e.get("text", "") for e in (bp.get("evidence") or [])], _meta(bp)))

    # 보유 기술 — 개별 기술은 한 줄짜리라 따로 두면 검색에 걸려도 맥락이 없다.
    # 숙련도와 활용 경험이 함께 있어야 '어디까지 다뤄 봤는지'가 보이므로 한 덩어리로 묶는다.
    skills = data.get("skills") or []
    if skills:
        lines = []
        for s in skills:
            ev = _first_evidence(s)
            lines.append(f"- {s.get('name')} ({s.get('category')}): "
                         f"{ev.get('text', '') if ev else ''}")
        add(_chunk("보유기술", lines, _meta(skills[0])))

    # 프로젝트 — 하나당 한 덩어리. 수행 범위 판정의 핵심 단위다.
    for p in data.get("projects") or []:
        lines = [
            f"프로젝트: {p.get('name')}",
            f"기간: {p.get('period')}  인원: {p.get('teamSize')}  담당: {p.get('role') or '-'}",
            f"사용 기술: {', '.join(p.get('skills') or [])}",
        ]
        lines += [f"수행: {t}" for t in (p.get("tasks") or [])]
        lines += [f"성과: {a}" for a in (p.get("achievements") or [])]
        add(_chunk(f"프로젝트:{p.get('name')}", lines, _meta(p)))

    # 경력 — 샘플에는 비어 있지만 경력자 서류에서는 채워진다.
    for x in data.get("experiences") or []:
        lines = [f"{k}: {v}" for k, v in x.items() if k != "evidence" and v]
        add(_chunk(f"경력:{x.get('company') or x.get('name') or ''}", lines, _meta(x)))

    # 학력·교육 — 개별 항목이 짧아 묶는다. 학력 일치율 판정에 쓰인다.
    edus = data.get("education") or []
    if edus:
        lines = [f"- {e.get('period')} {e.get('institution')} "
                 f"{e.get('major') or e.get('name') or ''} ({e.get('type')})"
                 for e in edus]
        add(_chunk("학력교육", lines, _meta(edus[0])))

    # 자격증 — 취득일과 발급기관까지 한 덩어리로 둔다.
    certs = data.get("certificates") or []
    if certs:
        lines = [f"- {c.get('issueDate')} {c.get('name')} "
                 f"{c.get('issuer') or ''} {c.get('status') or ''}".strip()
                 for c in certs]
        add(_chunk("자격증", lines, _meta(certs[0])))

    # 역량 — 하나당 한 덩어리. 근거 문장이 자기소개서에서 오므로 출처가 달라진다.
    for c in data.get("competencies") or []:
        lines = [f"역량: {c.get('name')}", c.get("description") or ""]
        lines += [e.get("text", "") for e in (c.get("evidence") or [])]
        add(_chunk(f"역량:{c.get('name')}", lines, _meta(c)))

    # 자기소개서 문항 — 문항 하나가 한 덩어리. 작성 방향 생성(GDE-08)의 대상이 된다.
    for s in data.get("coverLetterSections") or []:
        lines = [f"문항: {s.get('title')}", s.get("summary") or ""]
        lines += [e.get("text", "") for e in (s.get("evidence") or [])]
        add(_chunk(f"자기소개서:{s.get('title')}", lines, _meta(s)))

    return chunks