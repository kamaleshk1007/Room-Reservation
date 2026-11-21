// src/api/auth.ts
import api, { setToken, clearToken } from './client';

export interface LoginRequest {
  portalStaffId: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
}

export interface UserSummary {
  id: string;
  portalStaffId: string;
  name: string;
  role: string;
}

export async function login(req: LoginRequest): Promise<AuthResponse> {
  const res = await api.post<AuthResponse>('/api/auth/login', req);
  setToken(res.accessToken);
  if (res.refreshToken) localStorage.setItem('refreshToken', res.refreshToken);
  return res;
}

export async function getMe(): Promise<UserSummary> {
  return api.get<UserSummary>('/api/auth/me');
}

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export async function ssoPortal(): Promise<UserSummary | null> {
  try {
    const res = await fetch(`${API_BASE}/api/auth/portal-sso`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
    });
    if (!res.ok) return null;
    const data = await res.json();
    setToken(data.accessToken);
    if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken);
    return await getMe();
  } catch {
    return null;
  }
}

interface HandoffResponse {
  accessToken?: string;
  refreshToken?: string;
  message?: string;
  error?: string;
}

/**
 * Exchanges a single-use hand-off token for Room Reserve tokens. The backend
 * message is surfaced as-is so the /sso page can explain what went wrong.
 */
export async function handoffLogin(token: string): Promise<UserSummary> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}/api/auth/portal-handoff`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token }),
    });
  } catch {
    throw new Error('Could not reach Room Reserve. Try again.');
  }

  const body = (await res.json().catch(() => null)) as HandoffResponse | null;
  if (!res.ok) {
    throw new Error(body?.message || body?.error || 'Hand-off sign-in failed.');
  }
  if (!body?.accessToken) {
    throw new Error('Room Reserve returned an invalid response.');
  }

  setToken(body.accessToken);
  if (body.refreshToken) localStorage.setItem('refreshToken', body.refreshToken);
  return await getMe();
}

export async function logout(): Promise<void> {
  const rt = localStorage.getItem('refreshToken');
  try {
    if (rt) {
      await fetch(`${(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080')}/api/auth/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${rt}` },
      });
    }
  } finally {
    clearToken();
  }
}
