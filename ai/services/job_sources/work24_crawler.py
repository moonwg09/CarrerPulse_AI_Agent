import re
import time
from dataclasses import dataclass
from datetime import date
from typing import Optional
from urllib.parse import parse_qs, urljoin, urlparse

import requests
from lxml import html

from schemas.job_collection_schema import JobCollectionResult
from schemas.raw_job_schema import RawJobPosting


WORK24_BASE_URL = "https://www.work24.go.kr"
WORK24_SEARCH_URL = (
    f"{WORK24_BASE_URL}/wk/a/b/1200/retriveDtlEmpSrchListInPost.do"
)
WORK24_SOURCE = "WORK24"
WORK24_SOFTWARE_OCCUPATION_CODE = "024"
DEFAULT_MAX_PAGES = 5
DEFAULT_REQUEST_INTERVAL_SECONDS = 0.5
PUBLIC_REQUIREMENT_LABELS = (
    "모집 직종",
    "관련 직종",
    "직종 키워드",
    "경력",
    "학력",
    "자격 면허",
    "고용 형태",
    "임금 조건",
    "전공",
    "컴퓨터 활용 능력",
    "외국어 능력",
    "우대조건",
    "기타 우대사항",
)


class Work24CollectionError(RuntimeError):
    """고용24 공개 채용공고를 수집하지 못했을 때 발생하는 오류."""


@dataclass(frozen=True)
class Work24SearchItem:
    external_job_id: str
    title: str
    company_name: str
    region: Optional[str]
    posted_date: Optional[date]
    close_date: Optional[date]
    detail_url: str


def _clean_text(value: str) -> str:
    return " ".join(value.replace("\xa0", " ").split())


def _first_text(elements) -> Optional[str]:
    if not elements:
        return None
    text = _clean_text(elements[0].text_content())
    return text or None


def _normalize_search_keyword(keyword: str) -> str:
    normalized = _clean_text(keyword)
    if normalized == "개발자":
        return ""
    return re.sub(r"\s*개발자$", "", normalized).strip()


def _parse_iso_date(value: Optional[str]) -> Optional[date]:
    if not value:
        return None
    matched = re.search(r"(\d{4})[-.](\d{2})[-.](\d{2})", value)
    if not matched:
        return None
    try:
        return date.fromisoformat("-".join(matched.groups()))
    except ValueError:
        return None


def _table_value(document, label: str) -> Optional[str]:
    cells = document.xpath(
        "//th[normalize-space()=$label]/following-sibling::td[1]",
        label=label,
    )
    return _first_text(cells)


def parse_work24_search_page(page_html: str) -> list[Work24SearchItem]:
    document = html.fromstring(page_html)
    items: list[Work24SearchItem] = []

    for row in document.xpath("//tr[.//a[@data-emp-detail]]"):
        detail_links = row.xpath(".//a[@data-emp-detail]")
        if not detail_links:
            continue

        detail_link = detail_links[0]
        detail_href = detail_link.get("href") or ""
        query = parse_qs(urlparse(detail_href).query)
        external_job_id = (query.get("wantedAuthNo") or [""])[0].strip()
        info_type = (query.get("infoTypeCd") or [""])[0].strip()
        info_group = (query.get("infoTypeGroup") or [""])[0].strip()
        title = _clean_text(detail_link.text_content())
        company_name = _first_text(
            row.xpath(".//a[contains(@class, 'cp_name')]")
        )

        if (
            not external_job_id
            or info_type != "VALIDATION"
            or info_group != "tb_workinfoworknet"
            or not title
            or not company_name
        ):
            continue

        region = _first_text(row.xpath(".//li[contains(@class, 'site')]//p"))
        deadline_text = _first_text(
            row.xpath(".//p[contains(normalize-space(.), '마감일')]")
        )
        posted_date_text = _first_text(
            row.xpath(".//p[contains(normalize-space(.), '등록일')]")
        )

        items.append(
            Work24SearchItem(
                external_job_id=external_job_id,
                title=title,
                company_name=company_name,
                region=region,
                posted_date=_parse_iso_date(posted_date_text),
                close_date=_parse_iso_date(deadline_text),
                detail_url=urljoin(WORK24_BASE_URL, detail_href),
            )
        )

    return items


