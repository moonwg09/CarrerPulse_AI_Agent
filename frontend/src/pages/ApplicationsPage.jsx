import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NavBar from "../components/NavBar";
import PageToast from "../components/PageToast";
import { getApplications, updateApplication } from "../api/applicationApi";

const statusLabels = {
  APPLIED: "지원 완료",
  DOCUMENT_PASS: "서류 합격",
  INTERVIEW: "면접",
  FINAL_PASS: "최종 합격",
  REJECTED: "불합격",
};

export default function ApplicationsPage() {
  const [items, setItems] = useState([]);
  const [message, setMessage] = useState("");
  const [statusDrafts, setStatusDrafts] = useState({});
  const [notice, setNotice] = useState("");

  const load = () => getApplications().then(setItems).catch((error) => setMessage(error.message));
  useEffect(() => {
    let active = true;
    getApplications()
      .then(data => { if (active) setItems(data); })
      .catch(error => { if (active) setMessage(error.message); });
    return () => { active = false; };
  }, []);

  async function change(item, status, interviewDate) {
    try {
      setMessage("");
      await updateApplication(item.applicationId, status, interviewDate);
      setStatusDrafts(current => {
        const next = { ...current };
        delete next[item.applicationId];
        return next;
      });
      await load();
      setNotice("지원 상태를 변경했습니다.");
    } catch (error) { setMessage(error.message); }
  }

  const statusClass = (status) => `application-status application-status--${status.toLowerCase().replaceAll("_", "-")}`;

  return <main>
    <NavBar />
    <PageToast message={notice} onClose={setNotice} />
    <h1>지원 현황</h1>
    {items.length > 0 && <div className="application-list">
      {items.map((item, index) => {
        const draft = statusDrafts[item.applicationId];
        const displayedStatus = draft?.status || item.status;
        const displayedInterviewDate = draft?.interviewDate ?? item.interviewDate ?? "";
        return <article className="application-row" key={item.applicationId}>
        <span className="record-index">{String(index + 1).padStart(2, "0")}</span>
        <div className="application-primary">
          <span className="application-label">기업명</span>
          <strong>{item.companyName || "회사 미기재"}</strong>
          <span className="application-label application-label--job">직무명</span>
          <Link to={`/jobs/${item.jobId}`}>{item.jobTitle || "직무 미기재"}</Link>
        </div>
        <div className="application-location">
          <span className="application-label">회사 위치</span>
          <strong>{item.region || "지역 미기재"}</strong>
        </div>
        <div className="application-state">
          <span className="application-label">현재 상태</span>
          <select
            className={`${statusClass(displayedStatus)} application-status-select`}
            aria-label={`${item.companyName || "지원 공고"} 지원 상태 변경`}
            title="클릭하여 지원 상태 변경"
            value={displayedStatus}
            onChange={(event) => {
              const status = event.target.value;
              if (status === "INTERVIEW") {
                setStatusDrafts(current => ({
                  ...current,
                  [item.applicationId]: { status, interviewDate: item.interviewDate || "" },
                }));
              } else {
                change(item, status, null);
              }
            }}
          >
            {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
          {displayedStatus === "INTERVIEW" && <div className="application-interview-date">
            <label htmlFor={`interview-date-${item.applicationId}`}>면접일</label>
            <input
              id={`interview-date-${item.applicationId}`}
              type="date"
              value={displayedInterviewDate}
              onChange={(event) => setStatusDrafts(current => ({
                ...current,
                [item.applicationId]: { status: "INTERVIEW", interviewDate: event.target.value },
              }))}
            />
            <button
              className="button-compact"
              type="button"
              disabled={!displayedInterviewDate}
              onClick={() => change(item, "INTERVIEW", displayedInterviewDate)}
            >
              저장
            </button>
          </div>}
        </div>
      </article>;
      })}
    </div>}
    {!items.length && <p className="record-list-empty">등록된 지원 내역이 없습니다.</p>}
    {message && <p>{message}</p>}
  </main>;
}
