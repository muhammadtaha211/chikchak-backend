// Thin fetch wrapper around the backend's ApiResponse envelope:
// { success, message, data, timestamp } — callers get `data` directly,
// and failures (HTTP errors or success=false) throw ApiError with the
// backend's message.

const BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');

const TOKEN_KEY = 'chikchak_admin_token';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

// Registered by AuthProvider: called on any 401 so the app can log out
// and bounce to the login page.
let onUnauthorized = null;
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn;
}

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

export async function api(path, { method = 'GET', body, params } = {}) {
  let query = '';
  if (params) {
    const search = new URLSearchParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        search.set(key, value);
      }
    }
    const qs = search.toString();
    if (qs) query = `?${qs}`;
  }

  const headers = {};
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}${query}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(
      'Cannot reach the server. Check VITE_API_BASE_URL and that the backend is running.',
      0
    );
  }

  if (response.status === 401) {
    if (onUnauthorized) onUnauthorized();
    throw new ApiError('Session expired. Please log in again.', 401);
  }

  let json = null;
  try {
    json = await response.json();
  } catch {
    // Non-JSON body (e.g. empty error response) — fall through to status check.
  }

  if (!response.ok || (json && json.success === false)) {
    const message =
      (json && json.message) ||
      (response.status === 403
        ? 'You do not have permission to do that (admin role required).'
        : `Request failed (HTTP ${response.status})`);
    throw new ApiError(message, response.status);
  }

  return json ? json.data : null;
}
