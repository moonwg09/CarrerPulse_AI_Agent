export async function getMyInfo() {
  const response = await fetch(
    "http://localhost:8080/api/v1/users/me",
    {
      method: "GET",
      credentials: "include",
    }
  );

  const body = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      body?.message || "사용자 정보를 불러오지 못했습니다."
    );
  }

  return body;
}

export async function getCsrfToken() {
  const response = await fetch(
    "http://localhost:8080/api/v1/auth/csrf",
    {
      method: "GET",
      credentials: "include",
    }
  );

  const body = await response.json();

  if (!response.ok) {
    throw new Error("CSRF 토큰을 가져오지 못했습니다.");
  }

  return body;
}

export async function updateMyInfo(data) {
  const csrf = await getCsrfToken();

  const response = await fetch(
    "http://localhost:8080/api/v1/users/me",
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        [csrf.headerName]: csrf.token,
      },
      credentials: "include",
      body: JSON.stringify(data),
    }
  );

  const body = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      body?.message || "회원 정보 수정에 실패했습니다."
    );
  }

  return body;
}

export async function getMyJobs() {
  const response = await fetch(
    "http://localhost:8080/api/v1/users/me/jobs",
    {
      method: "GET",
      credentials: "include",
    }
  );

  const body = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      body?.message || "관심 직무를 불러오지 못했습니다."
    );
  }

  return body;
}

export async function addMyJob(data) {
  const csrf = await getCsrfToken();

  const response = await fetch(
    "http://localhost:8080/api/v1/users/me/jobs",
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

  const body = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      body?.message || "관심 직무 등록에 실패했습니다."
    );
  }

  return body;
}

export async function deleteMyJob(userJobId) {
  const csrf = await getCsrfToken();

  const response = await fetch(
    `http://localhost:8080/api/v1/users/me/jobs/${userJobId}`,
    {
      method: "DELETE",
      headers: {
        [csrf.headerName]: csrf.token,
      },
      credentials: "include",
    }
  );

  if (!response.ok) {
    const body = await response.json().catch(() => null);

    throw new Error(
      body?.message || "관심 직무 삭제에 실패했습니다."
    );
  }
}

export async function withdraw() {
  const csrf = await getCsrfToken();

  const response = await fetch(
    "http://localhost:8080/api/v1/users/me",
    {
      method: "DELETE",
      headers: {
        [csrf.headerName]: csrf.token,
      },
      credentials: "include",
    }
  );

  if (!response.ok) {
    const body = await response.json().catch(() => null);

    throw new Error(
      body?.message || "회원탈퇴에 실패했습니다."
    );
  }
}