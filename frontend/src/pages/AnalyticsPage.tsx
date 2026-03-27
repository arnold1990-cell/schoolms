import { useEffect, useState } from 'react';
import api from '../services/api';

export function AnalyticsPage() {
  const [data, setData] = useState<Record<string, unknown>>({});
  useEffect(() => { api.get('/api/analytics/overview').then(r => setData(r.data)); }, []);
  return <div className="page"><h2>Analytics</h2><div className="grid">{Object.entries(data).map(([k,v]) => <div className="card" key={k}>{k}: {String(v)}</div>)}</div></div>;
}
