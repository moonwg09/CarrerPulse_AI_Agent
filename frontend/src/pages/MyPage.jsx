import { useEffect, useState } from "react";
import NavBar from "../components/NavBar";
import {
  getMyInfo,
  updateMyInfo,
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

  useEffect(() => {
    loadMyInfo();
  }, []);

  const loadMyInfo = async () => {
    try {
      const user = await getMyInfo();

      setForm({
        name: user.name ?? "",
        phone: user.phone ?? "",
        careerType: user.careerType ?? "NEW",
        careerYears: user.careerYears ?? "",
      });
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
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
    return <p>불러오는 중...</p>;
  }

  return (
    <div>
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

      {message && <p>{message}</p>}
    </div>
  );
}

export default MyPage;