const AUTH_API_URL =
  process.env.NEXT_PUBLIC_AUTH_API_URL || "http://localhost:8001";

export async function loginUser(email: string, password: string) {
  const response = await fetch(`${AUTH_API_URL}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ username: email, password }),
    credentials: "include", // ✅ Ensure cookies are included
  });

  if (!response.ok) throw new Error("Invalid login credentials");
  return response.json();
}

export async function registerUser(email: string, password: string) {
  const response = await fetch(`${AUTH_API_URL}/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
    credentials: "include", // ✅ Ensure cookies are included
  });

  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.detail || "Registration failed");
  }
  return response.json();
}

export async function fetchUser() {
  const response = await fetch(`${AUTH_API_URL}/me`, {
    method: "GET",
    credentials: "include", // ✅ Ensure cookies are included
  });

  if (!response.ok) throw new Error("Unauthorized");
  return response.json();
}

export async function logoutUser() {
  await fetch(`${AUTH_API_URL}/logout`, {
    method: "POST",
    credentials: "include", // ✅ Ensure cookies are included
  });
}

export async function debugCookies() {
  const response = await fetch(`${AUTH_API_URL}/debug-cookies`, {
    method: "GET",
    credentials: "include",
  });
  const data = await response.json();
  console.log("Debug Cookies:", data);
  return data;
}