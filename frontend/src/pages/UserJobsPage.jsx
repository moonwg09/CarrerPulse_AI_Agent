import { useEffect, useState } from "react";
import NavBar from "../components/NavBar";
import {
  getMyJobs,
  getDevelopmentJobCatalog,
  addMyJob,
  deleteMyJob,
} from "../api/userApi";

function UserJobsPage() {
  const [jobs, setJobs] = useState([]);
  const [catalog, setCatalog] = useState([]);

  const [form, setForm] = useState({
    jobCode: "",
  });

  const [message, setMessage] = useState("");

  async function loadJobs() {
    try {
      const data = await getMyJobs();
      setJobs(data);
    } catch (error) {
      setMessage(error.message);
    }
  }

  useEffect(() => {
    let active = true;
    getMyJobs()
      .then((items) => { if (active) setJobs(items); })
      .catch((error) => { if (active) setMessage(error.message); });
    getDevelopmentJobCatalog().then((items) => {
      if (!active) return;
      setCatalog(items);
      if (items.length) setForm({ jobCode: items[0].jobCode });
    }).catch((error) => { if (active) setMessage(error.message); });
    return () => { active = false; };
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleAdd = async (e) => {
    e.preventDefault();

    setMessage("");

    try {
      await addMyJob(form);

      setForm({
        jobCode: catalog[0]?.jobCode || "",
      });

      setMessage("관심 직무가 등록되었습니다.");

      await loadJobs();
    } catch (error) {
      setMessage(error.message);
    }
  };

  const handleDelete = async (userJobId) => {
    setMessage("");

    try {
      await deleteMyJob(userJobId);

      setMessage("관심 직무가 삭제되었습니다.");

      await loadJobs();
    } catch (error) {
      setMessage(error.message);
    }
  };

  return (
    <main>
        <NavBar />
      <h1>관심 직무 관리</h1>

      <form onSubmit={handleAdd}>
        <label>개발 직무
          <select name="jobCode" value={form.jobCode} onChange={handleChange} required>
            {catalog.map((job) => (
              <option key={job.jobCode} value={job.jobCode}>{job.jobName}</option>
            ))}
          </select>
        </label>

        <button type="submit" disabled={jobs.length >= 3 || !form.jobCode}>
          관심 직무 추가
        </button>
        {jobs.length >= 3 && <p>관심 직무는 최대 3개까지 등록할 수 있습니다.</p>}
      </form>

      {message && <p>{message}</p>}

      <hr />

      <h2>내 관심 직무</h2>

      {jobs.length === 0 ? (
        <p className="record-list-empty">등록된 관심 직무가 없습니다.</p>
      ) : (
        <div className="record-list">
          {jobs.map((job, index) => (
            <div className="record-row" key={job.userJobId}>
              <span className="record-index">{String(index + 1).padStart(2, "0")}</span>
              <div>
                <strong>{job.jobName}</strong>
                <small className="record-meta">직무 코드 · {job.jobCode}</small>
              </div>
              <button
                className="button-compact button-danger"
                type="button"
                disabled={jobs.length <= 1}
                onClick={() =>
                  handleDelete(job.userJobId)
                }
              >
                삭제
              </button>
            </div>
          ))}
        </div>
      )}
      {jobs.length === 1 && <p>관심 직무는 최소 1개를 유지해야 합니다.</p>}
    </main>
  );
}

export default UserJobsPage;
