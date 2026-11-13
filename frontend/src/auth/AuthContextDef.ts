// src/auth/AuthContextDef.ts
import { createContext } from 'react';
import type { UserSummary } from '../api/auth';

interface AuthContextType {
  user: UserSummary | null;
  loading: boolean;
  login: (user: UserSummary) => void;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
export type { AuthContextType };
