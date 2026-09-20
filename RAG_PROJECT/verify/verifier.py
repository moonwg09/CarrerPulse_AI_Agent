"""생성 결과 근거 검증 (SRS GDE-09).

1) 인용이 없는 문장 제거
2) 존재하지 않는 근거를 인용한 문장 제거
3) 인용 근거 원문과 겹치지 않는 문장 제거
통과 문장이 없으면 재생성 1회, 그래도 없으면 보류한다.
"""
import re
from typing import List, Tuple

MIN_OVERLAP = 0.2
MAX_REGENERATE = 1   # SRS 미정 항목 — 팀 확정 필요

EV_RE = re.compile(r"EV-\d{4}")


def verify_guide(text: str, hits, min_overlap: float = MIN_OVERLAP
                 ) -> Tuple[List[str], List[Tuple[str, str]]]:
    ev_map = {e.evidence_id: e.evidence_text for _, e in hits}
    kept: List[str] = []
    dropped: List[Tuple[str, str]] = []
    for line in [l.strip() for l in text.splitlines() if l.strip()]:
        ids = EV_RE.findall(line)
        if not ids:
            dropped.append((line, "인용 없음"))
            continue
        if any(i not in ev_map for i in ids):
            dropped.append((line, "존재하지 않는 근거 인용"))
            continue
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
    """생성 → 검증 → (실패 시) 재생성 → 그래도 실패하면 보류."""
    attempts = []
    for _ in range(max_retry + 1):
        text = call_fn(build_prompt_fn())
        kept, dropped = verify_guide(text, hits)
        attempts.append({"text": text, "kept": kept, "dropped": dropped})
        if kept:
            return {"status": "제공", "lines": kept, "attempts": attempts}
    return {"status": "보류", "lines": [], "attempts": attempts}
