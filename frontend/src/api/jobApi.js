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

export async function getJob(jobId) {
  const response = await fetch(`/api/v1/jobs/${jobId}`, { credentials: "include" });
  const result = await body(response);
  if (!response.ok) throw new Error(result?.message || "채용공고를 불러오지 못했습니다.");
  return result;
}

export async function getBookmarkStatus(jobId) {
  const response = await fetch(`/api/v1/jobs/${jobId}/bookmark`, { credentials: "include" });
  const result = await body(response);
  if (!response.ok) throw new Error(result?.message || "관심 공고 상태를 불러오지 못했습니다.");
  return result;
}

export async function setBookmark(jobId, bookmarked) {
  const csrf = await getCsrfToken();
  const response = await fetch(`/api/v1/jobs/${jobId}/bookmark`, {
    method: bookmarked ? "POST" : "DELETE",
    headers: { [csrf.headerName]: csrf.token },
    credentials: "include",
  });
  if (!response.ok) {
    const result = await body(response);
    throw new Error(result?.message || "관심 공고 변경에 실패했습니다.");
  }
  return bookmarked;
}

export async function getBookmarks() {
  const response = await fetch("/api/v1/jobs/bookmarks", {
    credentials: "include",
  });
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(body?.message || "관심 공고를 불러오지 못했습니다.");
  }
  return body;
}
