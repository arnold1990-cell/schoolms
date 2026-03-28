import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { FormModal } from '../components/FormModal';
import { useAuth } from '../hooks/useAuth';
import { apiErrorMessage, unwrapList } from '../utils/apiHelpers';
import { assignSubjectTeacher, createSubject, listSubjects, SubjectDto } from '../services/subjectService';

type Subject = SubjectDto;
interface Teacher { id: number; firstName: string; lastName: string; staffCode: string; }

export function SubjectsPage() {
  const { user, authReady } = useAuth();
  const isAdmin = useMemo(() => user?.role === 'ADMIN', [user?.role]);
  const [rows, setRows] = useState<Subject[]>([]);
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [loading, setLoading] = useState(true);
  const [teachersLoading, setTeachersLoading] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [open, setOpen] = useState(false);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [teacherId, setTeacherId] = useState('');
  const [assignSubjectId, setAssignSubjectId] = useState<number | null>(null);
  const [assignTeacherId, setAssignTeacherId] = useState('');

  const loadSubjects = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setRows(await listSubjects());
    } catch (err) {
      const message = apiErrorMessage(err, 'Failed to load subjects.');
      const lowered = message.toLowerCase();
      if (lowered.includes('403')) {
        setError('You do not have permission to access this data. Please sign in with an ADMIN or TEACHER account.');
      } else if (lowered.includes('401')) {
        setError('Your session could not be used for loading subjects. Please sign out and sign in again if this persists.');
      } else {
        setError(message);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  const loadTeachers = useCallback(async () => {
    if (!isAdmin || teachers.length > 0 || teachersLoading) return;
    setTeachersLoading(true);
    try {
      const teachersResponse = await api.get('/api/teachers');
      const teacherRows = unwrapList<Teacher>(teachersResponse.data);
      setTeachers(teacherRows);
    } catch {
      setTeachers([]);
      setTeacherId('');
    } finally {
      setTeachersLoading(false);
    }
  }, [isAdmin, teachers.length, teachersLoading]);

  useEffect(() => {
    if (!authReady || !user) {
      return;
    }
    void loadSubjects();
  }, [authReady, loadSubjects, user]);

  useEffect(() => {
    if (open) void loadTeachers();
  }, [open, loadTeachers]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!isAdmin) {
      setError('Only admins can create subjects and assign teachers.');
      return;
    }
    if (!code.trim() || !name.trim()) {
      setError('Subject code and name are required.');
      return;
    }
    try {
      const createdSubject = await createSubject({ code: code.trim().toUpperCase(), name: name.trim() });
      if (createdSubject?.id && teacherId) {
        await assignSubjectTeacher(createdSubject.id, Number(teacherId));
      }
      setFeedback('Subject created successfully.');
      setOpen(false);
      setCode('');
      setName('');
      await loadSubjects();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to create subject.'));
    }
  };

  const assignTeacher = async (event: FormEvent) => {
    event.preventDefault();
    if (!isAdmin || !assignSubjectId) {
      setError('Only admins can assign teachers to subjects.');
      return;
    }
    try {
      await assignSubjectTeacher(assignSubjectId, assignTeacherId ? Number(assignTeacherId) : null);
      setFeedback('Teacher assignment updated.');
      setAssignSubjectId(null);
      setAssignTeacherId('');
      await loadSubjects();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to assign teacher.'));
    }
  };

  if (!authReady) {
    return <div className="page"><LoadingState title="Restoring session..." /></div>;
  }

  return (
    <div className="page">
      <PageHeader
        title="Subjects"
        subtitle="Manage subject catalog and teacher mapping."
        actionLabel={isAdmin ? 'Add Subject' : undefined}
        onAction={() => setOpen(true)}
      />
      {feedback ? <p className="success-text">{feedback}</p> : null}
      {loading ? <LoadingState title="Loading subjects..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void loadSubjects()} /> : null}
      {!loading && !error && rows.length === 0 ? <EmptyState title="No subjects found" message="Create subjects to prepare exam and marks workflows." /> : null}
      {!loading && !error && rows.length > 0 ? (
        <table className="table"><thead><tr><th>Code</th><th>Name</th><th>Assigned Teacher</th>{isAdmin ? <th>Actions</th> : null}</tr></thead><tbody>{rows.map((item) => <tr key={item.id}><td>{item.code}</td><td>{item.name}</td><td>{item.assignedTeacher ? `${item.assignedTeacher.firstName ?? ''} ${item.assignedTeacher.lastName ?? ''}`.trim() || item.assignedTeacher.staffCode : '-'}</td>{isAdmin ? <td><button type="button" onClick={() => { setAssignSubjectId(item.id); setAssignTeacherId(item.assignedTeacher?.id ? String(item.assignedTeacher.id) : ''); void loadTeachers(); }}>Assign Teacher</button></td> : null}</tr>)}</tbody></table>
      ) : null}
      <FormModal title="Add Subject" open={open && isAdmin} onClose={() => setOpen(false)}>
        <form className="form-grid" onSubmit={submit}>
          <label>Code<input value={code} onChange={(e) => setCode(e.target.value)} /></label>
          <label>Name<input value={name} onChange={(e) => setName(e.target.value)} /></label>
          <label>Teacher<select value={teacherId} onChange={(e) => setTeacherId(e.target.value)}><option value="">Unassigned</option>{teachers.map((t) => <option key={t.id} value={t.id}>{t.firstName} {t.lastName} ({t.staffCode})</option>)}</select></label>
          <button type="submit">Save Subject</button>
        </form>
      </FormModal>
      <FormModal title="Assign Teacher" open={assignSubjectId !== null && isAdmin} onClose={() => setAssignSubjectId(null)}>
        <form className="form-grid" onSubmit={assignTeacher}>
          <label>Teacher<select value={assignTeacherId} onChange={(e) => setAssignTeacherId(e.target.value)}><option value="">Unassigned</option>{teachers.map((t) => <option key={t.id} value={t.id}>{t.firstName} {t.lastName} ({t.staffCode})</option>)}</select></label>
          <button type="submit">Save Assignment</button>
        </form>
      </FormModal>
    </div>
  );
}
