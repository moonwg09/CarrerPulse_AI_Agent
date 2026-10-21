import { getCsrfToken } from "./userApi";

const BASE_URL = "http://localhost:8080/api/v1/analysis";

async function readBody(response) {
  return await response.json().catch(() => null);
}

// [서류와 공고비교] + [fast api 호출] + [결과저장]
export async function compareDocumentWithJob(data) {
  const csrf = await getCsrfToken();

  const response = await fetch(
    `${BASE_URL}/compare`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        [csrf.headerName]: csrf.token,
      },
      credentials: "include",
      body: JSON.stringify(data),
    }
  );

  const body = await readBody(response);

  if (!response.ok) {
    throw new Error(
      body?.message || "서류와 공고 비교에 실패했습니다."
    );
  }

  return body;
}

// 저장된 분석 결과 목록
export async function getAnalyses() {
  const response = await fetch(
    BASE_URL,
    {
      method: "GET",
      credentials: "include",
    }
  );

  const body = await readBody(response);

  if (!response.ok) {
    throw new Error(
      body?.message || "분석 결과를 불러오지 못했습니다."
    );
  }

  return body;
}

// 분석 결과 상세 (요구사항별 판정 + 근거 원문)
export async function getAnalysisDetail(analysisId) {
  const response = await fetch(
    `${BASE_URL}/${analysisId}`,
    {
      method: "GET",
      credentials: "include",
    }
  );

  const body = await readBody(response);

  if (!response.ok) {
    throw new Error(
      body?.message || "분석 결과 상세를 불러오지 못했습니다."
    );
  }

  return body;
}

// AI 응답 원본 JSON
export async function getAnalysisRaw(analysisId) {
  const response = await fetch(
    `${BASE_URL}/${analysisId}/raw`,
    {
      method: "GET",
      credentials: "include",
    }
  );

  const body = await readBody(response);

  if (!response.ok) {
    throw new Error(
      body?.message || "AI 원본 응답을 불러오지 못했습니다."
    );
  }

  return body;
}

// [추천조회]
export async function getRecommendations(status, limit) {
  const params = new URLSearchParams();

  if (status) {
    params.set("status", status);
  }

  if (limit) {
    params.set("limit", String(limit));
  }

  const query = params.toString();

  const response = await fetch(
    query ? `${BASE_URL}/recommendations?${query}`
          : `${BASE_URL}/recommendations`,
    {
      method: "GET",
      credentials: "include",
    }
  );

  const body = await readBody(response);

  if (!response.ok) {
    throw new Error(
      body?.message || "추천 공고를 불러오지 못했습니다."
    );
  }

  return body;
}

// AI 서버 연결 확인
export async function checkAiServer() {
  const response = await fetch(
    `${BASE_URL}/ai-health`,
    {
      method: "GET",
      credentials: "include",
    }
  );

  const body = await readBody(response);

  if (!response.ok) {
    throw new Error(
      body?.message || "AI 서버에 연결할 수 없습니다."
    );
  }

  return body;
}
