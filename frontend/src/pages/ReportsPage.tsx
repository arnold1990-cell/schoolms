import { useCallback, useEffect, useMemo, useState } from 'react';
import { AxiosError } from 'axios';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';

interface SchoolClass {
  id: number;
  name: string;
  stream?: string;
}

function getError(error: unknown) {
  const axiosError = error as AxiosError<{ message?: string }>;
  return axiosError.response?.data?.message ?? axiosError.message ?? 'Failed to load reports module.';
}

export function ReportsPage() {
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [selectedClassId, setSelectedClassId] = useState<number | ''>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadClasses = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/classes');
      const list = response.data?.data ?? [];
      setClasses(Array.isArray(list) ? list : []);
    } catch (err) {
      setError(getError(err));
      if (import.meta.env.DEV) {
        console.error('[Reports] class load failed', err);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadClasses();
  }, [loadClasses]);

  useEffect(() => {
    if (classes.length > 0 && selectedClassId === '') {
      setSelectedClassId(classes[0].id);
    }
  }, [classes, selectedClassId]);

  const canExport = useMemo(() => selectedClassId !== '', [selectedClassId]);

  const exportReport = async (format: 'pdf' | 'excel') => {
    if (!selectedClassId) return;

    try {
      const response = await api.get(`/api/reports/class/${selectedClassId}/${format}`, {
        responseType: 'blob',
      });

      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `class-${selectedClassId}-report.${format === 'pdf' ? 'pdf' : 'xlsx'}`;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      const message = getError(err);
      setError(message);
      if (import.meta.env.DEV) {
        console.error(`[Reports] ${format} export failed`, err);
      }
    }
  };

  return (
    <div className="page">
      <PageHeader title="Reports" subtitle="Export class result sheets in PDF or Excel format." />

      {loading ? <LoadingState title="Loading classes for reports..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void loadClasses()} /> : null}
      {!loading && !error && classes.length === 0 ? (
        <EmptyState title="No classes available" message="Create classes first, then generate report exports." />
      ) : null}

      {!loading && !error && classes.length > 0 ? (
        <div className="card">
          <div className="toolbar">
            <label htmlFor="report-class">Class</label>
            <select
              id="report-class"
              value={selectedClassId}
              onChange={(event) => setSelectedClassId(Number(event.target.value))}
            >
              {classes.map((schoolClass) => (
                <option key={schoolClass.id} value={schoolClass.id}>
                  {schoolClass.name} {schoolClass.stream ? `(${schoolClass.stream})` : ''}
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', gap: 8 }}>
            <button disabled={!canExport} onClick={() => void exportReport('pdf')}>
              Export PDF
            </button>
            <button disabled={!canExport} onClick={() => void exportReport('excel')}>
              Export Excel
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
