"""Gemini 호출 (API 키가 없으면 모의 응답).

모의 응답은 흐름 확인용이며, 실제 판단 품질을 대표하지 않는다.
"""
import json
import os
import re
from typing import Optional

MODEL_NAME = "gemini-2.0-flash"


def call_gemini(prompt: str, api_key: Optional[str] = None) -> dict:
    api_key = api_key or os.environ.get("GEMINI_API_KEY")
    if api_key:
        import google.generativeai as genai

        genai.configure(api_key=api_key)
        model = genai.GenerativeModel(MODEL_NAME)
        raw = model.generate_content(prompt).text
        m = re.search(r"\{.*\}", raw, re.S)
        if not m:
            raise ValueError("JSON 형식의 응답을 받지 못했습니다.")
        return json.loads(m.group())
    return _mock(prompt)


def _mock(prompt: str) -> dict:
    block = prompt.split("[사용자 근거]")[1].split("[규칙]")[0]
    cited = re.findall(r"EV-\d{4}", block)
    has = bool(cited)
    scope = has and any(k in block for k in ["직접 구현", "직접 작성", "엔드포인트"])
    partial = has and ("참여" in block) and not scope
    return {"has_evidence": has, "scope_specified": scope,
            "partially_satisfied": partial, "cited": cited[:3],
            "missing_part": "" if scope else "수행 범위가 명시되지 않음",
            "reason": "모의 응답(API 키 미설정)"}


def call_gemini_text(prompt: str, api_key: Optional[str] = None) -> str:
    """작성 방향처럼 문장을 받는 호출."""
    api_key = api_key or os.environ.get("GEMINI_API_KEY")
    if api_key:
        import google.generativeai as genai

        genai.configure(api_key=api_key)
        return genai.GenerativeModel(MODEL_NAME).generate_content(prompt).text
    # 근거 구역에서 실제 표현을 가져와 '근거 있는 문장'을 흉내 낸다.
    block = prompt.split("[사용자 근거]")[1].split("[규칙]")[0] if "[사용자 근거]" in prompt else ""
    m = re.search(r"(EV-\d{4})[^\n]*\n?(.*)", block, re.S)
    ev = m.group(1) if m else "EV-0000"
    body = re.sub(r"EV-\d{4}|\[[^\]]*\]", " ", block)
    keywords = [w for w in re.findall(r"[가-힣]{2,}", body) if w not in ("담당", "직접")][:3]
    grounded = " ".join(keywords) if keywords else "담당 작업"
    return (f"{grounded} 부분의 수행 범위를 덧붙이십시오. ({ev})\n"
            "Docker 배포 경험을 강조하십시오.")  # 인용 없음 → 검증에서 제거되어야 함
