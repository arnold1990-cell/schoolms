import { ReactNode, createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authService } from '../services/authService';
import { UNAUTHORIZED_EVENT } from '../services/api';
import { MeResponse } from '../types';

const ACCESS_TOKEN_KEY = 'accessToken';
const AUTH_USER_KEY = 'authUser';

function parseStoredAuthUser(rawValue: string | null): MeResponse | null {
  if (!rawValue) return null;
  try {
    const parsed = JSON.parse(rawValue) as Partial<MeResponse>;
    if (
      typeof parsed?.id === 'number'
      && typeof parsed?.email === 'string'
      && (parsed.role === 'ADMIN' || parsed.role === 'TEACHER')
    ) {
      return { id: parsed.id, email: parsed.email, role: parsed.role };
    }
  } catch {
    // ignore invalid cached auth user
  }
  return null;
}

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
    if (import.meta.env.DEV) {
      console.debug('[Auth] logout triggered; clearing auth session');
    }
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(AUTH_USER_KEY);
    setUser(null);
    setToken(null);
  }, []);

  useEffect(() => {
    let active = true;

    const bootstrap = async () => {
      setLoading(true);
      if (import.meta.env.DEV) {
        console.debug('[Auth] bootstrap started');
      }

      const storedToken = localStorage.getItem(ACCESS_TOKEN_KEY);
      const storedUser = parseStoredAuthUser(localStorage.getItem(AUTH_USER_KEY));
      if (!active) {
        return;
      }

      setToken(storedToken);
      if (storedToken && storedUser) {
        setUser(storedUser);
      } else if (!storedToken && storedUser) {
        localStorage.removeItem(AUTH_USER_KEY);
      }

      if (!storedToken) {
        setLoading(false);
        setAuthReady(true);
        if (import.meta.env.DEV) {
          console.debug('[Auth] bootstrap completed (no stored token)');
        }
        return;
      }

      try {
        if (import.meta.env.DEV) {
          console.debug('[Auth] /api/auth/me request start (bootstrap)');
        }
        const me = await authService.me();
        if (!active) {
          return;
        }
        if (import.meta.env.DEV) {
          console.debug('[Auth] /api/auth/me request success (bootstrap)');
        }
        setUser(me);
        localStorage.setItem(AUTH_USER_KEY, JSON.stringify(me));
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
          if (import.meta.env.DEV) {
            console.debug('[Auth] bootstrap completed');
          }
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
      if (import.meta.env.DEV) {
        console.debug('[Auth] unauthorized event fired');
      }
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
      if (import.meta.env.DEV) {
        console.debug('[Auth] login success; token extracted');
      }
      localStorage.setItem(ACCESS_TOKEN_KEY, nextToken);
      if (import.meta.env.DEV) {
        console.debug('[Auth] token stored under accessToken');
      }
      setToken(nextToken);

      if (import.meta.env.DEV) {
        console.debug('[Auth] /api/auth/me request start (login)');
      }
      const me = await authService.me();
      if (import.meta.env.DEV) {
        console.debug('[Auth] /api/auth/me request success (login)');
      }
      setUser(me);
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(me));
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
