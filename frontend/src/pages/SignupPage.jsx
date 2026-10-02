import { useState } from "react";
import { signup } from "../api/authApi";
import { useNavigate } from "react-router-dom";

function SignupPage() {
  const [form, setForm] = useState({
    email: "",
    password: "",
    name: "",
    careerType: "NEW",
    careerYears: "",
    consented: false,
    consentVersion: "v1.0",
  });

  const [message, setMessage] = useState("");

  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;

    setForm((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    setMessage("");

    const requestData = {
      email: form.email,
      password: form.password,
      name: form.name,
      careerType: form.careerType,
      careerYears:
        form.careerType === "EXPERIENCED"
          ? Number(form.careerYears)
          : null,
      consented: form.consented,
      consentVersion: form.consentVersion,
    };

    try {
        
        await signup(requestData);

        navigate("/login");
      
    } catch (error) {
      setMessage(error.message);
    }
  };

  return (
    <div>
      <h1>회원가입</h1>

      <form onSubmit={handleSubmit}>
        <div>
          <label>이메일</label>
          <input
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
          />
        </div>

        <div>
          <label>비밀번호</label>
          <input
            type="password"
            name="password"
            value={form.password}
            onChange={handleChange}
          />
        </div>

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

        <div>
          <label>
            <input
              type="checkbox"
              name="consented"
              checked={form.consented}
              onChange={handleChange}
            />
            개인정보 수집 및 이용에 동의합니다.
          </label>
        </div>

        <button type="submit">
          회원가입
        </button>
      </form>

      {message && <p>{message}</p>}
    </div>
  );
}

export default SignupPage;