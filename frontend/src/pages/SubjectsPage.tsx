import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { FormModal } from '../components/FormModal';
import { useAuth } from '../hooks/useAuth';
import { apiErrorMessage, unwrapList } from '../utils/apiHelpers';

interface Subject { id: number; code: string; name: string; assignedTeacher?: { id: number; firstName?: string; lastName?: string; staffCode?: string } }
interface Teacher { id: number; firstName: string; lastName: string; staffCode: string; }

export function SubjectsPage() {
  const { user } = useAuth();
  const canCreate = useMemo(() => user?.role === 'ADMIN', [user?.role]);
  const [rows, setRows] = useState<Subject[]>([]);
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [open, setOpen] = useState(false);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [teacherId, setTeacherId] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [subjectsResponse, teachersResponse] = await Promise.all([api.get('/api/subjects'), api.get('/api/teachers')]);
      const teacherRows = unwrapList<Teacher>(teachersResponse.data);
      setRows(unwrapList<Subject>(subjectsResponse.data));
      setTeachers(teacherRows);
      if (!teacherId && teacherRows[0]) setTeacherId(String(teacherRows[0].id));
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load subjects.'));
    } finally {
      setLoading(false);
    }
  }, [teacherId]);

  useEffect(() => { void load(); }, [load]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!code.trim() || !name.trim()) {
      setError('Subject code and name are required.');
      return;
    }
    try {
      await api.post('/api/subjects', { code: code.trim().toUpperCase(), name: name.trim(), teacherId: teacherId ? Number(teacherId) : null });
      setFeedback('Subject created successfully.');
      setOpen(false);
      setCode('');
      setName('');
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to create subject.'));
    }
  };

  return (
    <div className="page">
      <PageHeader title="Subjects" subtitle="Manage subject catalog and teacher mapping." actionLabel="Add Subject" onAction={() => setOpen(true)} disabled={!canCreate} disabledReason="Only admins can create subjects." />
      {feedback ? <p className="success-text">{feedback}</p> : null}
      {loading ? <LoadingState title="Loading subjects..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && rows.length === 0 ? <EmptyState title="No subjects found" message="Create subjects to prepare exam and marks workflows." /> : null}
      {!loading && !error && rows.length > 0 ? (
        <table className="table"><thead><tr><th>Code</th><th>Name</th><th>Assigned Teacher</th></tr></thead><tbody>{rows.map((item) => <tr key={item.id}><td>{item.code}</td><td>{item.name}</td><td>{item.assignedTeacher ? `${item.assignedTeacher.firstName ?? ''} ${item.assignedTeacher.lastName ?? ''}`.trim() || item.assignedTeacher.staffCode : '-'}</td></tr>)}</tbody></table>
      ) : null}
      <FormModal title="Add Subject" open={open} onClose={() => setOpen(false)}>
        <form className="form-grid" onSubmit={submit}>
          <label>Code<input value={code} onChange={(e) => setCode(e.target.value)} /></label>
          <label>Name<input value={name} onChange={(e) => setName(e.target.value)} /></label>
          <label>Teacher<select value={teacherId} onChange={(e) => setTeacherId(e.target.value)}><option value="">Unassigned</option>{teachers.map((t) => <option key={t.id} value={t.id}>{t.firstName} {t.lastName} ({t.staffCode})</option>)}</select></label>
          <button type="submit">Save Subject</button>
        </form>
      </FormModal>
    </div>
  );
}
