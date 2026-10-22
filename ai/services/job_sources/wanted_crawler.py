import re
from datetime import date
from typing import Optional

import requests
from bs4 import BeautifulSoup

from schemas.raw_job_schema import RawJobPosting


def crawl_wanted_job(
    url: str
) -> RawJobPosting:

    headers = {
        "User-Agent": (
            "Mozilla/5.0 "
            "(Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 "
            "(KHTML, like Gecko) "
            "Chrome/154.0.0.0 Safari/537.36"
        )
    }

    response = requests.get(
        url,
        headers=headers,
        timeout=10,
    )

    response.raise_for_status()

    soup = BeautifulSoup(
        response.text,
        "html.parser",
    )

    # ---------------------------------------------
    # 공고 ID
    # 예: https://www.wanted.co.kr/wd/277790
    # ---------------------------------------------
    match = re.search(
        r"/wd/(\d+)",
        url,
    )

    if not match:
        raise ValueError(
            "원티드 공고 ID를 찾을 수 없습니다."
        )

    wanted_id = match.group(1)

    job_id = f"wanted_{wanted_id}"

    # ---------------------------------------------
    # 페이지 전체 텍스트
    # ---------------------------------------------
    page_text = soup.get_text(
        "\n",
        strip=True,
    )

    if not page_text:
        raise ValueError(
            "원티드 공고 본문을 가져오지 못했습니다."
        )

    # ---------------------------------------------
    # 우선 테스트 단계에서는
    # 제목/회사명을 HTML에서 단순 추출
    # ---------------------------------------------
    title = None
    company_name = None

    if soup.title:
        title_text = soup.title.get_text(
            " ",
            strip=True,
        )

        title = title_text

    # 실제 selector는
    # HTML 구조 확인 후 다음 단계에서 정교하게 수정
    company_name = "UNKNOWN"

    return RawJobPosting(
        jobId=job_id,
        title=title or "UNKNOWN",
        companyName=company_name,
        region=None,
        closeDate=None,
        url=url,
        content=page_text,
        source="WANTED_CRAWLER",
    )

if __name__ == "__main__":

    test_url = (
        "https://www.wanted.co.kr/wd/277790"
    )

    result = crawl_wanted_job(
        test_url
    )

    print(
        result.model_dump_json(
            indent=2
        )
    )