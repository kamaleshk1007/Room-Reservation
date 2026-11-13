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
  return res;
}

export async function getMe(): Promise<UserSummary> {
  return api.get<UserSummary>('/api/auth/me');
}

export async function logout(): Promise<void> {
  try {
    await api.post('/api/auth/logout');
  } finally {
    clearToken();
  }
}

export async function refreshToken(token: string): Promise<AuthResponse> {
  return api.post<AuthResponse>('/api/auth/refresh', { refreshToken: token });
}
