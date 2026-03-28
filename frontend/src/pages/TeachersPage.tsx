import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { FormModal } from '../components/FormModal';
import { apiErrorMessage, unwrapItem, unwrapList } from '../utils/apiHelpers';
import { useAuth } from '../hooks/useAuth';

interface Teacher {
  id: number;
  firstName: string;
  lastName: string;
  staffCode: string;
  phone?: string;
  email: string;
  enabled: boolean;
}

const initialForm = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  employeeNumber: '',
  subjectSpecialization: '',
  password: 'Teacher123!',
  enabled: true,
};

export function TeachersPage() {
  const { user, authReady } = useAuth();
  const [rows, setRows] = useState<Teacher[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState(initialForm);

  const canCreate = useMemo(() => user?.role === 'ADMIN', [user?.role]);

  const loadTeachers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/teachers');
      setRows(unwrapList<Teacher>(response.data));
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load teachers.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!authReady || !user) {
      return;
    }
    void loadTeachers();
  }, [authReady, loadTeachers, user]);

  const validate = () => {
    if (!form.firstName.trim() || !form.lastName.trim()) return 'First and last name are required.';
    if (!form.email.includes('@')) return 'A valid email is required.';
    if (!form.employeeNumber.trim()) return 'Employee number is required.';
    if (form.password.length < 8) return 'Password must be at least 8 characters.';
    return '';
  };

  const onCreate = async (event: FormEvent) => {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setSubmitting(true);
    setError('');
    setFeedback('');
    try {
      const response = await api.post('/api/teachers', {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || null,
        staffCode: form.employeeNumber.trim(),
        password: form.password,
      });

      const created = unwrapItem<Teacher>(response.data);
      if (created && form.enabled === false) {
        await api.put(`/api/teachers/${created.id}/status?enabled=false`);
      }

      setFeedback('Teacher created successfully.');
      setModalOpen(false);
      setForm(initialForm);
      await loadTeachers();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to create teacher.'));
    } finally {
      setSubmitting(false);
    }
  };

  const toggleStatus = async (teacher: Teacher) => {
    try {
      setError('');
      await api.put(`/api/teachers/${teacher.id}/status?enabled=${!teacher.enabled}`);
      await loadTeachers();
      setFeedback(`Teacher ${teacher.enabled ? 'disabled' : 'enabled'} successfully.`);
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to update teacher status.'));
    }
  };

  if (!authReady) {
    return <div className="page"><LoadingState title="Restoring session..." /></div>;
  }

  return (
    <div className="page">
      <PageHeader
        title="Teachers"
        subtitle="Manage teacher records, accounts, and activation status."
        actionLabel="Add Teacher"
        onAction={() => setModalOpen(true)}
        disabled={!canCreate}
        disabledReason="Only admins can create teacher accounts."
      />

      {feedback ? <p className="success-text">{feedback}</p> : null}
      {loading ? <LoadingState title="Loading teachers..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void loadTeachers()} /> : null}
      {!loading && !error && rows.length === 0 ? (
        <EmptyState title="No teachers found" message="Create your first teacher to begin class and subject assignment." />
      ) : null}

      {!loading && !error && rows.length > 0 ? (
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Employee #</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((teacher) => (
              <tr key={teacher.id}>
                <td>{teacher.firstName} {teacher.lastName}</td>
                <td>{teacher.email}</td>
                <td>{teacher.phone || '-'}</td>
                <td>{teacher.staffCode}</td>
                <td><span className={teacher.enabled ? 'badge active' : 'badge'}>{teacher.enabled ? 'Enabled' : 'Disabled'}</span></td>
                <td><button type="button" onClick={() => void toggleStatus(teacher)}>{teacher.enabled ? 'Disable' : 'Enable'}</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}

      <FormModal title="Add Teacher" open={modalOpen} onClose={() => setModalOpen(false)}>
        <form className="form-grid" onSubmit={onCreate}>
          <label>First name<input value={form.firstName} onChange={(e) => setForm((s) => ({ ...s, firstName: e.target.value }))} /></label>
          <label>Last name<input value={form.lastName} onChange={(e) => setForm((s) => ({ ...s, lastName: e.target.value }))} /></label>
          <label>Email<input type="email" value={form.email} onChange={(e) => setForm((s) => ({ ...s, email: e.target.value }))} /></label>
          <label>Phone<input value={form.phone} onChange={(e) => setForm((s) => ({ ...s, phone: e.target.value }))} /></label>
          <label>Employee number<input value={form.employeeNumber} onChange={(e) => setForm((s) => ({ ...s, employeeNumber: e.target.value }))} /></label>
          <label>Subject specialization<input value={form.subjectSpecialization} onChange={(e) => setForm((s) => ({ ...s, subjectSpecialization: e.target.value }))} /></label>
          <label>Password<input type="password" value={form.password} onChange={(e) => setForm((s) => ({ ...s, password: e.target.value }))} /></label>
          <label className="check-row"><input type="checkbox" checked={form.enabled} onChange={(e) => setForm((s) => ({ ...s, enabled: e.target.checked }))} /> Enabled account</label>
          <p className="hint-text">Subject specialization is captured for admin workflow context and can be mapped via Subjects.</p>
          <button type="submit" disabled={submitting}>{submitting ? 'Saving...' : 'Save Teacher'}</button>
        </form>
      </FormModal>
    </div>
  );
}
