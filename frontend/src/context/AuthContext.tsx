import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { MeResponse } from '../types';
import { authService } from '../services/authService';
import { UNAUTHORIZED_EVENT } from '../services/api';

interface AuthContextType {
  user: MeResponse | null;
  loading: boolean;
  authReady: boolean;
  login: (email: string, password: string) => Promise<MeResponse>;
  logout: () => void;
}

const ACCESS_TOKEN_KEY = 'accessToken';
const AUTH_USER_KEY = 'authUser';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function readStoredUser(): MeResponse | null {
  const serialized = localStorage.getItem(AUTH_USER_KEY);
  if (!serialized) {
    return null;
  }

  try {
    const parsed = JSON.parse(serialized) as Partial<MeResponse>;
    if (typeof parsed?.id !== 'number') {
      return null;
    }
    if (typeof parsed?.email !== 'string' || !parsed.email.trim()) {
      return null;
    }
    if (parsed.role !== 'ADMIN' && parsed.role !== 'TEACHER') {
      return null;
    }
    return {
      id: parsed.id,
      email: parsed.email,
      role: parsed.role,
    };
  } catch {
    return null;
  }
}

function persistUser(user: MeResponse | null): void {
  if (!user) {
    localStorage.removeItem(AUTH_USER_KEY);
    return;
  }
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [authReady, setAuthReady] = useState(false);

  const clearSession = useCallback(() => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(AUTH_USER_KEY);
    setUser(null);
  }, []);

  useEffect(() => {
    let mounted = true;

    const bootstrapAuth = async () => {
      const token = localStorage.getItem(ACCESS_TOKEN_KEY);
      const cachedUser = readStoredUser();

      if (cachedUser && mounted) {
        setUser(cachedUser);
      }

      if (!token) {
        if (mounted) {
          setLoading(false);
          setAuthReady(true);
        }
        return;
      }

      try {
        const me = await authService.me();
        if (!mounted) {
          return;
        }
        setUser(me);
        persistUser(me);
      } catch (error: unknown) {
        const status = (error as { response?: { status?: number } })?.response?.status;
        if (status === 401) {
          if (mounted) {
            clearSession();
          }
        } else if (import.meta.env.DEV) {
          console.warn('[Auth] Unable to hydrate session from /api/auth/me', error);
        }
      } finally {
        if (mounted) {
          setLoading(false);
          setAuthReady(true);
        }
      }
    };

    void bootstrapAuth();

    return () => {
      mounted = false;
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
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(AUTH_USER_KEY);

    try {
      const result = await authService.login(email, password);
      localStorage.setItem(ACCESS_TOKEN_KEY, result.accessToken);

      const me = await authService.me();
      setUser(me);
      persistUser(me);
      return me;
    } finally {
      setLoading(false);
      setAuthReady(true);
    }
  }, []);

  const logout = useCallback(() => {
    clearSession();
    setLoading(false);
    setAuthReady(true);
  }, [clearSession]);

  const value = useMemo<AuthContextType>(() => ({
    user,
    loading,
    authReady,
    login,
    logout,
  }), [authReady, loading, login, logout, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
