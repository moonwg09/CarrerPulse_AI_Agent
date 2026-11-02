import { useEffect, useState } from "react";
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

  return <main>
    <NavBar />
    <h1>지원 현황</h1>
    {items.map((item) => <section key={item.applicationId}>
      <h2>{item.jobTitle}</h2>
      <p>{item.companyName}</p>
      <label>상태 <select value={item.status} onChange={(event) => {
        const status = event.target.value;
        const date = status === "INTERVIEW" ? window.prompt("면접일을 YYYY-MM-DD로 입력하세요.", item.interviewDate || "") : null;
        if (status !== "INTERVIEW" || date) change(item, status, date);
      }}>
        {Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select></label>
      {item.interviewDate && <p>면접일: {item.interviewDate}</p>}
    </section>)}
    {!items.length && <p>등록된 지원 내역이 없습니다.</p>}
    {message && <p>{message}</p>}
  </main>;
}
