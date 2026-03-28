import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { FormModal } from '../components/FormModal';
import { useAuth } from '../hooks/useAuth';
import { apiErrorMessage, unwrapList } from '../utils/apiHelpers';

interface AcademicSession { id: number; name: string; active: boolean; }

export function SessionsPage() {
  const { user, authReady } = useAuth();
  const canCreate = useMemo(() => user?.role === 'ADMIN', [user?.role]);
  const [rows, setRows] = useState<AcademicSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [active, setActive] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/sessions');
      setRows(unwrapList<AcademicSession>(response.data));
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load sessions.'));
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

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!name.trim()) {
      setError('Session name is required.');
      return;
    }
    try {
      await api.post('/api/sessions', { name: name.trim(), active });
      setFeedback('Session created successfully.');
      setOpen(false);
      setName('');
      setActive(false);
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to create session.'));
    }
  };

  const setAsActive = async (id: number) => {
    try {
      await api.put(`/api/sessions/${id}/activate`);
      setFeedback('Active session updated.');
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to activate session.'));
    }
  };

  if (!authReady) {
    return <div className="page"><LoadingState title="Restoring session..." /></div>;
  }

  return (
    <div className="page">
      <PageHeader title="Sessions" subtitle="Manage academic sessions and active school period." actionLabel="Add Session" onAction={() => setOpen(true)} disabled={!canCreate} disabledReason="Only admins can create or activate sessions." />
      {feedback ? <p className="success-text">{feedback}</p> : null}
      {loading ? <LoadingState title="Loading sessions..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && rows.length === 0 ? <EmptyState title="No sessions found" message="Create at least one session to organize exams and terms." /> : null}
      {!loading && !error && rows.length > 0 ? (
        <table className="table"><thead><tr><th>Name</th><th>Status</th><th>Actions</th></tr></thead><tbody>{rows.map((item) => <tr key={item.id}><td>{item.name}</td><td><span className={item.active ? 'badge active' : 'badge'}>{item.active ? 'Active' : 'Inactive'}</span></td><td><button type="button" disabled={!canCreate || item.active} onClick={() => void setAsActive(item.id)}>{item.active ? 'Current' : 'Set Active'}</button></td></tr>)}</tbody></table>
      ) : null}
      <FormModal title="Add Session" open={open} onClose={() => setOpen(false)}>
        <form className="form-grid" onSubmit={submit}>
          <label>Session name<input value={name} onChange={(e) => setName(e.target.value)} placeholder="2026/2027" /></label>
          <label className="check-row"><input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} /> Set as active immediately</label>
          <button type="submit">Save Session</button>
        </form>
      </FormModal>
    </div>
  );
}
