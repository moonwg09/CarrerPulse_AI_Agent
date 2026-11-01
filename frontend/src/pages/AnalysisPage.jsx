import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NavBar from "../components/NavBar";
import { compareDocumentWithJob, getAnalyses } from "../api/analysisApi";
import { collectJobs, searchJobs } from "../api/jobApi";
import { getMyJobs } from "../api/userApi";

export default function AnalysisPage() {
  const [interests, setInterests] = useState([]);
  const [selectedInterest, setSelectedInterest] = useState("");
  const [region, setRegion] = useState(() => localStorage.getItem("careerpulse.region") || "");
  const [jobs, setJobs] = useState([]);
  const [analyses, setAnalyses] = useState([]);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState("");

  const loadAnalyses = () => getAnalyses().then(setAnalyses).catch(error => setMessage(error.message));
  useEffect(() => {
    getMyJobs().then(items => {
      setInterests(items);
      if (items.length) setSelectedInterest(String(items[0].userJobId));
    }).catch(error => setMessage(error.message));
    loadAnalyses();
  }, []);

  const selected = interests.find(item => String(item.userJobId) === selectedInterest);

  async function collectAndSearch(event) {
    event.preventDefault();
    if (!selected) return;
    setBusy("collect"); setMessage("");
    try {
      const collected = await collectJobs({
        jobKeyword: selected.searchKeyword || selected.jobName,
        region,
        limit: 10,
      });
      localStorage.setItem("careerpulse.region", region.trim());
      const result = await searchJobs(selected.userJobId, region);
      setJobs(result);
      setMessage(`${collected.collectedCount}개를 수집했고, 조건에 맞는 공고 ${result.length}개를 찾았습니다.`);
    } catch (error) { setMessage(error.message); }
    finally { setBusy(""); }
  }

  async function analyze(jobId) {
    setBusy(String(jobId)); setMessage("");
    try {
      await compareDocumentWithJob({ jobId });
      setMessage("문서 근거 비교가 완료되었습니다.");
      await loadAnalyses();
    } catch (error) { setMessage(error.message); }
    finally { setBusy(""); }
  }

  return <main>
    <NavBar />
    <h1>채용공고 분석</h1>
    <p>관심 직무와 지역으로 공고를 수집한 뒤 등록 문서를 근거로 비교합니다.</p>
    {!interests.length && <p><Link to="/jobs">관심 직무를 먼저 등록해주세요.</Link></p>}
    <form onSubmit={collectAndSearch}>
      <label>관심 직무 <select required value={selectedInterest} onChange={event => setSelectedInterest(event.target.value)}>
        {interests.map(item => <option key={item.userJobId} value={item.userJobId}>{item.jobName}</option>)}
      </select></label>{" "}
      <label>지역 <input required value={region} onChange={event => setRegion(event.target.value)} placeholder="서울특별시 또는 전국" /></label>{" "}
      <button disabled={!selected || !region.trim() || !!busy}>{busy === "collect" ? "수집 중" : "공고 수집·조회"}</button>
    </form>
    {message && <p role="status">{message}</p>}

    {jobs.length > 0 && <>
      <h2>수집 공고</h2>
      <table><thead><tr><th>공고</th><th>회사</th><th>지역</th><th>마감일</th><th>분석</th></tr></thead>
        <tbody>{jobs.map(job => <tr key={job.jobId}>
          <td><Link to={`/jobs/${job.jobId}`}>{job.title}</Link></td>
          <td>{job.companyName}</td><td>{job.region}</td><td>{job.deadline || "-"}</td>
          <td><button disabled={!!busy} onClick={() => analyze(job.jobId)}>{busy === String(job.jobId) ? "분석 중" : "근거 비교"}</button></td>
        </tr>)}</tbody>
      </table>
    </>}

    <h2>분석 결과</h2>
    <table><thead><tr><th>공고</th><th>회사</th><th>상태</th><th>전체 일치율</th><th>평가 가능 비율</th></tr></thead>
      <tbody>{analyses.map(item => <tr key={item.analysisId}>
        <td><Link to={`/analysis/${item.analysisId}`}>{item.jobTitle || `공고 ${item.jobId}`}</Link></td>
        <td>{item.companyName || "-"}</td><td>{item.analysisStatus}</td><td>{item.overallMatchRate ?? "-"}</td><td>{item.evaluationCoverage ?? "-"}</td>
      </tr>)}</tbody>
    </table>
  </main>;
}
