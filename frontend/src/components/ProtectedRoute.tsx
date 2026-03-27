import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function ProtectedRoute({ children }: { children: JSX.Element }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="page">Loading...</div>;
  if (!user) return <Navigate to="/login" replace />;
  return children;
}
