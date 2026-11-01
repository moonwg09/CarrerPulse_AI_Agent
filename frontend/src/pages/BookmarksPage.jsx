import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NavBar from "../components/NavBar";
import { getBookmarks } from "../api/jobApi";

export default function BookmarksPage() {
  const [jobs, setJobs] = useState([]);
  const [message, setMessage] = useState("");

  useEffect(() => {
    getBookmarks()
      .then(setJobs)
      .catch((error) => setMessage(error.message));
  }, []);

  return <main>
    <NavBar />
    <h1>관심 공고</h1>
    <p>저장한 공고를 최근 저장 순서로 보여줍니다.</p>
    {message && <p role="status">{message}</p>}
    {!message && jobs.length === 0 && <p>저장한 관심 공고가 없습니다.</p>}
    {jobs.length > 0 && <table>
      <thead><tr><th>공고</th><th>회사</th><th>지역</th><th>마감일</th><th>상태</th></tr></thead>
      <tbody>{jobs.map((job) => <tr key={job.jobId}>
        <td><Link to={`/jobs/${job.jobId}`}>{job.title}</Link></td>
        <td>{job.companyName}</td>
        <td>{job.region || "-"}</td>
        <td>{job.deadline || "-"}</td>
        <td>{job.status}</td>
      </tr>)}</tbody>
    </table>}
  </main>;
}
