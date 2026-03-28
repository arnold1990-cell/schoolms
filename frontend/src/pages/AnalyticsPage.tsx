import { useCallback, useEffect, useState } from 'react';
import { AxiosError } from 'axios';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { useAuth } from '../hooks/useAuth';

type AnalyticsData = Record<string, number | string>;

function messageFrom(error: unknown) {
  const axiosError = error as AxiosError<{ message?: string }>;
  return axiosError.response?.data?.message ?? axiosError.message ?? 'Unable to load analytics.';
}

export function AnalyticsPage() {
  const { user, authReady } = useAuth();
  const [data, setData] = useState<AnalyticsData>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/analytics/overview');
      const payload = response.data?.data ?? response.data;
      setData(payload ?? {});
    } catch (err) {
      setError(messageFrom(err));
      if (import.meta.env.DEV) {
        console.error('[Analytics] load failed', err);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!authReady || !user) {
      return;
    }
    void load();
  }, [authReady, load, user]);

  const entries = Object.entries(data);

  if (!authReady) {
    return <div className="page"><LoadingState title="Restoring session..." /></div>;
  }

  return (
    <div className="page">
      <PageHeader title="Analytics" subtitle="Performance and examination insights." />
      {loading ? <LoadingState title="Loading analytics overview..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && entries.length === 0 ? <EmptyState title="Analytics not available" message="No analytics metrics were returned." /> : null}
      {!loading && !error && entries.length > 0 ? (
        <div className="grid">
          {entries.map(([key, value]) => (
            <div className="card" key={key}>
              <p>{key}</p>
              <h3>{String(value)}</h3>
            </div>
          ))}
        </div>
      ) : null}
    </div>
  );
}
