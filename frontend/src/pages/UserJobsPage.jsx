import { useEffect, useState } from "react";
import NavBar from "../components/NavBar";
import {
  getMyJobs,
  addMyJob,
  deleteMyJob,
} from "../api/userApi";

function UserJobsPage() {
  const [jobs, setJobs] = useState([]);

  const [form, setForm] = useState({
    jobCode: "",
    jobName: "",
  });

  const [message, setMessage] = useState("");

  useEffect(() => {
    loadJobs();
  }, []);

  const loadJobs = async () => {
    try {
      const data = await getMyJobs();
      setJobs(data);
    } catch (error) {
      setMessage(error.message);
    }
  };

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
        jobCode: "",
        jobName: "",
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
    <div>
        <NavBar />
      <h1>관심 직무 관리</h1>

      <form onSubmit={handleAdd}>
        <div>
          <label>직무 코드</label>

          <input
            type="text"
            name="jobCode"
            value={form.jobCode}
            onChange={handleChange}
            placeholder="예: BACKEND"
          />
        </div>

        <div>
          <label>직무 이름</label>

          <input
            type="text"
            name="jobName"
            value={form.jobName}
            onChange={handleChange}
            placeholder="예: 백엔드 개발자"
          />
        </div>

        <button type="submit">
          관심 직무 추가
        </button>
      </form>

      {message && <p>{message}</p>}

      <hr />

      <h2>내 관심 직무</h2>

      {jobs.length === 0 ? (
        <p>등록된 관심 직무가 없습니다.</p>
      ) : (
        <ul>
          {jobs.map((job) => (
            <li key={job.userJobId}>
              {job.jobName} ({job.jobCode})

              <button
                type="button"
                onClick={() =>
                  handleDelete(job.userJobId)
                }
              >
                삭제
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default UserJobsPage;