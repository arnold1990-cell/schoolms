import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { FormModal } from '../components/FormModal';
import { apiErrorMessage, unwrapItem, unwrapList } from '../utils/apiHelpers';
import { useAuth } from '../hooks/useAuth';

interface Student {
  id: number;
  fullName: string;
  admissionNumber: string;
  className: string;
  guardianContact?: string;
  status?: string;
}

interface SchoolClass {
  id: number;
  name: string;
  stream?: string;
}

const blankForm = {
  firstName: '',
  lastName: '',
  admissionNumber: '',
  gender: 'MALE',
  dateOfBirth: '',
  guardianName: '',
  guardianContact: '',
  status: 'ACTIVE',
  classId: '',
};

export function StudentsPage() {
  const { user, authReady } = useAuth();
  const canManage = useMemo(() => user?.role === 'ADMIN', [user?.role]);
  const [rows, setRows] = useState<Student[]>([]);
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Student | null>(null);
  const [form, setForm] = useState(blankForm);

  const loadClasses = useCallback(async () => {
    const response = await api.get('/api/classes');
    const classRows = unwrapList<SchoolClass>(response.data);
    setClasses(classRows);
    if (classRows.length > 0 && !form.classId) {
      setForm((prev) => ({ ...prev, classId: String(classRows[0].id) }));
    }
  }, [form.classId]);

  const loadStudents = useCallback(async (search: string) => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/students', { params: search ? { q: search } : undefined });
      setRows(unwrapList<Student>(response.data));
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load students.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!authReady || !user) {
      return;
    }
    void Promise.all([loadStudents(''), loadClasses()]);
  }, [authReady, loadClasses, loadStudents, user]);

  const openCreate = () => {
    setEditing(null);
    setForm((prev) => ({ ...blankForm, classId: prev.classId || (classes[0] ? String(classes[0].id) : '') }));
    setModalOpen(true);
  };

  const openEdit = (student: Student) => {
    setEditing(student);
    const [firstName, ...rest] = student.fullName.split(' ');
    setForm({
      ...blankForm,
      firstName,
      lastName: rest.join(' '),
      admissionNumber: student.admissionNumber,
      guardianContact: student.guardianContact || '',
      status: student.status || 'ACTIVE',
      classId: String(classes.find((c) => c.name === student.className)?.id ?? classes[0]?.id ?? ''),
    });
    setModalOpen(true);
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!form.firstName || !form.lastName || !form.admissionNumber || !form.classId) {
      setError('First name, last name, admission number and class are required.');
      return;
    }

    try {
      setError('');
      const payload = {
        firstName: form.firstName,
        lastName: form.lastName,
        admissionNumber: form.admissionNumber,
        gender: form.gender,
        dateOfBirth: form.dateOfBirth || null,
        guardianName: form.guardianName || null,
        guardianContact: form.guardianContact || null,
        status: form.status,
        classId: Number(form.classId),
      };

      if (editing) {
        await api.put(`/api/students/${editing.id}`, payload);
        setFeedback('Student updated successfully.');
      } else {
        await api.post('/api/students', payload);
        setFeedback('Student created successfully.');
      }
      setModalOpen(false);
      await loadStudents(query);
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to save student.'));
    }
  };

  const search = async (event: FormEvent) => {
    event.preventDefault();
    await loadStudents(query);
  };

  if (!authReady) {
    return <div className="page"><LoadingState title="Restoring session..." /></div>;
  }

  return (
    <div className="page">
      <PageHeader
        title="Students"
        subtitle="Manage students, class assignment, and enrollment status."
        actionLabel="Add Student"
        onAction={openCreate}
        disabled={!canManage}
        disabledReason="Only admins can add or edit students."
      />
      {feedback ? <p className="success-text">{feedback}</p> : null}

      <form className="toolbar-row" onSubmit={search}>
        <input placeholder="Search by name or admission #" value={query} onChange={(e) => setQuery(e.target.value)} />
        <button type="submit">Search</button>
      </form>

      {loading ? <LoadingState title="Loading students..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void loadStudents(query)} /> : null}
      {!loading && !error && rows.length === 0 ? <EmptyState title="No students found" message="Add students to start enrollment and assessment workflows." /> : null}

      {!loading && !error && rows.length > 0 ? (
        <table className="table">
          <thead><tr><th>Name</th><th>Admission #</th><th>Class</th><th>Guardian</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>
            {rows.map((student) => (
              <tr key={student.id}>
                <td>{student.fullName}</td><td>{student.admissionNumber}</td><td>{student.className || '-'}</td><td>{student.guardianContact || '-'}</td><td>{student.status || '-'}</td>
                <td>
                  <button type="button" disabled={!canManage} onClick={() => openEdit(student)}>Edit</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}

      <FormModal title={editing ? 'Edit Student' : 'Add Student'} open={modalOpen} onClose={() => setModalOpen(false)}>
        <form className="form-grid" onSubmit={submit}>
          <label>First name<input value={form.firstName} onChange={(e) => setForm((s) => ({ ...s, firstName: e.target.value }))} /></label>
          <label>Last name<input value={form.lastName} onChange={(e) => setForm((s) => ({ ...s, lastName: e.target.value }))} /></label>
          <label>Admission #<input value={form.admissionNumber} onChange={(e) => setForm((s) => ({ ...s, admissionNumber: e.target.value }))} /></label>
          <label>Class<select value={form.classId} onChange={(e) => setForm((s) => ({ ...s, classId: e.target.value }))}>{classes.map((c) => <option key={c.id} value={c.id}>{c.name} {c.stream || ''}</option>)}</select></label>
          <label>Status<select value={form.status} onChange={(e) => setForm((s) => ({ ...s, status: e.target.value }))}><option>ACTIVE</option><option>INACTIVE</option></select></label>
          <label>Guardian contact<input value={form.guardianContact} onChange={(e) => setForm((s) => ({ ...s, guardianContact: e.target.value }))} /></label>
          <button type="submit">{editing ? 'Update Student' : 'Save Student'}</button>
        </form>
      </FormModal>
    </div>
  );
}
