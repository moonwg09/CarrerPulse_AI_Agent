import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NavBar from "../components/NavBar";
import PageToast from "../components/PageToast";
import { compareDocumentWithJob, getAnalyses, resetAnalyses } from "../api/analysisApi";
import { collectJobs, searchJobs } from "../api/jobApi";
import { getMyJobs } from "../api/userApi";
import { regions } from "../constants/regions";

function initialRegions() {
  const savedRegions = localStorage.getItem("careerpulse.regions");
  if (savedRegions) {
    try {
      const parsed = JSON.parse(savedRegions);
      const valid = parsed.filter(item => regions.includes(item));
      if (valid.length > 0) return valid;
    } catch {
      // 기존 단일 지역 값으로 복구합니다.
    }
  }
  const savedRegion = localStorage.getItem("careerpulse.region");
  return regions.includes(savedRegion) ? [savedRegion] : ["전국"];
}

function initialJobs() {
  try {
    const saved = JSON.parse(sessionStorage.getItem("careerpulse.analysis.jobs") || "[]");
    const now = new Date();
    const today = [
      now.getFullYear(),
      String(now.getMonth() + 1).padStart(2, "0"),
      String(now.getDate()).padStart(2, "0"),
    ].join("-");
    return saved.filter(job => !job.deadline || job.deadline >= today);
  } catch {
    return [];
  }
}

function scoreText(value, status) {
  if (status !== "COMPLETED") return "재분석 필요";
  return value == null ? "평가 불가" : `${value}%`;
}

function collectionKeywords(interest) {
  const seen = new Set();
  return [interest.searchKeyword || interest.jobName, ...(interest.searchExpressions || [])]
    .filter(keyword => {
      const normalized = keyword.toLowerCase().replace(/\s+/g, "").replace(/개발자$/, "");
      if (!normalized || seen.has(normalized)) return false;
      seen.add(normalized);
      return true;
    })
    .slice(0, 3);
}

