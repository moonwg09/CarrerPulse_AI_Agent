"""prompt — LLM 입력을 만든다. 요구/근거 구역 분리와 인젝션 방어 (RAG-02)."""
from .templates import (JUDGE_PROMPT, GUIDE_PROMPT, build_evidence_block,
                        build_judge_prompt, build_guide_prompt)

__all__ = ["JUDGE_PROMPT", "GUIDE_PROMPT", "build_evidence_block",
           "build_judge_prompt", "build_guide_prompt"]
