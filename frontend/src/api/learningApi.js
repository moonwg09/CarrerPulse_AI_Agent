import { getCsrfToken } from "./userApi";

// 세션 쿠키와 CSRF 토큰을 함께 보내 기존 로그인 보안 흐름을 따른다.
export async function analyzeLearning(source, assessments) {
  const csrf = await getCsrfToken();
  const response = await fetch("/api/v1/learning/analysis/me", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
    body: JSON.stringify({ source, assessments }),
  });
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(body?.message || body?.detail || `학습 분석 실패 (${response.status}). Python 서버와 입력 내용을 확인하세요.`);
  }
  return body;
}

// 조회는 저장된 결과만 읽으며 Gemini를 다시 호출하지 않는다.
export async function getLatestLearningPlan() {
  const response = await fetch("/api/v1/learning/plans/me/latest", { credentials: "include" });
  const body = await response.json().catch(() => null);
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(body?.message || "저장된 계획을 불러오지 못했습니다.");
  return body;
}

export async function generateLearningPlan(input) {
  const csrf = await getCsrfToken();
  const response = await fetch("/api/v1/learning/plans/me", {
    method: "POST", credentials: "include",
    headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
    body: JSON.stringify(input),
  });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message || "학습계획 생성·저장에 실패했습니다.");
  return body;
}
