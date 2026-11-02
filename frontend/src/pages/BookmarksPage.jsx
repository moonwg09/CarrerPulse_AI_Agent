import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NavBar from "../components/NavBar";
import { getBookmarks, setBookmark } from "../api/jobApi";

export default function BookmarksPage() {
  const [jobs, setJobs] = useState([]);
  const [message, setMessage] = useState("");
  const [deletingId, setDeletingId] = useState(null);

  useEffect(() => {
    getBookmarks()
      .then(setJobs)
      .catch((error) => setMessage(error.message));
  }, []);

  async function remove(job) {
    if (!window.confirm(`'${job.title}' 공고를 관심 공고에서 삭제하시겠습니까?`)) return;
    setDeletingId(job.jobId);
    setMessage("");
    try {
      await setBookmark(job.jobId, false);
      setJobs(current => current.filter(item => item.jobId !== job.jobId));
      setMessage("관심 공고에서 삭제했습니다.");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setDeletingId(null);
    }
  }

  return <main>
    <NavBar />
    <h1>관심 공고</h1>
    <p>저장한 공고를 최근 저장 순서로 보여줍니다.</p>
    {message && <p role="status">{message}</p>}
    {!message && jobs.length === 0 && <p>저장한 관심 공고가 없습니다.</p>}
    {jobs.length > 0 && <table>
      <thead><tr><th>공고</th><th>회사</th><th>지역</th><th>마감일</th><th>상태</th><th>관리</th></tr></thead>
      <tbody>{jobs.map((job) => <tr key={job.jobId}>
        <td><Link to={`/jobs/${job.jobId}`}>{job.title}</Link></td>
        <td>{job.companyName}</td>
        <td>{job.region || "-"}</td>
        <td>{job.deadline || "-"}</td>
        <td>{job.status}</td>
        <td><button className="button-compact button-danger" type="button" disabled={deletingId !== null} onClick={() => remove(job)}>
          {deletingId === job.jobId ? "삭제 중" : "삭제"}
        </button></td>
      </tr>)}</tbody>
    </table>}
  </main>;
}
