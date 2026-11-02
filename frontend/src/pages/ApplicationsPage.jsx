import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import NavBar from "../components/NavBar";
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
      await updateApplication(item.applicationId, status, interviewDate);
      await load();
    } catch (error) { setMessage(error.message); }
  }

  const statusClass = (status) => `application-status application-status--${status.toLowerCase().replaceAll("_", "-")}`;

  return <main>
    <NavBar />
    <h1>지원 현황</h1>
    {items.length > 0 && <div className="application-list">
      {items.map((item, index) => <article className="application-row" key={item.applicationId}>
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
            className={`${statusClass(item.status)} application-status-select`}
            aria-label={`${item.companyName || "지원 공고"} 지원 상태 변경`}
            title="클릭하여 지원 상태 변경"
            value={item.status}
            onChange={(event) => {
              const status = event.target.value;
              const date = status === "INTERVIEW" ? window.prompt("면접일을 YYYY-MM-DD로 입력하세요.", item.interviewDate || "") : null;
              if (status !== "INTERVIEW" || date) change(item, status, date);
            }}
          >
            {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
          {item.interviewDate && <small>면접일 · {item.interviewDate}</small>}
        </div>
      </article>)}
    </div>}
    {!items.length && <p className="record-list-empty">등록된 지원 내역이 없습니다.</p>}
    {message && <p>{message}</p>}
  </main>;
}
