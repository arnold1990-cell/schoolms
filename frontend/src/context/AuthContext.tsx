import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { MeResponse } from '../types';
import { authService } from '../services/authService';

interface AuthContextType {
  user: MeResponse | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) return setLoading(false);
    authService.me()
      .then(setUser)
      .catch(() => {
        localStorage.removeItem('accessToken');
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    login: async (email: string, password: string) => {
      localStorage.removeItem('accessToken');
      const result = await authService.login(email, password);
      localStorage.setItem('accessToken', result.accessToken);
      setUser(result.user);
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
