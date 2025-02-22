const AUTH_API_URL = process.env.NEXT_PUBLIC_AUTH_API_URL || "http://localhost:8001";

export async function login(email: string, password: string) {
  const response = await fetch(`${AUTH_API_URL}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ username: email, password }),
  });

  if (!response.ok) {
    throw new Error("Invalid email or password");
  }
  return response.json(); // Returns { access_token, token_type }
}

export async function register(email: string, password: string) {
  const response = await fetch(`${AUTH_API_URL}/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });

  if (!response.ok) {
    throw new Error("Failed to register user");
  }
  return response.json();
}

export async function validateToken(token: string) {
  const response = await fetch(`${AUTH_API_URL}/validate-token`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token }),
  });

  if (!response.ok) {
    throw new Error("Invalid token");
  }
  return response.json(); // Returns { email, id }
}
