import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getJob } from "../api/jobApi";
import { createApplication } from "../api/applicationApi";

export default function JobDetailPage() {
  const { jobId } = useParams();
  const [job, setJob] = useState(null);
  const [message, setMessage] = useState("");

  async function apply() {
    try {
      await createApplication(job.jobId);
      setMessage("지원 현황에 등록했습니다.");
    } catch (error) { setMessage(error.message); }
  }

  useEffect(() => {
    getJob(jobId).then(setJob).catch((error) => setMessage(error.message));
  }, [jobId]);

  if (!job) return <main>{message || "불러오는 중"}</main>;

  return <main>
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
    {job.originalUrl && <p><a href={job.originalUrl} target="_blank" rel="noreferrer">원문 공고 보기</a></p>}
    <button type="button" onClick={apply}>지원 현황에 등록</button>
    {message && <p>{message}</p>}
  </main>;
}
