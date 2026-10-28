import unittest
from datetime import date
from unittest.mock import Mock

from services.job_sources.work24_crawler import (
    Work24Crawler,
    Work24SearchItem,
    parse_work24_detail_page,
    parse_work24_search_page,
)


SEARCH_HTML = """
<html><body><table>
  <tr>
    <td>
      <a class="cp_name">테스트 주식회사</a>
      <a data-emp-detail href="/wk/a/b/1500/empDetailAuthView.do?wantedAuthNo=K123&amp;infoTypeCd=VALIDATION">
        백엔드 개발자
      </a>
    </td>
    <td><li class="site"><p>서울특별시 강남구</p></li></td>
    <td><p>마감일 : 2026-11-30</p></td>
  </tr>
</table></body></html>
"""


DETAIL_HTML = """
<html><body>
  <div class="tit_area">
    <p class="corp_info"><strong>테스트 주식회사</strong></p>
    <strong class="title">백엔드 개발자</strong>
  </div>
  <div class="fold">
    <strong>직무내용</strong>
    Python과 FastAPI를 이용한 API 개발
    <button>더보기</button>
  </div>
  <table><tr>
    <th>근무 예정지</th>
    <td><div><p>서울특별시 강남구 테헤란로</p></div></td>
  </tr></table>
  <div class="recruiter">채용 담당자 010-0000-0000</div>
</body></html>
"""


class Work24ParserTest(unittest.TestCase):
    def test_search_page_parses_public_job_metadata(self):
        items = parse_work24_search_page(SEARCH_HTML)

        self.assertEqual(1, len(items))
        self.assertEqual("K123", items[0].external_job_id)
        self.assertEqual("백엔드 개발자", items[0].title)
        self.assertEqual("테스트 주식회사", items[0].company_name)
        self.assertEqual("서울특별시 강남구", items[0].region)
        self.assertEqual(date(2026, 11, 30), items[0].close_date)

    def test_detail_page_collects_only_job_description(self):
        item = Work24SearchItem(
            external_job_id="K123",
            title="목록 제목",
            company_name="목록 회사",
            region="서울특별시",
            close_date=date(2026, 11, 30),
            detail_url="https://www.work24.go.kr/detail/K123",
        )

        job = parse_work24_detail_page(DETAIL_HTML, item)

        self.assertEqual("work24_K123", job.jobId)
        self.assertEqual("백엔드 개발자", job.title)
        self.assertEqual("테스트 주식회사", job.companyName)
        self.assertEqual("서울특별시 강남구 테헤란로", job.region)
        self.assertEqual("Python과 FastAPI를 이용한 API 개발", job.content)
        self.assertNotIn("채용 담당자", job.content)
        self.assertNotIn("더보기", job.content)


class Work24CrawlerTest(unittest.TestCase):
    def test_collect_filters_region_and_removes_duplicate_jobs(self):
        search_response = Mock()
        search_response.text = SEARCH_HTML.replace(
            "</table>",
            SEARCH_HTML.split("<table>", 1)[1].split("</table>", 1)[0]
            + "</table>",
        )
        search_response.raise_for_status = Mock()

        detail_response = Mock()
        detail_response.text = DETAIL_HTML
        detail_response.raise_for_status = Mock()

        session = Mock()
        session.headers = {}
        session.post.return_value = search_response
        session.get.return_value = detail_response

        result = Work24Crawler(
            session=session,
            request_interval_seconds=0,
            max_pages=1,
        ).collect("개발자", "서울", limit=10)

        self.assertEqual(1, result.collectedCount)
        self.assertEqual("work24_K123", result.jobs[0].jobId)
        session.get.assert_called_once()

    def test_collect_does_not_open_detail_when_region_does_not_match(self):
        search_response = Mock()
        search_response.text = SEARCH_HTML
        search_response.raise_for_status = Mock()

        session = Mock()
        session.headers = {}
        session.post.return_value = search_response

        result = Work24Crawler(
            session=session,
            request_interval_seconds=0,
            max_pages=1,
        ).collect("개발자", "부산", limit=10)

        self.assertEqual(0, result.collectedCount)
        session.get.assert_not_called()


if __name__ == "__main__":
    unittest.main()
