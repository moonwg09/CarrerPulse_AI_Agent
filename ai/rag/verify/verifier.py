"""생성 결과 근거 검증 (SRS GDE-09)."""
import re
from typing import List, Tuple

MIN_OVERLAP = 0.2     # 근거 원문과 겹쳐야 하는 최소 비율 (SRS 미정 — 팀 확정 필요)
MAX_REGENERATE = 1    # 재생성 횟수 (SRS 미정 — 팀 확정 필요)

EV_RE = re.compile(r"EV-\d{4}")


def verify_guide(text: str, hits, min_overlap: float = MIN_OVERLAP
                 ) -> Tuple[List[str], List[Tuple[str, str]]]:
    """생성 문장을 근거와 대조해 통과·제거로 나눈다. 반환: (통과 문장, [(제거 문장, 사유)])"""
    ev_map = {e.evidence_id: e.evidence_text for _, e in hits}
    kept: List[str] = []
    dropped: List[Tuple[str, str]] = []

    for line in [l.strip() for l in text.splitlines() if l.strip()]:
        # 1차: 인용이 없거나, 검색되지 않은 근거를 인용한 문장은 바로 제거한다.
        #      "Docker 배포 경험을 강조하세요" 같은 지어낸 문장이 여기서 걸러진다.
        ids = EV_RE.findall(line)
        if not ids:
            dropped.append((line, "인용 없음"))
            continue
        if any(i not in ev_map for i in ids):
            dropped.append((line, "존재하지 않는 근거 인용"))
            continue

        # 2차: 인용은 달았지만 내용이 근거와 무관할 수 있으므로 원문과 겹치는 정도를 본다.
        body = EV_RE.sub("", line)
        words = re.findall(r"[가-힣A-Za-z]{2,}", body)
        src = " ".join(ev_map[i] for i in ids)
        overlap = sum(1 for w in words if w in src) / (len(words) or 1)
        if overlap >= min_overlap:
            kept.append(line)
        else:
            dropped.append((line, f"근거 대조 실패({overlap:.2f})"))

    return kept, dropped


def generate_and_verify(build_prompt_fn, call_fn, hits, max_retry: int = MAX_REGENERATE):
    """생성 → 검증 → (실패 시) 재생성 → 그래도 실패하면 보류한다."""
    # SRS는 "검증에 실패하면 해당 결과 제공을 보류하고 안내한다"고 정했다.
    # 통과 문장이 하나도 없으면 사용자에게 보여 주지 않는다.
    attempts = []
    for _ in range(max_retry + 1):
        text = call_fn(build_prompt_fn())
        kept, dropped = verify_guide(text, hits)
        attempts.append({"text": text, "kept": kept, "dropped": dropped})
        if kept:
            return {"status": "제공", "lines": kept, "attempts": attempts}
    return {"status": "보류", "lines": [], "attempts": attempts}
