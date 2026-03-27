import { useCallback, useEffect, useState } from 'react';
import { AxiosError } from 'axios';
import api from '../services/api';
import { DataTable } from '../components/DataTable';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';

interface ExamOption {
  id: number;
  title?: string;
  examCode?: string;
}

function message(error: unknown) {
  const axiosError = error as AxiosError<{ message?: string }>;
  return axiosError.response?.data?.message ?? axiosError.message ?? 'Failed to load marks.';
}

export function MarksPage() {
  const [exams, setExams] = useState<ExamOption[]>([]);
  const [selectedExamId, setSelectedExamId] = useState<number | ''>('');
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadExams = useCallback(async () => {
    try {
      const examResponse = await api.get('/api/exams');
      const examList = examResponse.data?.data ?? [];
      const normalizedExams = Array.isArray(examList) ? examList : [];
      setExams(normalizedExams);
      if (normalizedExams.length > 0) {
        setSelectedExamId((prev) => prev || normalizedExams[0].id);
      }
    } catch (err) {
      throw new Error(message(err));
    }
  }, []);

  const loadMarks = useCallback(async (examId: number) => {
    const marksResponse = await api.get(`/api/marks/exam/${examId}`);
    const list = marksResponse.data?.data ?? [];
    setRows(Array.isArray(list) ? list : []);
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      await loadExams();
    } catch (err) {
      setError(message(err));
      if (import.meta.env.DEV) {
        console.error('[Marks] bootstrap failed', err);
      }
    } finally {
      setLoading(false);
    }
  }, [loadExams]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!selectedExamId) return;

    setLoading(true);
    setError('');
    loadMarks(selectedExamId)
      .catch((err) => {
        setError(message(err));
        if (import.meta.env.DEV) {
          console.error('[Marks] load failed', err);
        }
      })
      .finally(() => setLoading(false));
  }, [loadMarks, selectedExamId]);

  return (
    <div className="page">
      <PageHeader title="Marks" subtitle="View and manage marks by exam." actionLabel="Add Mark" disabled />
      {loading ? <LoadingState title="Loading marks..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void load()} /> : null}

      {!loading && !error && exams.length === 0 ? (
        <EmptyState title="No exams available" message="Create an exam first to start entering marks." />
      ) : null}

      {!loading && !error && exams.length > 0 ? (
        <div className="card" style={{ marginBottom: 12 }}>
          <label htmlFor="marks-exam">Exam</label>{' '}
          <select
            id="marks-exam"
            value={selectedExamId}
            onChange={(event) => setSelectedExamId(Number(event.target.value))}
          >
            {exams.map((exam) => (
              <option key={exam.id} value={exam.id}>
                {exam.title ?? exam.examCode ?? `Exam ${exam.id}`}
              </option>
            ))}
          </select>
        </div>
      ) : null}

      {!loading && !error && exams.length > 0 && rows.length === 0 ? (
        <EmptyState title="No marks submitted" message="This exam has no marks yet." />
      ) : null}
      {!loading && !error && rows.length > 0 ? <DataTable rows={rows} /> : null}
    </div>
  );
}
