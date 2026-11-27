import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import SignupPage from "./pages/SignupPage";
import LoginPage from "./pages/LoginPage";
import MyPage from "./pages/MyPage";
import UserJobsPage from "./pages/UserJobsPage";
import AccountPage from "./pages/AccountPage";
import AnalysisPage from "./pages/AnalysisPage";
import AnalysisDetailPage from "./pages/AnalysisDetailPage";
import RecommendationPage from "./pages/RecommendationPage";
import LearningPage from "./pages/LearningPage";
import DocumentsPage from "./pages/DocumentsPage";
import JobDetailPage from "./pages/JobDetailPage";
import ApplicationsPage from "./pages/ApplicationsPage";
import BookmarksPage from "./pages/BookmarksPage";

import ProtectedRoute from "./components/ProtectedRoute";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/learning" element={<ProtectedRoute><LearningPage /></ProtectedRoute>} />
        <Route path="/documents" element={<ProtectedRoute><DocumentsPage /></ProtectedRoute>} />
        <Route
          path="/"
          element={<Navigate to="/login" replace />}
        />

        <Route
          path="/signup"
          element={<SignupPage />}
        />

        <Route
          path="/login"
          element={<LoginPage />}
        />

        <Route
          path="/mypage"
          element={
            <ProtectedRoute>
              <MyPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/jobs"
          element={
            <ProtectedRoute allowWithoutInterest>
              <UserJobsPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/jobs/:jobId"
          element={<ProtectedRoute><JobDetailPage /></ProtectedRoute>}
        />

        <Route
          path="/account"
          element={
            <ProtectedRoute>
              <AccountPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/analysis"
          element={
            <ProtectedRoute>
              <AnalysisPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/analysis/:analysisId"
          element={
            <ProtectedRoute>
              <AnalysisDetailPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="/recommendations"
          element={
            <ProtectedRoute>
              <RecommendationPage />
            </ProtectedRoute>
          }
        />
        <Route path="/applications" element={<ProtectedRoute><ApplicationsPage /></ProtectedRoute>} />
        <Route path="/bookmarks" element={<ProtectedRoute><BookmarksPage /></ProtectedRoute>} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
