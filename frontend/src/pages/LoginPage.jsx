import { useState } from "react";
import { login, getMyInfo } from "../api/authApi";
import { Link, useNavigate } from "react-router-dom";

function LoginPage() {
  const [form, setForm] = useState({
    email: "",
    password: "",
  });

  const [message, setMessage] = useState("");
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

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
    setUser(null);

    try {
      await login(form);

      navigate("/mypage");

    } catch (error) {
      setMessage(error.message);
    }
  };

  return (
    <div>
      <h1>로그인</h1>

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

        <button type="submit">
          로그인
        </button>
      </form>

      <p>
        아직 회원이 아니신가요?{" "}
        <Link to="/signup">회원가입</Link>
      </p>

      {message && <p>{message}</p>}

      {user && (
        <div>
          <h2>로그인 사용자 정보</h2>

          <p>이름: {user.name}</p>
          <p>이메일: {user.email}</p>
          <p>경력 유형: {user.careerType}</p>
        </div>
      )}
    </div>
  );
}

export default LoginPage;