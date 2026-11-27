"""문서 파서 공통 데이터 모델"""

from dataclasses import dataclass, field
from typing import List, Optional


@dataclass
class DocumentLocation:
    """원문에서 Block의 위치"""

    page: Optional[int] = None
    paragraph: Optional[int] = None
    table: Optional[int] = None


@dataclass
class DocumentBlock:
    """PDF/DOCX에서 추출한 최소 문서 구조 단위"""

    block_id: str
    block_type: str          # paragraph / table
    text: str

    location: DocumentLocation

    style: Optional[str] = None
    is_heading: bool = False

    # table인 경우 원래 행/열 구조를 보존
    rows: List[List[str]] = field(default_factory=list)

    def to_legacy_dict(self) -> dict:
        """기존 processor와의 호환용 dict"""
        """새 parser -> document block -> legacy dict -> 현재 chunker"""

        return {
            "page": self.location.page,
            "para": self.location.paragraph,
            "text": self.text,
            "block_id": self.block_id,
            "block_type": self.block_type,
            "table": self.location.table,
            "style": self.style,
            "is_heading": self.is_heading,
            "rows": self.rows,
        }