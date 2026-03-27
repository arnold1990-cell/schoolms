import { useEffect, useState } from 'react';
import api from '../services/api';

export function CrudPageFactory({ title, endpoint }: { title: string; endpoint: string }) {
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get(endpoint).then(r => setRows(r.data.data ?? r.data)).finally(() => setLoading(false));
  }, [endpoint]);

  return <div className="page">
    <h2>{title}</h2>
    {loading ? 'Loading...' : (
      <table className="table"><thead><tr>{rows[0] && Object.keys(rows[0]).slice(0,6).map(k => <th key={k}>{k}</th>)}</tr></thead>
      <tbody>{rows.map((r,i) => <tr key={i}>{Object.values(r).slice(0,6).map((v,j) => <td key={j}>{String(v)}</td>)}</tr>)}</tbody></table>
    )}
  </div>;
}
