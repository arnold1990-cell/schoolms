import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const menus = [
  'dashboard',
  'teachers',
  'students',
  'classes',
  'subjects',
  'sessions',
  'exams',
  'marks',
  'results',
  'reports',
  'notifications',
  'analytics',
] as const;

function toLabel(item: string) {
  return item.charAt(0).toUpperCase() + item.slice(1);
}

export function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="layout">
      <aside className="sidebar">
        <h3>SchoolMS</h3>
        {menus
          .filter((menu) => user?.role === 'ADMIN' || menu !== 'teachers')
          .map((menu) => (
            <NavLink key={menu} to={`/${menu}`} className={({ isActive }) => (isActive ? 'active-nav' : '')}>
              {toLabel(menu)}
            </NavLink>
          ))}
      </aside>
      <div className="content">
        <header className="header">
          <span>
            {user?.email} ({user?.role})
          </span>
          <button onClick={logout}>Logout</button>
        </header>
        <Outlet />
      </div>
    </div>
  );
}