def parse_work24_detail_page(
    page_html: str,
    search_item: Work24SearchItem,
) -> RawJobPosting:
    document = html.fromstring(page_html)

    title = _first_text(
        document.xpath("//div[contains(@class, 'tit_area')]//strong[@class='title']")
    ) or search_item.title
    company_name = _first_text(
        document.xpath(
            "//div[contains(@class, 'tit_area')]"
            "//p[contains(@class, 'corp_info')]/strong"
        )
    ) or search_item.company_name
    region = _first_text(
        document.xpath(
            "//th[normalize-space()='근무 예정지']/following-sibling::td[1]//p[1]"
        )
    ) or search_item.region

    content_nodes = document.xpath(
        "//strong[normalize-space()='직무내용']/parent::*"
    )
    if not content_nodes:
        raise Work24CollectionError(
            f"고용24 공고 본문을 찾을 수 없습니다: {search_item.external_job_id}"
        )

    content_node = content_nodes[0]
    for removable in content_node.xpath(".//script | .//style | .//button"):
        removable.getparent().remove(removable)
    job_description = _clean_text(content_node.text_content())
    job_description = re.sub(r"^직무내용\s*", "", job_description).strip()
    if not job_description:
        raise Work24CollectionError(
            f"고용24 공고 본문이 비어 있습니다: {search_item.external_job_id}"
        )

    content_parts = [f"직무내용\n{job_description}"]
    for label in PUBLIC_REQUIREMENT_LABELS:
        value = _table_value(document, label)
        if value and value not in {"-", "해당없음"}:
            content_parts.append(f"{label}\n{value}")
    content = "\n\n".join(content_parts)

    return RawJobPosting(
        jobId=f"work24_{search_item.external_job_id}",
        externalJobId=search_item.external_job_id,
        title=title,
        companyName=company_name,
        region=region,
        postedDate=search_item.posted_date,
        closeDate=search_item.close_date,
        url=search_item.detail_url,
        content=content,
        source=WORK24_SOURCE,
    )


class Work24Crawler:
    def __init__(
        self,
        session: Optional[requests.Session] = None,
        request_interval_seconds: float = DEFAULT_REQUEST_INTERVAL_SECONDS,
        max_pages: int = DEFAULT_MAX_PAGES,
    ) -> None:
        self.session = session or requests.Session()
        self.session.headers.update(
            {
                "User-Agent": "CareerPulse-MVP/1.0 (educational project)",
                "Accept-Language": "ko-KR,ko;q=0.9",
            }
        )
        self.request_interval_seconds = max(0.0, request_interval_seconds)
        self.max_pages = max(1, max_pages)

    def _wait(self) -> None:
        if self.request_interval_seconds:
            time.sleep(self.request_interval_seconds)

    def _search(self, keyword: str, page: int) -> list[Work24SearchItem]:
        search_keyword = _normalize_search_keyword(keyword)
        try:
            response = self.session.post(
                WORK24_SEARCH_URL,
                data={
                    "currentPageNo": str(page),
                    "pageIndex": str(page),
                    "searchMode": "Y",
                    "occupation": WORK24_SOFTWARE_OCCUPATION_CODE,
                    "srcKeyword": search_keyword,
                    "keyword": search_keyword,
                    "keywordWantedTitle": "Y",
                    "keywordJobCont": "Y",
                },
                timeout=20,
            )
            response.raise_for_status()
        except requests.RequestException as exc:
            raise Work24CollectionError(
                "고용24 채용공고 목록 요청에 실패했습니다."
            ) from exc

        response.encoding = "utf-8"
        return parse_work24_search_page(response.text)

    def _fetch_detail(self, item: Work24SearchItem) -> RawJobPosting:
        self._wait()
        try:
            response = self.session.get(item.detail_url, timeout=20)
            response.raise_for_status()
        except requests.RequestException as exc:
            raise Work24CollectionError(
                f"고용24 채용공고 상세 요청에 실패했습니다: {item.external_job_id}"
            ) from exc

        response.encoding = "utf-8"
        return parse_work24_detail_page(response.text, item)

    def collect(
        self,
        job_keyword: str,
        region: str,
        limit: int = 10,
    ) -> JobCollectionResult:
        keyword = job_keyword.strip()
        region_filter = region.strip()
        if not keyword:
            raise ValueError("직무 키워드는 비어 있을 수 없습니다.")
        if not region_filter:
            raise ValueError("지역은 비어 있을 수 없습니다.")
        if not 1 <= limit <= 10:
            raise ValueError("수집 개수는 1개 이상 10개 이하여야 합니다.")

        jobs: list[RawJobPosting] = []
        seen_ids: set[str] = set()

        for page in range(1, self.max_pages + 1):
            if page > 1:
                self._wait()
            search_items = self._search(keyword, page)
            if not search_items:
                break

            for item in search_items:
                if item.external_job_id in seen_ids:
                    continue
                seen_ids.add(item.external_job_id)

                if not item.region or region_filter not in item.region:
                    continue

                try:
                    jobs.append(self._fetch_detail(item))
                except Work24CollectionError:
                    continue
                if len(jobs) >= limit:
                    break

            if len(jobs) >= limit:
                break

        return JobCollectionResult(
            jobKeyword=keyword,
            region=region_filter,
            requestedLimit=limit,
            collectedCount=len(jobs),
            jobs=jobs,
        )


def collect_work24_jobs(
    job_keyword: str,
    region: str,
    limit: int = 10,
) -> JobCollectionResult:
    return Work24Crawler().collect(job_keyword, region, limit)
