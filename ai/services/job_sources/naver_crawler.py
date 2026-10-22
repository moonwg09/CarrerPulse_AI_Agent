import re

from playwright.sync_api import sync_playwright

from schemas.raw_job_schema import RawJobPosting


def crawl_naver_job(
    url: str,
) -> RawJobPosting:

    match = re.search(
        r"annoId=(\d+)",
        url,
    )

    if not match:
        raise ValueError(
            "네이버 공고 ID를 찾을 수 없습니다."
        )

    anno_id = match.group(1)

    with sync_playwright() as p:

        browser = p.chromium.launch(
            headless=True
        )

        page = browser.new_page()

        page.goto(
            url,
            wait_until="networkidle",
            timeout=30000,
        )

        # 페이지 렌더링 완료 후 텍스트 추출
        page_text = page.locator(
            "body"
        ).inner_text()

        title = page.title()

        browser.close()

    if not page_text.strip():
        raise ValueError(
            "네이버 채용공고 내용을 가져오지 못했습니다."
        )

    return RawJobPosting(
        jobId=f"naver_{anno_id}",
        title=title or "NAVER 채용공고",
        companyName="NAVER",
        region=None,
        closeDate=None,
        url=url,
        content=page_text.strip(),
        source="NAVER_CAREERS",
    )


if __name__ == "__main__":

    test_url = (
        "https://recruit.navercorp.com/"
        "rcrt/view.do?"
        "annoId=30005466&lang=ko"
    )

    result = crawl_naver_job(
        test_url
    )

    print(
        result.model_dump_json(
            indent=2
        )
    )