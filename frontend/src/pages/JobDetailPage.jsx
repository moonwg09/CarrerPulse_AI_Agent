import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getJob, getBookmarkStatus, setBookmark } from "../api/jobApi";
import { createApplication } from "../api/applicationApi";
import NavBar from "../components/NavBar";
import PageToast from "../components/PageToast";

export default function JobDetailPage() {
  const { jobId } = useParams();
  const [job, setJob] = useState(null);
  const [message, setMessage] = useState("");
  const [bookmarked, setBookmarkedState] = useState(false);
  const [notice, setNotice] = useState("");

  async function apply() {
    try {
      setMessage("");
      await createApplication(job.jobId);
      setNotice("지원 현황에 등록했습니다.");
    } catch (error) { setMessage(error.message); }
  }

  useEffect(() => {
    getJob(jobId).then(setJob).catch((error) => setMessage(error.message));
    getBookmarkStatus(jobId)
      .then((result) => setBookmarkedState(result.bookmarked))
      .catch((error) => setMessage(error.message));
  }, [jobId]);

  async function toggleBookmark() {
    try {
      setMessage("");
      const next = !bookmarked;
      await setBookmark(jobId, next);
      setBookmarkedState(next);
      setNotice(next ? "관심 공고로 저장했습니다." : "관심 공고에서 삭제했습니다.");
    } catch (error) { setMessage(error.message); }
  }

  if (!job) return <main><NavBar />{message || "불러오는 중"}</main>;

  const requirements = job.requirements || [];
  const byType = (type) => requirements.filter((item) =>
    item.comparisonKind === "EXPERIENCE" && item.requirementType === type
  );
  const objective = requirements.filter((item) => item.comparisonKind === "OBJECTIVE");
  const responsibilities = requirements.filter((item) => item.comparisonKind === "RESPONSIBILITY");

  const requirementList = (title, items) => items.length > 0 && <section>
    <h2>{title}</h2>
    <div className="detail-list">{items.map((item, index) => <div className="detail-list-row" key={item.requirementId}>
      <span className="record-index">{String(index + 1).padStart(2, "0")}</span>
      <div>
        <strong>{item.name}</strong>
        {item.sourceQuote && <small className="record-meta">원문 · {item.sourceQuote}</small>}
      </div>
    </div>)}</div>
  </section>;

  return <main>
    <NavBar />
    <PageToast message={notice} onClose={setNotice} />
    <Link to="/analysis">채용공고 목록</Link>
    <h1>{job.title}</h1>
    <p>{job.companyName}</p>
    <dl>
      <dt>직무</dt><dd>{job.jobCategory || "미기재"}</dd>
      <dt>지역</dt><dd>{job.region || "미기재"}</dd>
      <dt>등록일</dt><dd>{job.postedDate || "미기재"}</dd>
      <dt>마감일</dt><dd>{job.deadline || "미기재"}</dd>
      <dt>수집 출처</dt><dd>{job.source}</dd>
    </dl>
    <h2>공고 내용</h2>
    <p style={{ whiteSpace: "pre-wrap" }}>{job.description}</p>
    {requirements.length === 0
      ? <p>아직 구조화된 요구조건이 없습니다. 공고 비교를 실행하면 분석 결과가 표시됩니다.</p>
      : <>
        {requirementList("주요 업무", responsibilities)}
        {requirementList("필수 조건", byType("REQUIRED"))}
        {requirementList("우대 조건", byType("PREFERRED"))}
        {requirementList("구분 불명확", byType("UNCLEAR"))}
        {requirementList("객관 조건", objective)}
      </>}
    {job.originalUrl && <p><a href={job.originalUrl} target="_blank" rel="noreferrer">원문 공고 보기</a></p>}
    <button type="button" onClick={apply}>지원 현황에 등록</button>
    <button className={bookmarked ? "button-danger" : ""} type="button" onClick={toggleBookmark}>
      {bookmarked ? "관심 공고 해제" : "관심 공고 저장"}
    </button>
    {message && <p>{message}</p>}
  </main>;
}
