const AUTH_API_URL = process.env.NEXT_PUBLIC_AUTH_API_URL || "http://localhost:8001";

export async function loginUser(email: string, password: string) {
  const response = await fetch(`${AUTH_API_URL}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ username: email, password }),
  });

  if (!response.ok) throw new Error("Invalid login credentials");
  return response.json(); // Returns { access_token, token_type }
}

export async function registerUser(email: string, password: string) {
  const response = await fetch(`${AUTH_API_URL}/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });

  if (!response.ok) throw new Error("Registration failed");
  return response.json();
}

export async function fetchUser(token: string) {
  const response = await fetch(`${AUTH_API_URL}/me`, {
    method: "GET",
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!response.ok) throw new Error("Unauthorized");
  return response.json();
}

export async function logoutUser(token: string) {
  await fetch(`${AUTH_API_URL}/logout`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token }),
  });

  localStorage.removeItem("token");
}
