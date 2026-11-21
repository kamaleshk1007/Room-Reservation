// src/auth/AuthContext.tsx
import { useState, useEffect, useRef, type ReactNode } from 'react';
import { getMe, logout as apiLogout, ssoPortal } from '../api/auth';
import type { UserSummary } from '../api/auth';
import { clearToken } from '../api/client';
import { AuthContext } from './AuthContextDef';

const PORTAL_LOGOUT_PARAM = 'portalLogout';
const HANDOFF_PATH = '/sso';

/**
 * The portal redirects here with ?portalLogout=1 after clearing its own cookie.
 * Drop the Room Reserve tokens and strip the flag before the app renders.
 */
function consumePortalLogout(): boolean {
  const url = new URL(window.location.href);
  if (!url.searchParams.has(PORTAL_LOGOUT_PARAM)) {
    return false;
  }
  clearToken();
  url.searchParams.delete(PORTAL_LOGOUT_PARAM);
  window.history.replaceState({}, '', `${url.pathname}${url.search}${url.hash}`);
  return true;
}

const portalLogoutAtLoad = consumePortalLogout();

/** The hand-off page signs in with its own token, so cookie SSO must not race it. */
const handoffAtLoad = window.location.pathname === HANDOFF_PATH;

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [loading, setLoading] = useState(!portalLogoutAtLoad && !handoffAtLoad);
  const bootstrapStarted = useRef(false);

  useEffect(() => {
    if (portalLogoutAtLoad || handoffAtLoad || bootstrapStarted.current) {
      return;
    }
    // StrictMode runs effects twice; only one session bootstrap may run.
    bootstrapStarted.current = true;

    const token = localStorage.getItem('accessToken');
    const restore = token ? getMe().catch(() => null) : ssoPortal();
    restore
      .then((restored) => setUser(restored))
      .finally(() => setLoading(false));
  }, []);

  const login = (u: UserSummary) => setUser(u);
  const logout = async () => {
    await apiLogout();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
