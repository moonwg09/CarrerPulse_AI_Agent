import { getCsrfToken } from "./userApi";

export async function getDocuments() {
  const response = await fetch("/api/v1/documents", { credentials: "include" });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message || "문서 목록을 불러오지 못했습니다.");
  return body;
}

export async function uploadDocument(type, file) {
  const csrf = await getCsrfToken();
  const form = new FormData();
  form.append("file", file);
  const response = await fetch(`/api/v1/documents/${type}`, {
    method: "POST",
    credentials: "include",
    headers: { [csrf.headerName]: csrf.token },
    body: form,
  });
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message || "문서 업로드에 실패했습니다.");
  return body;
}

export async function deleteDocument(type) {
  const csrf = await getCsrfToken();
  const response = await fetch(`/api/v1/documents/${type}`, {
    method: "DELETE",
    credentials: "include",
    headers: { [csrf.headerName]: csrf.token },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message || "문서 삭제에 실패했습니다.");
  }
}
