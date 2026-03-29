import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const roleMenus: Record<'ADMIN' | 'TEACHER', string[]> = {
  ADMIN: ['dashboard', 'teachers', 'students', 'classes', 'subjects', 'sessions', 'exams', 'marks', 'results', 'reports', 'notifications', 'analytics'],
  TEACHER: ['dashboard', 'subjects', 'marks', 'results', 'notifications', 'analytics'],
};

function toLabel(item: string) {
  return item.charAt(0).toUpperCase() + item.slice(1);
}

export function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="layout">
      <aside className="sidebar">
        <h3>SchoolMS</h3>
        {(user?.role ? roleMenus[user.role] : [])
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
