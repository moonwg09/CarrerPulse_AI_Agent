"""문서의 의미 단위 모델"""

from dataclasses import dataclass, field
from typing import List, Optional

from rag.parser.models import DocumentBlock


@dataclass
class SemanticUnit:
    """여러 DocumentBlock을 하나의 의미 단위로 묶는다"""

    unit_id: str

    # PERSONAL_INFO / EDUCATION / TRAINING / PROJECT /
    # CAREER / CERTIFICATION / SKILL / ACTIVITY / LANGUAGE / OTHER
    category: str

    section: str
    title: str

    blocks: List[DocumentBlock] = field(default_factory=list)

    # 개인정보 등 RAG 검색 대상에서 제외할 영역
    embedding_enabled: bool = True

    project_status: Optional[str] = None

    @property
    def block_ids(self) -> List[str]:
        return [block.block_id for block in self.blocks]

    @property
    def text(self) -> str:
        return "\n".join(
            block.text
            for block in self.blocks
            if block.text.strip()
        )

    @property
    def source_locations(self) -> List[str]:
        locations = []

        for block in self.blocks:
            loc = block.location

            if loc.page is not None:
                value = f"p.{loc.page}"
            elif loc.paragraph is not None:
                value = f"문단 {loc.paragraph}"
            elif loc.table is not None:
                value = f"표 {loc.table}"
            else:
                continue

            if value not in locations:
                locations.append(value)

        return locations