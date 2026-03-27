import { Link } from 'react-router-dom';

export function AccessDeniedPage() {
  return (
    <div className="page">
      <h2>Access denied</h2>
      <p>You are authenticated but not authorized to view this page.</p>
      <Link to="/dashboard">Go to dashboard</Link>
    </div>
  );
}