export default function AnalysisPage() {
  const [interests, setInterests] = useState([]);
  const [selectedInterest, setSelectedInterest] = useState("");
  const [selectedRegions, setSelectedRegions] = useState(initialRegions);
  const [jobs, setJobs] = useState(initialJobs);
  const [analyses, setAnalyses] = useState([]);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState("");
  const [notice, setNotice] = useState("");

  const loadAnalyses = () => getAnalyses().then(setAnalyses).catch(error => setMessage(error.message));
  useEffect(() => {
    getMyJobs().then(items => {
      setInterests(items);
      if (items.length) {
        const saved = sessionStorage.getItem("careerpulse.analysis.userJobId");
        const restored = items.some(item => String(item.userJobId) === saved);
        setSelectedInterest(restored ? saved : String(items[0].userJobId));
      }
    }).catch(error => setMessage(error.message));
    loadAnalyses();
  }, []);

  useEffect(() => {
    sessionStorage.setItem("careerpulse.analysis.jobs", JSON.stringify(jobs));
  }, [jobs]);

  const selected = interests.find(item => String(item.userJobId) === selectedInterest);

  function toggleRegion(item) {
    setSelectedRegions(current => {
      if (item === "전국") return current.includes(item) ? [] : [item];
      if (current.includes(item)) return current.filter(region => region !== item);
      return [...current.filter(region => region !== "전국"), item];
    });
  }

  async function collectAndSearch(event) {
    event.preventDefault();
    if (!selected || selectedRegions.length === 0) return;
    setBusy("collect"); setMessage("");
    try {
      const foundJobs = new Map();
      const keywords = collectionKeywords(selected);
      for (let index = 0; index < selectedRegions.length; index += 1) {
        const region = selectedRegions[index];
        for (let keywordIndex = 0; keywordIndex < keywords.length; keywordIndex += 1) {
          const keyword = keywords[keywordIndex];
          setMessage(`${region} ${keyword} 공고 수집 중 (${index + 1}/${selectedRegions.length})`);
          const collected = await collectJobs({ jobKeyword: keyword, region, limit: 10 });
          if (collected.collectedCount > 0) break;
        }
        const result = await searchJobs(selected.userJobId, region);
        result.forEach(job => foundJobs.set(job.jobId, job));
      }
      localStorage.setItem("careerpulse.regions", JSON.stringify(selectedRegions));
      localStorage.setItem("careerpulse.region", selectedRegions[0]);
      sessionStorage.setItem("careerpulse.analysis.userJobId", selectedInterest);
      const result = [...foundJobs.values()];
      setJobs(result);
      if (result.length > 0) {
        setMessage("");
        setNotice(`${selectedRegions.length}개 지역에서 조건에 맞는 공고 ${result.length}개를 수집·조회했습니다.`);
      } else {
        setMessage("선택한 직무와 지역에 현재 공개된 공고가 없습니다. 지역을 추가하거나 전국을 선택해 다시 조회해주세요.");
      }
    } catch (error) { setMessage(error.message); }
    finally { setBusy(""); }
  }

  async function analyze(jobId) {
    setBusy(String(jobId)); setMessage("");
    try {
      await compareDocumentWithJob({ jobId });
      setNotice("문서 근거 비교가 완료되었습니다.");
      await loadAnalyses();
    } catch (error) { setMessage(error.message); }
    finally { setBusy(""); }
  }

  async function resetAllAnalyses() {
    const confirmed = window.confirm(
      "분석 결과를 모두 초기화하시겠습니까? 업로드 문서와 수집 공고는 유지되며, 기존 학습계획은 무효화됩니다."
    );
    if (!confirmed) return;
    setBusy("reset"); setMessage("");
    try {
      const result = await resetAnalyses();
      setAnalyses([]);
      setNotice(`분석 결과 ${result.deletedCount}개를 초기화했습니다.`);
    } catch (error) { setMessage(error.message); }
    finally { setBusy(""); }
  }

  return <main>
    <NavBar />
    <PageToast message={notice} onClose={setNotice} />
    <h1>채용공고 분석</h1>
    <p>관심 직무와 지역으로 공고를 수집한 뒤 등록 문서를 근거로 비교합니다.</p>
    {!interests.length && <p><Link to="/jobs">관심 직무를 먼저 등록해주세요.</Link></p>}
    <form onSubmit={collectAndSearch}>
      <label>관심 직무 <select required value={selectedInterest} onChange={event => setSelectedInterest(event.target.value)}>
        {interests.map(item => <option key={item.userJobId} value={item.userJobId}>{item.jobName}</option>)}
      </select></label>{" "}
      <fieldset>
        <legend>지역 (복수 선택 가능)</legend>
        {regions.map(item => <label key={item}>
          <input type="checkbox" checked={selectedRegions.includes(item)} onChange={() => toggleRegion(item)} /> {item}{" "}
        </label>)}
      </fieldset>
      <button disabled={!selected || selectedRegions.length === 0 || !!busy}>{busy === "collect" ? "수집 중" : "공고 수집·조회"}</button>
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
    <button className="button-danger" type="button" disabled={!analyses.length || !!busy} onClick={resetAllAnalyses}>
      {busy === "reset" ? "초기화 중" : "분석 결과 전체 초기화"}
    </button>
    <table><thead><tr><th>공고</th><th>회사</th><th>상태</th><th>전체 일치율</th><th>평가 가능 비율</th></tr></thead>
      <tbody>{analyses.map(item => <tr key={item.analysisId}>
        <td><Link to={`/analysis/${item.analysisId}`}>{item.jobTitle || `공고 ${item.jobId}`}</Link></td>
        <td>{item.companyName || "-"}</td><td>{item.analysisStatus}</td>
        <td>{scoreText(item.overallMatchRate, item.analysisStatus)}</td>
        <td>{scoreText(item.evaluationCoverage, item.analysisStatus)}</td>
      </tr>)}</tbody>
    </table>
  </main>;
}
