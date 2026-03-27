import { useCallback, useEffect, useMemo, useState } from 'react';
import { AxiosError } from 'axios';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { DataTable } from '../components/DataTable';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { useAuth } from '../hooks/useAuth';

function asRows(payload: unknown): Record<string, unknown>[] {
  if (Array.isArray(payload)) return payload as Record<string, unknown>[];

  if (payload && typeof payload === 'object' && 'data' in payload) {
    return asRows((payload as { data?: unknown }).data);
  }

  if (payload && typeof payload === 'object') {
    return [payload as Record<string, unknown>];
  }

  return [];
}

function extractMessage(error: unknown): string {
  const axiosError = error as AxiosError<{ message?: string }>;
  return axiosError.response?.data?.message ?? axiosError.message ?? 'Unable to load records.';
}

interface CrudPageFactoryProps {
  title: string;
  endpoint: string;
  subtitle?: string;
  addLabel?: string;
  adminOnlyAdd?: boolean;
}

export function CrudPageFactory({ title, endpoint, subtitle, addLabel = 'Add', adminOnlyAdd = true }: CrudPageFactoryProps) {
  const { user } = useAuth();
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState('');

  const canCreate = useMemo(() => (adminOnlyAdd ? user?.role === 'ADMIN' : true), [adminOnlyAdd, user?.role]);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      if (import.meta.env.DEV) {
        console.debug(`[${title}] loading from`, endpoint);
      }
      const response = await api.get(endpoint);
      const nextRows = asRows(response.data);
      setRows(nextRows);
      setLastUpdated(new Date().toLocaleString());
    } catch (err) {
      const message = extractMessage(err);
      setError(message);
      if (import.meta.env.DEV) {
        console.error(`[${title}] API load failed`, err);
      }
    } finally {
      setLoading(false);
    }
  }, [endpoint, title]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="page">
      <PageHeader
        title={title}
        subtitle={subtitle ?? `Manage ${title.toLowerCase()} records. ${lastUpdated ? `Last synced: ${lastUpdated}.` : ''}`}
        actionLabel={`${addLabel} ${title.slice(0, -1) || title}`}
        onAction={() => {
          if (import.meta.env.DEV) {
            console.info(`[${title}] create action clicked. Form integration pending.`);
          }
        }}
        disabled={!canCreate}
      />

      {loading ? <LoadingState title={`Loading ${title.toLowerCase()}...`} /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && rows.length === 0 ? (
        <EmptyState title={`No ${title.toLowerCase()} found`} message={`Create your first ${title.slice(0, -1).toLowerCase() || title.toLowerCase()} to get started.`} />
      ) : null}
      {!loading && !error && rows.length > 0 ? <DataTable rows={rows} /> : null}
    </div>
  );
}
