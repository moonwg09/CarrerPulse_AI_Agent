import { useEffect, useState } from "react";
import NavBar from "../components/NavBar";
import PageToast from "../components/PageToast";
import {
  getMyInfo,
  updateMyInfo,
  getPreferredCompanies,
  searchWork24Companies,
  addPreferredCompany,
  deletePreferredCompany,
} from "../api/userApi";

const normalizeCompanyName = (value) => value
  .replace(/주식회사|\(주\)|㈜/giu, "")
  .replace(/[^\p{L}\p{N}]/gu, "")
  .toLocaleLowerCase("ko-KR");

const phonePattern = /^0\d{1,2}-?\d{3,4}-?\d{4}$/;

function MyPage() {
  const [form, setForm] = useState({
    name: "",
    phone: "",
    careerType: "NEW",
    careerYears: "",
  });

  const [message, setMessage] = useState("");
  const [profileNotice, setProfileNotice] = useState("");
  const [loading, setLoading] = useState(true);
  const [companies, setCompanies] = useState([]);
  const [companyName, setCompanyName] = useState("");
  const [companySuggestions, setCompanySuggestions] = useState([]);
  const [selectedCompanyName, setSelectedCompanyName] = useState("");
  const [companySearchLoading, setCompanySearchLoading] = useState(false);
  const [companySearchComplete, setCompanySearchComplete] = useState(false);

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

  useEffect(() => {
    const query = companyName.trim();
    if (query.length < 2 || query === selectedCompanyName) {
      return undefined;
    }

    let active = true;
    const timer = window.setTimeout(() => {
      searchWork24Companies(query)
        .then((results) => {
          if (active) {
            const normalizedQuery = normalizeCompanyName(query);
            const exactMatches = results.filter(
              (name) => normalizeCompanyName(name) === normalizedQuery,
            );
            if (exactMatches.length === 1) {
              setCompanyName(exactMatches[0]);
              setSelectedCompanyName(exactMatches[0]);
              setCompanySuggestions([]);
              setMessage("검색 결과에서 확인된 기업명으로 선택했습니다.");
            } else {
              setCompanySuggestions(results);
            }
            setCompanySearchComplete(true);
          }
        })
        .catch((error) => {
          if (active) {
            setCompanySuggestions([]);
            setCompanySearchComplete(false);
            setMessage(`${error.message} 기업명을 직접 입력해 등록할 수 있습니다.`);
          }
        })
        .finally(() => {
          if (active) setCompanySearchLoading(false);
        });
    }, 350);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [companyName, selectedCompanyName]);

  const addCompany = async () => {
    try {
      await addPreferredCompany(companyName.trim());
      setCompanies(await getPreferredCompanies());
      setCompanyName("");
      setSelectedCompanyName("");
      setCompanySuggestions([]);
      setCompanySearchLoading(false);
      setCompanySearchComplete(false);
      setMessage("");
      setProfileNotice("관심 기업을 등록했습니다.");
    } catch (error) { setMessage(error.message); }
  };

  const selectCompany = (name) => {
    setCompanyName(name);
    setSelectedCompanyName(name);
    setCompanySuggestions([]);
    setCompanySearchLoading(false);
    setCompanySearchComplete(false);
    setMessage("");
  };

  const removeCompany = async (id) => {
    try {
      await deletePreferredCompany(id);
      setCompanies(await getPreferredCompanies());
      setProfileNotice("관심 기업을 삭제했습니다.");
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
    const phone = form.phone.trim();
    if (phone && !phonePattern.test(phone)) {
      setMessage("전화번호를 010-1234-5678 형식으로 입력해 주세요.");
      return;
    }

    const requestData = {
      name: form.name,
      phone,
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

      setProfileNotice("회원 정보가 수정되었습니다.");
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
      <PageToast message={profileNotice} onClose={setProfileNotice} />
      <h1>내 정보</h1>

      <form className="profile-form" onSubmit={handleSubmit}>
        <div className="profile-field">
          <label>이름</label>
          <input
            type="text"
            name="name"
            value={form.name}
            onChange={handleChange}
          />
        </div>

        <div className="profile-field">
          <label>전화번호</label>
          <input
            type="tel"
            name="phone"
            value={form.phone}
            onChange={handleChange}
            inputMode="tel"
            maxLength={13}
            pattern="0[0-9]{1,2}-?[0-9]{3,4}-?[0-9]{4}"
            placeholder="010-1234-5678"
            title="지역번호 또는 010으로 시작하는 전화번호를 입력해 주세요. 하이픈은 생략할 수 있습니다."
          />
        </div>

        <div className="profile-field">
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
          <div className="profile-field">
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

      <section className="preferred-company-section">
        <h2>관심 기업</h2>
        <p>수집된 채용공고와 고용24 검색 결과를 참고하거나, 기업명을 직접 입력해 최대 3개까지 등록할 수 있습니다.</p>
        <div className="company-search-row">
          <input
            value={companyName}
            onChange={(event) => {
              const value = event.target.value;
              setCompanyName(value);
              setSelectedCompanyName("");
              setCompanySuggestions([]);
              setCompanySearchLoading(value.trim().length >= 2);
              setCompanySearchComplete(false);
              setMessage("");
            }}
            placeholder="기업명을 입력하거나 검색 결과에서 선택하세요."
            aria-label="관심 기업명 입력"
          />
          <button
            type="button"
            disabled={!companyName.trim() || companies.length >= 3}
            onClick={addCompany}
          >
            관심 기업 추가
          </button>
        </div>
        {companySearchLoading && <p className="company-search-help">고용24에서 검색 중...</p>}
        {selectedCompanyName && <p className="company-search-help">검색 결과의 기업명으로 선택되었습니다.</p>}
        {companySearchComplete && !companySearchLoading && !selectedCompanyName && companySuggestions.length === 0 && (
          <p className="company-search-help">검색 결과가 없습니다. 입력한 기업명을 직접 등록할 수 있습니다.</p>
        )}
        {companySuggestions.length > 0 && (
          <div className="company-suggestions" role="listbox" aria-label="고용24 기업 검색 결과">
            {companySuggestions.map((name) => (
              <button key={name} type="button" role="option" onClick={() => selectCompany(name)}>
                {name}
              </button>
            ))}
          </div>
        )}

        <div className="preferred-company-list">
          {companies.length === 0 && <p className="preferred-company-empty">등록된 관심 기업이 없습니다.</p>}
          {companies.map((company, index) => (
            <div className="preferred-company-row" key={company.preferredCompanyId}>
              <span className="preferred-company-index">{String(index + 1).padStart(2, "0")}</span>
              <strong>{company.companyName}</strong>
              <button className="button-compact button-danger" type="button" onClick={() => removeCompany(company.preferredCompanyId)}>삭제</button>
            </div>
          ))}
        </div>
      </section>

      {message && <p>{message}</p>}
    </main>
  );
}

export default MyPage;
