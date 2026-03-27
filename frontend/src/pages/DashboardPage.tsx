import { useEffect, useState } from 'react';
import api from '../services/api';
import { useAuth } from '../hooks/useAuth';

export function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState<Record<string, unknown>>({});

  useEffect(() => {
    api.get(user?.role === 'ADMIN' ? '/api/admin/dashboard' : '/api/teacher/dashboard').then(r => setData(r.data.data));
  }, [user?.role]);

  return <div className="page">
    <h2>{user?.role} Dashboard</h2>
    <div className="grid">{Object.entries(data).map(([k, v]) => <div className="card" key={k}><b>{k}</b><div>{typeof v === 'object' ? JSON.stringify(v).slice(0,70)+'...' : String(v)}</div></div>)}</div>
  </div>;
}
