"""DocumentBlock을 검색 가능한 의미 단위로 묶는다."""

from typing import List

from rag.parser.models import DocumentBlock

from .semantic_unit import SemanticUnit
from .section_parser import (
    PERSONAL_INFO_CONTEXT,
    detect_project_title,
    detect_section,
)


def build_semantic_units(
    blocks: List[DocumentBlock],
) -> List[SemanticUnit]:

    units: List[SemanticUnit] = []

    current_context = PERSONAL_INFO_CONTEXT

    current_blocks: List[DocumentBlock] = []
    current_title = "개인정보"
    current_project_status = None

    unit_seq = 1

    def flush():
        nonlocal current_blocks
        nonlocal unit_seq
        nonlocal current_title
        nonlocal current_project_status

        if not current_blocks:
            return

        units.append(
            SemanticUnit(
                unit_id=f"U-{unit_seq:04d}",
                category=current_context.category,
                section=current_context.section,
                title=current_title,
                blocks=current_blocks.copy(),
                embedding_enabled=current_context.embedding_enabled,
                project_status=current_project_status,
            )
        )

        unit_seq += 1
        current_blocks = []
        current_project_status = None

    for block in blocks:

        # -------------------------
        # 최상위 Section
        # -------------------------
        section = detect_section(block)

        if section is not None:

            flush()

            current_context = section
            current_title = section.section

            # 제목 block 자체도 원문 근거에 포함
            current_blocks = [block]

            continue

        # -------------------------
        # Project
        # -------------------------
        if current_context.category == "PROJECT":

            project = detect_project_title(block)

            if project is not None:

                flush()

                current_title = project.title
                current_project_status = project.status

                current_blocks = [block]

                continue

        current_blocks.append(block)

    flush()

    # "프로젝트" 섹션 제목만 들어 있는 의미 없는 unit 제거
    filtered_units = [
        unit
        for unit in units
        if not (
            unit.category == "PROJECT"
            and len(unit.blocks) == 1
            and unit.title == unit.section
        )
    ]

    # 필터링 후 unit_id를 연속 번호로 다시 부여
    for i, unit in enumerate(filtered_units, start=1):
        unit.unit_id = f"U-{i:04d}"

    return filtered_units

def embedding_units(
    units: List[SemanticUnit],
) -> List[SemanticUnit]:
    """RAG 검색/임베딩 대상만 반환"""

    return [
        unit
        for unit in units
        if unit.embedding_enabled
        and unit.category != "PERSONAL_INFO"
        and unit.text.strip()
    ]

