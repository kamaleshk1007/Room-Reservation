// src/pages/SsoPage.tsx
import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { handoffLogin } from '../api/auth';
import { useAuth } from '../auth/useAuth';

const TOKEN_PREFIX = '#token=';

const MISSING_TOKEN_MESSAGE =
  'This sign-in link is missing its hand-off token. Open Room Reserve from the college portal.';

function readTokenFromHash(): string | null {
  const hash = window.location.hash;
  if (!hash.startsWith(TOKEN_PREFIX)) {
    return null;
  }
  try {
    return decodeURIComponent(hash.slice(TOKEN_PREFIX.length));
  } catch {
    return null;
  }
}

export function SsoPage() {
  const [token] = useState(readTokenFromHash);
  const [error, setError] = useState<string | null>(token ? null : MISSING_TOKEN_MESSAGE);
  const started = useRef(false);
  const navigate = useNavigate();
  const { login } = useAuth();

  useEffect(() => {
    // Remove the fragment before doing anything else so the token leaves the address bar.
    if (window.location.hash) {
      window.history.replaceState({}, '', `${window.location.pathname}${window.location.search}`);
    }
    if (!token || started.current) {
      return;
    }
    // StrictMode runs effects twice; the token is single use, so only one request may run.
    started.current = true;

    handoffLogin(token)
      .then((user) => {
        login(user);
        navigate('/dashboard', { replace: true });
      })
      .catch((err: unknown) => {
        setError(err instanceof Error ? err.message : 'Hand-off sign-in failed.');
      });
  }, [token, login, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-sm p-8 bg-white rounded-lg shadow-sm border border-gray-200">
        <h1 className="text-2xl font-semibold text-gray-900 mb-2">Room Reserve</h1>
        {error ? (
          <>
            <p className="text-sm text-gray-600 mb-4">Could not complete the hand-off sign-in.</p>
            <div role="alert" className="p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-700 mb-4">
              {error}
            </div>
            <Link to="/login" className="text-sm font-medium text-blue-700 hover:text-blue-800">
              Back to login
            </Link>
          </>
        ) : (
          <p role="status" className="text-sm text-gray-600">Signing you in...</p>
        )}
        <p className="text-xs text-gray-500 mt-6">
          Demo hand-off sign-in: the link expires after 60 seconds and works once.
        </p>
      </div>
    </div>
  );
}
