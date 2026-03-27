import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { FormModal } from '../components/FormModal';
import { useAuth } from '../hooks/useAuth';
import { apiErrorMessage, unwrapList } from '../utils/apiHelpers';

interface Exam {
  id: number;
  title: string;
  examCode: string;
  examDate: string;
  totalMarks: number;
  status: string;
  schoolClass?: { id: number; name: string };
  subject?: { id: number; name: string };
}
interface OptionItem { id: number; name?: string; title?: string; }

export function ExamsPage() {
  const { user } = useAuth();
  const [rows, setRows] = useState<Exam[]>([]);
  const [classes, setClasses] = useState<OptionItem[]>([]);
  const [subjects, setSubjects] = useState<OptionItem[]>([]);
  const [terms, setTerms] = useState<OptionItem[]>([]);
  const [sessions, setSessions] = useState<OptionItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [open, setOpen] = useState(false);
  const [filters, setFilters] = useState({ classId: '', subjectId: '', status: '' });
  const [form, setForm] = useState({ title: '', classId: '', subjectId: '', termId: '', sessionId: '', examDate: '', durationMinutes: '90', totalMarks: '100', status: 'DRAFT' });

  const canCreate = useMemo(() => user?.role === 'ADMIN' || user?.role === 'TEACHER', [user?.role]);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [examRes, classRes, subjectRes, termRes, sessionRes] = await Promise.all([
        api.get('/api/exams', { params: filters.classId || filters.subjectId || filters.status ? filters : undefined }),
        api.get('/api/classes'),
        api.get('/api/subjects'),
        api.get('/api/terms'),
        api.get('/api/sessions'),
      ]);
      const classRows = unwrapList<OptionItem>(classRes.data);
      const subjectRows = unwrapList<OptionItem>(subjectRes.data);
      const termRows = unwrapList<OptionItem>(termRes.data);
      const sessionRows = unwrapList<OptionItem>(sessionRes.data);
      setRows(unwrapList<Exam>(examRes.data));
      setClasses(classRows);
      setSubjects(subjectRows);
      setTerms(termRows);
      setSessions(sessionRows);
      setForm((prev) => ({
        ...prev,
        classId: prev.classId || String(classRows[0]?.id || ''),
        subjectId: prev.subjectId || String(subjectRows[0]?.id || ''),
        termId: prev.termId || String(termRows[0]?.id || ''),
        sessionId: prev.sessionId || String(sessionRows[0]?.id || ''),
      }));
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load exams module.'));
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => { void load(); }, [load]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!form.title || !form.classId || !form.subjectId || !form.termId || !form.sessionId || !form.examDate) {
      setError('Please complete all required exam fields.');
      return;
    }
    try {
      await api.post('/api/exams', {
        title: form.title,
        classId: Number(form.classId),
        subjectId: Number(form.subjectId),
        termId: Number(form.termId),
        sessionId: Number(form.sessionId),
        examDate: form.examDate,
        durationMinutes: Number(form.durationMinutes),
        totalMarks: Number(form.totalMarks),
        status: form.status,
      });
      setFeedback('Exam created successfully.');
      setOpen(false);
      setForm((prev) => ({ ...prev, title: '', examDate: '' }));
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to create exam.'));
    }
  };

  return (
    <div className="page">
      <PageHeader title="Exams" subtitle="Create exams and filter by class, subject, and status." actionLabel="Create Exam" onAction={() => setOpen(true)} disabled={!canCreate} disabledReason="You do not have permission to create exams." />
      {feedback ? <p className="success-text">{feedback}</p> : null}

      <div className="toolbar-row">
        <select value={filters.classId} onChange={(e) => setFilters((f) => ({ ...f, classId: e.target.value }))}><option value="">All classes</option>{classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select>
        <select value={filters.subjectId} onChange={(e) => setFilters((f) => ({ ...f, subjectId: e.target.value }))}><option value="">All subjects</option>{subjects.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}</select>
        <select value={filters.status} onChange={(e) => setFilters((f) => ({ ...f, status: e.target.value }))}><option value="">All status</option><option value="DRAFT">DRAFT</option><option value="PUBLISHED">PUBLISHED</option><option value="COMPLETED">COMPLETED</option></select>
        <button type="button" onClick={() => void load()}>Apply</button>
      </div>

      {loading ? <LoadingState title="Loading exams..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && rows.length === 0 ? <EmptyState title="No exams found" message="Create an exam after setting classes, subjects, terms, and sessions." /> : null}
      {!loading && !error && rows.length > 0 ? (
        <table className="table"><thead><tr><th>Title</th><th>Code</th><th>Class</th><th>Subject</th><th>Date</th><th>Status</th><th>Total</th></tr></thead><tbody>{rows.map((item) => <tr key={item.id}><td>{item.title}</td><td>{item.examCode}</td><td>{item.schoolClass?.name || '-'}</td><td>{item.subject?.name || '-'}</td><td>{item.examDate}</td><td>{item.status}</td><td>{item.totalMarks}</td></tr>)}</tbody></table>
      ) : null}

      <FormModal title="Create Exam" open={open} onClose={() => setOpen(false)}>
        <form className="form-grid" onSubmit={submit}>
          <label>Title<input value={form.title} onChange={(e) => setForm((s) => ({ ...s, title: e.target.value }))} /></label>
          <label>Class<select value={form.classId} onChange={(e) => setForm((s) => ({ ...s, classId: e.target.value }))}>{classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
          <label>Subject<select value={form.subjectId} onChange={(e) => setForm((s) => ({ ...s, subjectId: e.target.value }))}>{subjects.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label>
          <label>Term<select value={form.termId} onChange={(e) => setForm((s) => ({ ...s, termId: e.target.value }))}>{terms.map((t) => <option key={t.id} value={t.id}>{t.name || t.title}</option>)}</select></label>
          <label>Session<select value={form.sessionId} onChange={(e) => setForm((s) => ({ ...s, sessionId: e.target.value }))}>{sessions.map((sn) => <option key={sn.id} value={sn.id}>{sn.name || sn.title}</option>)}</select></label>
          <label>Date<input type="date" value={form.examDate} onChange={(e) => setForm((s) => ({ ...s, examDate: e.target.value }))} /></label>
          <label>Duration (min)<input type="number" value={form.durationMinutes} onChange={(e) => setForm((s) => ({ ...s, durationMinutes: e.target.value }))} /></label>
          <label>Total marks<input type="number" value={form.totalMarks} onChange={(e) => setForm((s) => ({ ...s, totalMarks: e.target.value }))} /></label>
          <label>Status<select value={form.status} onChange={(e) => setForm((s) => ({ ...s, status: e.target.value }))}><option>DRAFT</option><option>PUBLISHED</option><option>COMPLETED</option></select></label>
          <button type="submit">Save Exam</button>
        </form>
      </FormModal>
    </div>
  );
}
