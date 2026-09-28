export async function signup(data) {
  const response = await fetch(
    "http://localhost:8080/api/v1/auth/signup",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify(data),
    }
  );

  const responseBody = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      responseBody?.message || "회원가입에 실패했습니다."
    );
  }

  return responseBody;
}

export async function login(data) {
  const response = await fetch(
    "http://localhost:8080/api/v1/auth/login",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify(data),
    }
  );

  const responseBody = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      responseBody?.message || "로그인에 실패했습니다."
    );
  }

  return responseBody;
}

export async function getMyInfo() {
  const response = await fetch(
    "http://localhost:8080/api/v1/users/me",
    {
      method: "GET",
      credentials: "include",
    }
  );

  const responseBody = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      responseBody?.message || "사용자 정보를 불러오지 못했습니다."
    );
  }

  return responseBody;
}

export async function logout() {
  const csrfResponse = await fetch(
    "http://localhost:8080/api/v1/auth/csrf",
    {
      method: "GET",
      credentials: "include",
    }
  );

  const csrf = await csrfResponse.json();

  const response = await fetch(
    "http://localhost:8080/api/v1/auth/logout",
    {
      method: "POST",
      headers: {
        [csrf.headerName]: csrf.token,
      },
      credentials: "include",
    }
  );

  if (!response.ok) {
    const body = await response.json().catch(() => null);

    throw new Error(
      body?.message || "로그아웃에 실패했습니다."
    );
  }
}