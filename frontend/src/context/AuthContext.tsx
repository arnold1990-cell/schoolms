import { ReactNode, createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authService } from '../services/authService';
import { UNAUTHORIZED_EVENT } from '../services/api';
import { MeResponse } from '../types';

const ACCESS_TOKEN_KEY = 'accessToken';

export interface AuthContextValue {
  user: MeResponse | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  authReady: boolean;
  login: (email: string, password: string) => Promise<MeResponse>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [authReady, setAuthReady] = useState(false);

  const clearSession = useCallback(() => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    setUser(null);
    setToken(null);
  }, []);

  useEffect(() => {
    let active = true;

    const bootstrap = async () => {
      setLoading(true);

      const storedToken = localStorage.getItem(ACCESS_TOKEN_KEY);
      if (!active) {
        return;
      }

      setToken(storedToken);

      if (!storedToken) {
        setLoading(false);
        setAuthReady(true);
        return;
      }

      try {
        const me = await authService.me();
        if (!active) {
          return;
        }
        setUser(me);
      } catch (error: unknown) {
        const status = (error as { response?: { status?: number } })?.response?.status;
        if (!active) {
          return;
        }
        if (status === 401) {
          clearSession();
        } else if (import.meta.env.DEV) {
          console.warn('[Auth] Session bootstrap could not refresh /api/auth/me. Using cached user if present.', error);
        }
      } finally {
        if (active) {
          setLoading(false);
          setAuthReady(true);
        }
      }
    };

    void bootstrap();

    return () => {
      active = false;
    };
  }, [clearSession]);

  useEffect(() => {
    const onUnauthorized = () => {
      clearSession();
      setLoading(false);
      setAuthReady(true);
    };

    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, [clearSession]);

  const login = useCallback(async (email: string, password: string) => {
    setLoading(true);
    clearSession();

    try {
      const result = await authService.login(email, password);
      const nextToken = result.accessToken;
      localStorage.setItem(ACCESS_TOKEN_KEY, nextToken);
      setToken(nextToken);

      const me = await authService.me();
      setUser(me);
      setAuthReady(true);
      return me;
    } finally {
      setLoading(false);
    }
  }, [clearSession]);

  const logout = useCallback(() => {
    clearSession();
    setLoading(false);
    setAuthReady(true);
  }, [clearSession]);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    token,
    isAuthenticated: Boolean(user && token),
    loading,
    authReady,
    login,
    logout,
  }), [authReady, loading, login, logout, token, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
