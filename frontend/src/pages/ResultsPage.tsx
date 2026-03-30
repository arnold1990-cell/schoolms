import { useCallback, useEffect, useState } from 'react';
import { AxiosError } from 'axios';
import api from '../services/api';
import { DataTable } from '../components/DataTable';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { useAuth } from '../hooks/useAuth';

interface SchoolClass {
  id: number;
  name: string;
}

function message(error: unknown) {
  const axiosError = error as AxiosError<{ message?: string }>;
  return axiosError.response?.data?.message ?? axiosError.message ?? 'Failed to load results.';
}

export function ResultsPage() {
  const { user, authReady } = useAuth();
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [selectedClassId, setSelectedClassId] = useState<number | ''>('');
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadClasses = useCallback(async () => {
    const response = await api.get('/api/classes');
    const payload = response.data?.data ?? [];
    const list = Array.isArray(payload) ? payload : [];
    setClasses(list);
    if (list.length > 0) {
      setSelectedClassId((prev) => prev || list[0].id);
    }
  }, []);

  const loadResults = useCallback(async (classId: number) => {
    const response = await api.get(`/api/results/class/${classId}`);
    const payload = response.data?.data ?? [];
    setRows(Array.isArray(payload) ? payload : []);
  }, []);

  const bootstrap = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      await loadClasses();
    } catch (err) {
      setError(message(err));
      if (import.meta.env.DEV) {
        console.error('[Results] class load failed', err);
      }
    } finally {
      setLoading(false);
    }
  }, [loadClasses]);

  useEffect(() => {
    if (!authReady || !user) {
      return;
    }
    void bootstrap();
  }, [authReady, bootstrap, user]);

  useEffect(() => {
    if (!authReady || !user || !selectedClassId) return;

    setLoading(true);
    setError('');
    loadResults(selectedClassId)
      .catch((err) => {
        setError(message(err));
        if (import.meta.env.DEV) {
          console.error('[Results] results load failed', err);
        }
      })
      .finally(() => setLoading(false));
  }, [authReady, loadResults, selectedClassId, user]);

  if (!authReady) {
    return <div className="page"><LoadingState title="Restoring session..." /></div>;
  }

  return (
    <div className="page">
      <PageHeader title="Results" subtitle="Review class-level computed exam results." />
      {loading ? <LoadingState title="Loading results..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void bootstrap()} /> : null}
      {!loading && !error && classes.length === 0 ? (
        <EmptyState title="No classes available" message="Add classes before generating results." />
      ) : null}

      {!loading && !error && classes.length > 0 ? (
        <div className="card" style={{ marginBottom: 12 }}>
          <label htmlFor="results-class">Class</label>{' '}
          <select
            id="results-class"
            value={selectedClassId}
            onChange={(event) => setSelectedClassId(Number(event.target.value))}
          >
            {classes.map((schoolClass) => (
              <option key={schoolClass.id} value={schoolClass.id}>
                {schoolClass.name}
              </option>
            ))}
          </select>
        </div>
      ) : null}

      {!loading && !error && classes.length > 0 && rows.length === 0 ? (
        <EmptyState title="No result records" message="No published class results are available yet." />
      ) : null}
      {!loading && !error && rows.length > 0 ? <DataTable rows={rows} /> : null}
    </div>
  );
}
