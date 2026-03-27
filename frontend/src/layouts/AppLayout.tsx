import { Link, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function AppLayout() {
  const { user, logout } = useAuth();
  const menus = ['dashboard', 'teachers', 'students', 'classes', 'subjects', 'sessions', 'exams', 'marks', 'results', 'reports', 'notifications', 'analytics'];
  return (
    <div className="layout">
      <aside className="sidebar">
        <h3>SchoolMS</h3>
        {menus.filter(m => user?.role === 'ADMIN' || !['teachers'].includes(m)).map(m => (
          <Link key={m} to={`/${m}`}>{m.toUpperCase()}</Link>
        ))}
      </aside>
      <div className="content">
        <header className="header">
          <span>{user?.email} ({user?.role})</span>
          <button onClick={logout}>Logout</button>
        </header>
        <Outlet />
      </div>
    </div>
  );
}
