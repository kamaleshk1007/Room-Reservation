// src/auth/AuthContext.tsx
import { useState, useEffect, type ReactNode } from 'react';
import { getMe, logout as apiLogout, ssoPortal } from '../api/auth';
import type { UserSummary } from '../api/auth';
import { AuthContext } from './AuthContextDef';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [loading, setLoading] = useState(() => !!localStorage.getItem('accessToken'));

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      getMe()
        .then(setUser)
        .catch(() => setUser(null))
        .finally(() => setLoading(false));
      return;
    }
    // No stored session: try the portal cookie before showing the login page
    ssoPortal()
      .then((user) => setUser(user))
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
