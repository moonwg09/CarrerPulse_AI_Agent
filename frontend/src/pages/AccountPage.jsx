import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { logout } from "../api/authApi";
import { withdraw } from "../api/userApi";

function AccountPage() {
  const [message, setMessage] = useState("");

  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await logout();

      navigate("/login");
    } catch (error) {
      setMessage(error.message);
    }
  };

  const handleWithdraw = async () => {
    const confirmed = window.confirm(
      "정말 회원탈퇴하시겠습니까?"
    );

    if (!confirmed) {
      return;
    }

    try {
      await withdraw();

      navigate("/login");
    } catch (error) {
      setMessage(error.message);
    }
  };

  return (
    <div>
      <h1>계정 관리</h1>

      <button
        type="button"
        onClick={handleLogout}
      >
        로그아웃
      </button>

      <button
        type="button"
        onClick={handleWithdraw}
      >
        회원탈퇴
      </button>

      {message && <p>{message}</p>}
    </div>
  );
}

export default AccountPage;