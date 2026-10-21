"""DocumentBlock을 이력서의 의미 영역으로 분류"""

import re
from dataclasses import dataclass
from typing import List, Optional

from rag.parser.models import DocumentBlock


@dataclass(frozen=True)
class SectionDefinition:
    category: str
    canonical_name: str
    aliases: tuple[str, ...]
    embedding_enabled: bool = True


SECTION_DEFINITIONS = (
    SectionDefinition(
        category="EDUCATION",
        canonical_name="학력",
        aliases=("학력", "학력사항"),
    ),
    SectionDefinition(
        category="TRAINING",
        canonical_name="교육",
        aliases=("교육", "교육사항", "교육/연수", "교육이수"),
    ),
    SectionDefinition(
        category="PROJECT",
        canonical_name="프로젝트",
        aliases=("프로젝트", "프로젝트 이력", "프로젝트이력", "프로젝트 경험"),
    ),
    SectionDefinition(
        category="CAREER",
        canonical_name="경력",
        aliases=("경력", "경력사항", "경력기술", "기타경력"),
    ),
    SectionDefinition(
        category="CERTIFICATION",
        canonical_name="자격증",
        aliases=("자격증", "자격사항"),
    ),
    SectionDefinition(
        category="SKILL",
        canonical_name="역량",
        aliases=("활용능력", "보유기술", "기술스택"),
    ),
    SectionDefinition(
        category="ACTIVITY",
        canonical_name="대외활동",
        aliases=("기타활동", "대외활동"),
    ),
    SectionDefinition(
        category="LANGUAGE",
        canonical_name="어학",
        aliases=("어학", "외국어"),
    ),
)


@dataclass
class SectionContext:
    category: str
    section: str
    embedding_enabled: bool = True

PERSONAL_INFO_CONTEXT = SectionContext(
    category="PERSONAL_INFO",
    section="개인정보",
    embedding_enabled=False,
)

def _normalize(text: str) -> str:
    return re.sub(r"\s+", "", text).strip()


def detect_section(block: DocumentBlock) -> Optional[SectionContext]:
    """블록이 최상위 섹션 제목인지 판별"""

    if block.block_type != "paragraph":
        return None

    text = block.text.strip()

    # 너무 긴 일반 문장은 섹션 제목으로 보지 않는다.
    if len(text) > 60:
        return None

    normalized = _normalize(text)

    for definition in SECTION_DEFINITIONS:
        for alias in definition.aliases:
            normalized_alias = _normalize(alias)

            # "기타경력 (전체경력: 3년 6개월)" 같은 제목 허용
            if (
                normalized == normalized_alias
                or normalized.startswith(normalized_alias + "(")
            ):
                return SectionContext(
                    category=definition.category,
                    section=definition.canonical_name,
                    embedding_enabled=definition.embedding_enabled,
                )

    return None


# Project 제목 감지
PROJECT_TITLE_PATTERN = re.compile(
    r"^\s*(?P<number>\d{1,2})\s*[.)]\s*(?P<title>.+?)\s*$"
)

PROJECT_STATUS_PATTERN = re.compile(
    r"^\[(?P<status>진행\s*중|완료|중단)\]\s*"
)


@dataclass
class ProjectTitle:
    title: str
    status: Optional[str] = None


def detect_project_title(block: DocumentBlock) -> Optional[ProjectTitle]:
    """프로젝트 섹션 안의 개별 프로젝트 제목을 판별한다."""

    if block.block_type != "paragraph":
        return None

    match = PROJECT_TITLE_PATTERN.match(block.text)

    if not match:
        return None

    title = match.group("title").strip()
    status = None

    status_match = PROJECT_STATUS_PATTERN.match(title)

    if status_match:
        status = status_match.group("status").replace(" ", "")
        title = PROJECT_STATUS_PATTERN.sub("", title).strip()

    return ProjectTitle(
        title=title,
        status=status,
    )