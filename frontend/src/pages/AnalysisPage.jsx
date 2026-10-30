import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { compareDocumentWithJob, getAnalyses } from "../api/analysisApi";

function AnalysisPage() {
  const [jobId, setJobId] = useState("");
  const [analyses, setAnalyses] = useState([]);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const load = async () => {
    try {
      setAnalyses(await getAnalyses());
    } catch (error) {
      setMessage(error.message);
    }
  };

  useEffect(() => {
    getAnalyses()
      .then(setAnalyses)
      .catch((error) => setMessage(error.message));
  }, []);

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setMessage("");
    try {
      await compareDocumentWithJob({ jobId: Number(jobId) });
      setMessage("분석이 완료되었습니다.");
      await load();
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main>
      <h1>공고 분석</h1>
      <p>저장된 공고 ID를 입력하면 등록된 문서 근거로 적합도를 분석합니다.</p>

      <form onSubmit={submit}>
        <label htmlFor="jobId">공고 ID</label>{" "}
        <input
          id="jobId"
          type="number"
          min="1"
          required
          value={jobId}
          onChange={(event) => setJobId(event.target.value)}
        />{" "}
        <button type="submit" disabled={loading}>
          {loading ? "분석 중" : "분석 시작"}
        </button>
      </form>

      {message && <p>{message}</p>}

      <h2>분석 결과</h2>
      <table>
        <thead>
          <tr>
            <th>공고</th>
            <th>회사</th>
            <th>상태</th>
            <th>전체 일치율</th>
            <th>평가 가능 비율</th>
          </tr>
        </thead>
        <tbody>
          {analyses.map((item) => (
            <tr key={item.analysisId}>
              <td>
                <Link to={`/analysis/${item.analysisId}`}>
                  {item.jobTitle || `공고 ${item.jobId}`}
                </Link>
              </td>
              <td>{item.companyName || "-"}</td>
              <td>{item.analysisStatus}</td>
              <td>{item.overallMatchRate ?? "-"}</td>
              <td>{item.evaluationCoverage ?? "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </main>
  );
}

export default AnalysisPage;
