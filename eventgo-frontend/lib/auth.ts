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

export async function registerUser(email: string, password: string, full_name: string, role: string = "user") {
  const response = await fetch(`${AUTH_API_URL}/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password, full_name, role }),
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

  const userData = await response.json();
  return {
    id: userData.id,
    email: userData.email,
    full_name: userData.full_name,
    role: userData.role,
  };
}

export async function logoutUser() {
  await fetch(`${AUTH_API_URL}/logout`, {
    method: "POST",
    credentials: "include", // ✅ Ensure cookies are included
  });
}

export async function searchUsers(email: string) {
  const response = await fetch(`${AUTH_API_URL}/search-users?email=${encodeURIComponent(email)}`, {
    method: "GET",
    credentials: "include", // ✅ Ensure cookies are included
  });
  if (!response.ok) {
    const errorData = await response.json();
    throw new Error(errorData.detail || "Search failed");
  }
  return response.json();
}