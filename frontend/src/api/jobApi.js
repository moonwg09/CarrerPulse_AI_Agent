import { getCsrfToken } from "./userApi";

async function body(response) {
  return await response.json().catch(() => null);
}

export async function collectJobs(input) {
  const csrf = await getCsrfToken();
  const response = await fetch("/api/v1/jobs/collect", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
    body: JSON.stringify(input),
  });
  const result = await body(response);
  if (!response.ok) throw new Error(result?.message || "채용공고 수집에 실패했습니다.");
  return result;
}

export async function searchJobs(userJobId, region, keyword = "") {
  const params = new URLSearchParams({ userJobId: String(userJobId), region });
  if (keyword.trim()) params.set("keyword", keyword.trim());
  const response = await fetch(`/api/v1/jobs?${params}`, { credentials: "include" });
  const result = await body(response);
  if (!response.ok) throw new Error(result?.message || "채용공고 조회에 실패했습니다.");
  return result;
}
