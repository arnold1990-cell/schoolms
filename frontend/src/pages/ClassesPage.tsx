import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { FormModal } from '../components/FormModal';
import { useAuth } from '../hooks/useAuth';
import { apiErrorMessage, unwrapList } from '../utils/apiHelpers';

interface SchoolClass { id: number; name: string; stream?: string; }

export function ClassesPage() {
  const { user, authReady } = useAuth();
  const canCreate = useMemo(() => user?.role === 'ADMIN', [user?.role]);
  const [rows, setRows] = useState<SchoolClass[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [stream, setStream] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/classes');
      setRows(unwrapList<SchoolClass>(response.data));
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load classes.'));
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
      setError('Class name is required.');
      return;
    }
    try {
      await api.post('/api/classes', { name: name.trim(), stream: stream.trim() || null });
      setFeedback('Class created successfully.');
      setOpen(false);
      setName('');
      setStream('');
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to create class.'));
    }
  };

  if (!authReady) {
    return <div className="page"><LoadingState title="Restoring session..." /></div>;
  }

  return (
    <div className="page">
      <PageHeader title="Classes" subtitle="Manage class list and stream setup." actionLabel="Add Class" onAction={() => setOpen(true)} disabled={!canCreate} disabledReason="Only admins can create classes." />
      {feedback ? <p className="success-text">{feedback}</p> : null}
      {loading ? <LoadingState title="Loading classes..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && rows.length === 0 ? <EmptyState title="No classes found" message="Create a class to assign students and exams." /> : null}
      {!loading && !error && rows.length > 0 ? (
        <table className="table"><thead><tr><th>Name</th><th>Stream</th></tr></thead><tbody>{rows.map((item) => <tr key={item.id}><td>{item.name}</td><td>{item.stream || '-'}</td></tr>)}</tbody></table>
      ) : null}
      <FormModal title="Add Class" open={open} onClose={() => setOpen(false)}>
        <form className="form-grid" onSubmit={submit}>
          <label>Class name<input value={name} onChange={(e) => setName(e.target.value)} /></label>
          <label>Stream<input value={stream} onChange={(e) => setStream(e.target.value)} /></label>
          <button type="submit">Save Class</button>
        </form>
      </FormModal>
    </div>
  );
}
