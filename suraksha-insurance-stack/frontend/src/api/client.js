import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function getCookie(name) {
  const match = document.cookie.match(new RegExp("(^| )" + name + "=([^;]+)"));
  return match ? match[2] : null;
}

const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // send the HttpOnly session cookie with every request
});

// Double-submit CSRF: echo the readable XSRF-TOKEN cookie back as a header
// on every state-changing request, matching the Spring Security config.
api.interceptors.request.use((config) => {
  const method = (config.method || "get").toLowerCase();
  if (["post", "put", "delete", "patch"].includes(method)) {
    const token = getCookie("XSRF-TOKEN");
    if (token) config.headers["X-XSRF-TOKEN"] = token;
  }
  return config;
});

export default api;
