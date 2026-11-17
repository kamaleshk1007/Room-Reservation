// src/api/client.ts
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export class ApiError extends Error {
  readonly status: number;
  readonly details?: Record<string, string>;

  constructor(message: string, status: number, details?: Record<string, string>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

function getToken(): string | null {
  return localStorage.getItem('accessToken');
}

function setToken(token: string): void {
  localStorage.setItem('accessToken', token);
}

function clearToken(): void {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

let refreshPromise: Promise<string> | null = null;

async function refreshToken(): Promise<string> {
  if (refreshPromise) return refreshPromise;
  const rt = localStorage.getItem('refreshToken');
  if (!rt) throw new Error('No refresh token');

  refreshPromise = fetch(`${API_BASE}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${rt}` },
  })
    .then(async (res) => {
      if (!res.ok) throw new Error('Refresh failed');
      const data = await res.json();
      setToken(data.accessToken);
      if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken);
      return data.accessToken as string;
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

async function request<T>(path: string, options: RequestInit = {}, retry = true): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401 && retry) {
    let newToken: string;
    try {
      newToken = await refreshToken();
    } catch {
      clearToken();
      window.location.href = '/login';
      throw new Error('Unauthorized');
    }
    const retryHeaders = { ...headers, Authorization: `Bearer ${newToken}` };
    const retryResponse = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: retryHeaders,
    });

    if (!retryResponse.ok) {
      const body = await retryResponse.json().catch(() => ({ error: 'Request failed' }));
      throw new ApiError(body.error || body.message || 'Request failed', retryResponse.status, body.details);
    }
    if (retryResponse.status === 204) return undefined as T;
    return retryResponse.json();
  }

  if (response.status === 401) {
    clearToken();
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({ error: 'Request failed' }));
    throw new ApiError(body.error || body.message || 'Request failed', response.status, body.details);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

export const api = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, body?: unknown) => request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body?: unknown) => request<T>(path, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};

export { getToken, setToken, clearToken };
export default api;
