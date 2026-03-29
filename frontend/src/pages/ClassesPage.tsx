import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { FormModal } from '../components/FormModal';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { useAuth } from '../hooks/useAuth';
import {
  ClassStatus,
  ClassUpsertPayload,
  SchoolClassSummary,
  createClass,
  deleteClass,
  listClasses,
  updateClass,
} from '../services/classService';
import { apiErrorMessage } from '../utils/apiHelpers';

const statuses: ClassStatus[] = ['ACTIVE', 'INACTIVE'];

const blankForm = {
  name: '',
  code: '',
  level: '',
  academicYear: '',
  stream: '',
  capacity: '',
  status: 'ACTIVE' as ClassStatus,
};

export function ClassesPage() {
  const { user, authReady } = useAuth();
  const isAdmin = useMemo(() => user?.role === 'ADMIN', [user?.role]);

  const [rows, setRows] = useState<SchoolClassSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [keyword, setKeyword] = useState('');

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<SchoolClassSummary | null>(null);
  const [form, setForm] = useState(blankForm);
  const [deleteTarget, setDeleteTarget] = useState<SchoolClassSummary | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setRows(await listClasses());
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load classes.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!authReady || !user) return;
    void load();
  }, [authReady, load, user]);

  const visibleRows = useMemo(() => {
    const term = keyword.trim().toLowerCase();
    if (!term) return rows;
    return rows.filter((item) =>
      [item.name, item.code, item.level, item.academicYear, item.stream, item.classTeacherName]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(term))
    );
  }, [keyword, rows]);

  const openCreate = () => {
    setEditing(null);
    setForm(blankForm);
    setOpen(true);
  };

  const openEdit = (row: SchoolClassSummary) => {
    setEditing(row);
    setForm({
      name: row.name || '',
      code: row.code || '',
      level: row.level || '',
      academicYear: row.academicYear || '',
      stream: row.stream || '',
      capacity: row.capacity ? String(row.capacity) : '',
      status: row.status || 'ACTIVE',
    });
    setOpen(true);
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setFeedback('');

    if (!form.name.trim()) {
      setError('Class name is required.');
      return;
    }

    const payload: ClassUpsertPayload = {
      name: form.name.trim(),
      code: form.code.trim() || null,
      level: form.level.trim() || null,
      academicYear: form.academicYear.trim() || null,
      stream: form.stream.trim() || null,
      capacity: form.capacity ? Number(form.capacity) : null,
      status: form.status,
    };

    try {
      if (editing) {
        await updateClass(editing.id, payload);
        setFeedback('Class updated successfully.');
      } else {
        await createClass(payload);
        setFeedback('Class created successfully.');
      }
      setOpen(false);
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to save class.'));
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteClass(deleteTarget.id);
      setFeedback(`Class "${deleteTarget.name}" deleted successfully.`);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to delete class.'));
    }
  };

  if (!authReady) return <div className="page"><LoadingState title="Restoring session..." /></div>;

  return (
    <div className="page">
      <PageHeader
        title="Classes"
        subtitle="Manage classes, roster setup, and assignments."
        actionLabel={isAdmin ? 'Add Class' : undefined}
        onAction={openCreate}
      />
      <div className="card">
        <label>Search
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Search by class, code, year, stream" />
        </label>
      </div>
      {feedback ? <p className="success-text">{feedback}</p> : null}
      {loading ? <LoadingState title="Loading classes..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && visibleRows.length === 0 ? <EmptyState title="No classes found" message="Create a class to assign learners and subjects." /> : null}
      {!loading && !error && visibleRows.length > 0 ? (
        <table className="table">
          <thead>
            <tr>
              <th>Name</th><th>Code</th><th>Level</th><th>Year</th><th>Stream</th><th>Teacher</th><th>Learners</th><th>Subjects</th><th>Status</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {visibleRows.map((item) => (
              <tr key={item.id}>
                <td>{item.name}</td><td>{item.code || '-'}</td><td>{item.level || '-'}</td><td>{item.academicYear || '-'}</td><td>{item.stream || '-'}</td>
                <td>{item.classTeacherName || '-'}</td><td>{item.learnerCount}</td><td>{item.subjectCount}</td><td>{item.status}</td>
                <td>
                  <div className="action-buttons">
                    <Link to={`/classes/${item.id}`}><button type="button">View</button></Link>
                    {isAdmin ? <button type="button" onClick={() => openEdit(item)}>Edit</button> : null}
                    {isAdmin ? <button type="button" className="danger-button" onClick={() => setDeleteTarget(item)}>Delete</button> : null}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}

      <FormModal title={editing ? 'Edit Class' : 'Add Class'} open={open && isAdmin} onClose={() => setOpen(false)}>
        <form className="form-grid" onSubmit={submit}>
          <label>Class name<input value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))} /></label>
          <label>Class code<input value={form.code} onChange={(e) => setForm((p) => ({ ...p, code: e.target.value }))} /></label>
          <label>Grade/Level<input value={form.level} onChange={(e) => setForm((p) => ({ ...p, level: e.target.value }))} /></label>
          <label>Academic year<input value={form.academicYear} onChange={(e) => setForm((p) => ({ ...p, academicYear: e.target.value }))} /></label>
          <label>Stream/Section<input value={form.stream} onChange={(e) => setForm((p) => ({ ...p, stream: e.target.value }))} /></label>
          <label>Capacity<input type="number" min={1} value={form.capacity} onChange={(e) => setForm((p) => ({ ...p, capacity: e.target.value }))} /></label>
          <label>Status<select value={form.status} onChange={(e) => setForm((p) => ({ ...p, status: e.target.value as ClassStatus }))}>{statuses.map((status) => <option key={status}>{status}</option>)}</select></label>
          <button type="submit">{editing ? 'Update Class' : 'Save Class'}</button>
        </form>
      </FormModal>

      <FormModal title="Delete Class" open={Boolean(deleteTarget) && isAdmin} onClose={() => setDeleteTarget(null)}>
        <div className="form-grid">
          <p>Delete class {deleteTarget?.name}? This action is blocked when dependencies exist.</p>
          <div className="action-buttons">
            <button type="button" onClick={() => setDeleteTarget(null)}>Cancel</button>
            <button type="button" className="danger-button" onClick={() => void confirmDelete()}>Delete</button>
          </div>
        </div>
      </FormModal>
    </div>
  );
}
