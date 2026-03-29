import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { DataTable } from '../components/DataTable';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { FormModal } from '../components/FormModal';
import { useAuth } from '../hooks/useAuth';
import { apiErrorMessage, unwrapList } from '../utils/apiHelpers';

interface ExamOption { id: number; title?: string; examCode?: string; totalMarks?: number; }
interface StudentOption { id: number; fullName: string; admissionNumber: string; }
interface MarksSetupData { exams: ExamOption[]; students: StudentOption[]; }

function marksSetupErrorMessage(err: unknown): string {
  const status = (err as { response?: { status?: number } })?.response?.status;
  if (status === 403) {
    return 'You are not authorized to load marks setup data. Please contact an administrator.';
  }
  if (status === 401) {
    return 'Your session has expired. Please sign in again.';
  }
  return apiErrorMessage(err, 'Failed to load marks module.');
}

export function MarksPage() {
  const { user } = useAuth();
  const canWrite = useMemo(() => user?.role === 'ADMIN' || user?.role === 'TEACHER', [user?.role]);
  const [exams, setExams] = useState<ExamOption[]>([]);
  const [students, setStudents] = useState<StudentOption[]>([]);
  const [selectedExamId, setSelectedExamId] = useState<number | ''>('');
  const [selectedStudentId, setSelectedStudentId] = useState('');
  const [score, setScore] = useState('');
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [open, setOpen] = useState(false);

  const loadSetupData = useCallback(async () => {
    const setupResponse = await api.get('/api/marks/setup');
    const setup = (setupResponse.data?.data ?? setupResponse.data) as MarksSetupData;
    const examList = unwrapList<ExamOption>(setup.exams);
    const studentList = unwrapList<StudentOption>(setup.students);
    setExams(examList);
    setStudents(studentList);
    if (examList.length > 0) setSelectedExamId((prev) => prev || examList[0].id);
    if (!selectedStudentId && studentList[0]) setSelectedStudentId(String(studentList[0].id));
  }, [selectedStudentId]);

  const loadMarks = useCallback(async (examId: number) => {
    const marksResponse = await api.get(`/api/marks/exam/${examId}`);
    setRows(unwrapList<Record<string, unknown>>(marksResponse.data));
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      await loadSetupData();
    } catch (err) {
      setError(marksSetupErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [loadSetupData]);

  useEffect(() => { void load(); }, [load]);

  useEffect(() => {
    if (!selectedExamId) return;
    setLoading(true);
    setError('');
    loadMarks(selectedExamId)
      .catch((err) => setError(apiErrorMessage(err, 'Failed to load marks.')))
      .finally(() => setLoading(false));
  }, [loadMarks, selectedExamId]);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!selectedExamId || !selectedStudentId || !score) {
      setError('Exam, student, and score are required.');
      return;
    }
    try {
      await api.post('/api/marks', { examId: Number(selectedExamId), studentId: Number(selectedStudentId), score: Number(score) });
      setFeedback('Mark saved successfully.');
      setOpen(false);
      setScore('');
      await loadMarks(Number(selectedExamId));
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to save mark.'));
    }
  };

  return (
    <div className="page">
      <PageHeader title="Marks" subtitle="View marks by exam and submit student scores." actionLabel="Add Mark" onAction={() => setOpen(true)} disabled={!canWrite || exams.length === 0 || students.length === 0} disabledReason={exams.length === 0 ? 'Create an exam first.' : students.length === 0 ? 'Add students first.' : 'Permission denied.'} />
      {feedback ? <p className="success-text">{feedback}</p> : null}
      {loading ? <LoadingState title="Loading marks..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void load()} /> : null}

      {!loading && !error && exams.length === 0 ? <EmptyState title="No exams available" message="Create an exam first to start entering marks." /> : null}

      {!loading && !error && exams.length > 0 ? (
        <div className="card" style={{ marginBottom: 12 }}>
          <label htmlFor="marks-exam">Exam</label>{' '}
          <select id="marks-exam" value={selectedExamId} onChange={(event) => setSelectedExamId(Number(event.target.value))}>
            {exams.map((exam) => <option key={exam.id} value={exam.id}>{exam.title ?? exam.examCode ?? `Exam ${exam.id}`}</option>)}
          </select>
        </div>
      ) : null}

      {!loading && !error && exams.length > 0 && rows.length === 0 ? <EmptyState title="No marks submitted" message="This exam has no marks yet." /> : null}
      {!loading && !error && rows.length > 0 ? <DataTable rows={rows} /> : null}

      <FormModal title="Add Mark" open={open} onClose={() => setOpen(false)}>
        <form className="form-grid" onSubmit={submit}>
          <label>Exam<select value={selectedExamId} onChange={(event) => setSelectedExamId(Number(event.target.value))}>{exams.map((exam) => <option key={exam.id} value={exam.id}>{exam.title ?? exam.examCode}</option>)}</select></label>
          <label>Student<select value={selectedStudentId} onChange={(event) => setSelectedStudentId(event.target.value)}>{students.map((student) => <option key={student.id} value={student.id}>{student.fullName} ({student.admissionNumber})</option>)}</select></label>
          <label>Score<input type="number" step="0.01" value={score} onChange={(event) => setScore(event.target.value)} /></label>
          <button type="submit">Save Mark</button>
        </form>
      </FormModal>
    </div>
  );
}
