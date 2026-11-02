import { useEffect, useState } from "react";
import NavBar from "../components/NavBar";
import {
  getMyInfo,
  updateMyInfo,
  getPreferredCompanies,
  addPreferredCompany,
  deletePreferredCompany,
} from "../api/userApi";

function MyPage() {
  const [form, setForm] = useState({
    name: "",
    phone: "",
    careerType: "NEW",
    careerYears: "",
  });

  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [companies, setCompanies] = useState([]);
  const [companyName, setCompanyName] = useState("");

  useEffect(() => {
    let active = true;
    Promise.all([getMyInfo(), getPreferredCompanies()])
      .then(([user, preferredCompanies]) => {
        if (!active) return;
        setCompanies(preferredCompanies);
        setForm({
          name: user.name ?? "",
          phone: user.phone ?? "",
          careerType: user.careerType ?? "NEW",
          careerYears: user.careerYears ?? "",
        });
      })
      .catch((error) => { if (active) setMessage(error.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const addCompany = async () => {
    try {
      await addPreferredCompany(companyName);
      setCompanies(await getPreferredCompanies());
      setCompanyName("");
    } catch (error) { setMessage(error.message); }
  };

  const removeCompany = async (id) => {
    try {
      await deletePreferredCompany(id);
      setCompanies(await getPreferredCompanies());
    } catch (error) { setMessage(error.message); }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    setMessage("");

    const requestData = {
      name: form.name,
      phone: form.phone,
      careerType: form.careerType,
      careerYears:
        form.careerType === "EXPERIENCED"
          ? Number(form.careerYears)
          : null,
    };

    try {
      const result = await updateMyInfo(requestData);

      setForm({
        name: result.name ?? "",
        phone: result.phone ?? "",
        careerType: result.careerType ?? "NEW",
        careerYears: result.careerYears ?? "",
      });

      setMessage("회원 정보가 수정되었습니다.");
    } catch (error) {
      setMessage(error.message);
    }
  };

  if (loading) {
    return <main><NavBar /><p>불러오는 중...</p></main>;
  }

  return (
    <main>
        <NavBar />
      <h1>내 정보</h1>

      <form onSubmit={handleSubmit}>
        <div>
          <label>이름</label>
          <input
            type="text"
            name="name"
            value={form.name}
            onChange={handleChange}
          />
        </div>

        <div>
          <label>전화번호</label>
          <input
            type="text"
            name="phone"
            value={form.phone}
            onChange={handleChange}
          />
        </div>

        <div>
          <label>경력 유형</label>

          <select
            name="careerType"
            value={form.careerType}
            onChange={handleChange}
          >
            <option value="NEW">신입</option>
            <option value="EXPERIENCED">경력</option>
          </select>
        </div>

        {form.careerType === "EXPERIENCED" && (
          <div>
            <label>경력 연수</label>
            <input
              type="number"
              step="0.1"
              name="careerYears"
              value={form.careerYears}
              onChange={handleChange}
            />
          </div>
        )}

        <button type="submit">
          수정하기
        </button>
      </form>

      <h2>관심 기업</h2>
      <p>최대 3개까지 등록할 수 있습니다.</p>
      <input
        value={companyName}
        onChange={(event) => setCompanyName(event.target.value)}
        placeholder="기업명"
      />
      <button type="button" disabled={!companyName.trim() || companies.length >= 3} onClick={addCompany}>
        관심 기업 추가
      </button>
      <ul>
        {companies.map((company) => (
          <li key={company.preferredCompanyId}>
            {company.companyName}
            <button type="button" onClick={() => removeCompany(company.preferredCompanyId)}>삭제</button>
          </li>
        ))}
      </ul>

      {message && <p>{message}</p>}
    </main>
  );
}

export default MyPage;
