export function saveToken(token: string) {
    localStorage.setItem("eventgo_token", token);
  }
  
  export function getToken(): string | null {
    return localStorage.getItem("eventgo_token");
  }
  
  export function removeToken() {
    localStorage.removeItem("eventgo_token");
  }
  