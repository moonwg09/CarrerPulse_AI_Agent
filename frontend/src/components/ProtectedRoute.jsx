import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { getMyInfo, getMyJobs } from "../api/userApi";

function ProtectedRoute({ children, allowWithoutInterest = false }) {
  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);
  const [interestRequired, setInterestRequired] = useState(false);

  useEffect(() => {
    const checkLogin = async () => {
      try {
        await getMyInfo();
        setAuthenticated(true);
        if (!allowWithoutInterest) {
          const jobs = await getMyJobs();
          setInterestRequired(jobs.length === 0);
        }
      } catch (error) {
        setAuthenticated(false);
      } finally {
        setLoading(false);
      }
    };

    checkLogin();
  }, [allowWithoutInterest]);

  if (loading) {
    return <p>로그인 상태 확인 중...</p>;
  }

  if (!authenticated) {
    return <Navigate to="/login" replace />;
  }

  if (interestRequired) {
    return <Navigate to="/jobs" replace />;
  }

  return children;
}

export default ProtectedRoute;
