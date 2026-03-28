import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { MeResponse } from '../types';
import { authService } from '../services/authService';
import { UNAUTHORIZED_EVENT } from '../services/api';

interface AuthContextType {
  user: MeResponse | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<MeResponse>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      setLoading(false);
      return;
    }

    authService.me()
      .then(setUser)
      .catch((error: unknown) => {
        const status = (error as { response?: { status?: number } })?.response?.status;
        if (status === 401) {
          localStorage.removeItem('accessToken');
          setUser(null);
          return;
        }
        if (import.meta.env.DEV) {
          console.warn('[Auth] Unable to hydrate session from /api/auth/me', error);
        }
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const onUnauthorized = () => {
      localStorage.removeItem('accessToken');
      setUser(null);
      setLoading(false);
    };

    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    login: async (email: string, password: string) => {
      localStorage.removeItem('accessToken');
      const result = await authService.login(email, password);
      localStorage.setItem('accessToken', result.accessToken);
      const me = await authService.me();
      setUser(me);
      return me;
    },
    logout: () => {
      localStorage.removeItem('accessToken');
      setUser(null);
    }
  }), [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
