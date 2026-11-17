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
