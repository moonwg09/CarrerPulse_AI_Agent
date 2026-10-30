import { getCsrfToken } from "./userApi";

async function read(response) {
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message || "지원 현황 처리에 실패했습니다.");
  return body;
}

export async function getApplications() {
  return read(await fetch("/api/v1/applications", { credentials: "include" }));
}

export async function createApplication(jobId) {
  const csrf = await getCsrfToken();
  return read(await fetch("/api/v1/applications", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
    body: JSON.stringify({ jobId }),
  }));
}

export async function updateApplication(applicationId, status, interviewDate) {
  const csrf = await getCsrfToken();
  return read(await fetch(`/api/v1/applications/${applicationId}`, {
    method: "PATCH",
    credentials: "include",
    headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
    body: JSON.stringify({ status, interviewDate: interviewDate || null }),
  }));
}
