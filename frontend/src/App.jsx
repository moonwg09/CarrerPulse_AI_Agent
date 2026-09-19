import { useState } from "react";

function App() {
  const [message, setMessage] = useState("");

  const testConnection = async (url) => {
    try {
      console.log("요청 URL:", url);
      const response = await fetch(url);

      if (!response.ok) {
        throw new Error(`HTTP 오류: ${response.status}`);
      }

      const data = await response.json();

      setMessage(data.message);
    } catch (error) {
      console.error(error);
      setMessage("서버 연결 실패");
    }
  };

  return (
    <div>
      <h1>CareerPulse AI</h1>

      <button onClick={() => testConnection("/api/test")}>
        Spring Boot 연결 테스트
      </button>

      <button onClick={() => testConnection("/api/python/test")}>
        Python 전체 연동 테스트
      </button>

      <p>{message}</p>
    </div>
  );
}

export default App;