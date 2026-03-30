import { ChangeEvent, useCallback, useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { useAuth } from '../hooks/useAuth';
import { apiErrorMessage, unwrapList, unwrapItem } from '../utils/apiHelpers';

interface ClassOption { id: number; name: string; }
interface SubjectOption { id: number; name: string; code?: string; }
type TermOption = 'TERM_1' | 'TERM_2' | 'TERM_3';
interface MarksSetupData {
  classes: ClassOption[];
  subjects: SubjectOption[];
  examTypes: string[];
  terms: TermOption[];
  teacherProfileLinked: boolean;
  message?: string;
}
interface LearnerMarkRow { learnerId: number; learnerName: string; mark: number | null; grade: string | null; }

interface BulkMarkPayload {
  classId: number;
  subjectId: number;
  examType: string;
  term: TermOption;
  entries: Array<{ learnerId: number; mark: number | null }>;
}

const TERM_LABELS: Record<TermOption, string> = {
  TERM_1: 'Term 1',
  TERM_2: 'Term 2',
  TERM_3: 'Term 3',
};

function localGrade(mark: number | null): string {
  if (mark === null || Number.isNaN(mark)) return '-';
  if (mark >= 80) return 'A';
  if (mark >= 70) return 'B';
  if (mark >= 60) return 'C';
  if (mark >= 50) return 'D';
  return 'F';
}

export function MarksPage() {
  const { user } = useAuth();
  const canWrite = useMemo(() => user?.role === 'ADMIN' || user?.role === 'TEACHER', [user?.role]);
  const isTeacher = user?.role === 'TEACHER';

  const [setup, setSetup] = useState<MarksSetupData>({ classes: [], subjects: [], examTypes: [], terms: ['TERM_1', 'TERM_2', 'TERM_3'], teacherProfileLinked: true });
  const [selectedClassId, setSelectedClassId] = useState<number | ''>('');
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | ''>('');
  const [selectedExamType, setSelectedExamType] = useState('');
  const [selectedTerm, setSelectedTerm] = useState<TermOption | ''>('');

  const [rows, setRows] = useState<LearnerMarkRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [tableLoading, setTableLoading] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');

  const allSelectorsChosen = Boolean(selectedClassId && selectedSubjectId && selectedExamType && selectedTerm);

  const loadSetup = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/marks/setup');
      const payload = unwrapItem<MarksSetupData>(response.data) ?? { classes: [], subjects: [], examTypes: [], terms: ['TERM_1', 'TERM_2', 'TERM_3'], teacherProfileLinked: true };
      const nextSetup: MarksSetupData = {
        classes: unwrapList<ClassOption>(payload.classes),
        subjects: unwrapList<SubjectOption>(payload.subjects),
        examTypes: unwrapList<string>(payload.examTypes),
        terms: unwrapList<TermOption>(payload.terms),
        teacherProfileLinked: payload.teacherProfileLinked ?? true,
        message: payload.message,
      };
      setSetup(nextSetup);
      setSelectedClassId((prev) => prev || nextSetup.classes[0]?.id || '');
      setSelectedSubjectId((prev) => prev || nextSetup.subjects[0]?.id || '');
      setSelectedExamType((prev) => prev || nextSetup.examTypes[0] || '');
      setSelectedTerm((prev) => prev || nextSetup.terms[0] || '');
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load marks setup data.'));
    } finally {
      setLoading(false);
    }
  }, []);

  const loadLearners = useCallback(async () => {
    if (!allSelectorsChosen) {
      setRows([]);
      return;
    }
    setTableLoading(true);
    setError('');
    try {
      const response = await api.get('/api/marks/learners', {
        params: {
          classId: selectedClassId,
          subjectId: selectedSubjectId,
          examType: selectedExamType,
          term: selectedTerm,
        },
      });
      setRows(unwrapList<LearnerMarkRow>(response.data));
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load learners for marks entry.'));
      setRows([]);
    } finally {
      setTableLoading(false);
    }
  }, [allSelectorsChosen, selectedClassId, selectedExamType, selectedSubjectId, selectedTerm]);

  useEffect(() => { void loadSetup(); }, [loadSetup]);

  useEffect(() => {
    void loadLearners();
  }, [loadLearners]);

  const updateMark = (learnerId: number, value: string) => {
    const parsed = value === '' ? null : Number(value);
    setRows((prev) => prev.map((row) => {
      if (row.learnerId !== learnerId) return row;
      return { ...row, mark: parsed };
    }));
  };

  const validate = (): string | null => {
    if (!allSelectorsChosen) return 'Class, Exam Type, Subject, and Term are required.';
    const invalid = rows.find((row) => row.mark !== null && (row.mark < 0 || row.mark > 100));
    if (invalid) return `Mark for ${invalid.learnerName} must be between 0 and 100.`;
    return null;
  };

  const persist = async (mode: 'draft' | 'submit') => {
    setFeedback('');
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    const payload: BulkMarkPayload = {
      classId: Number(selectedClassId),
      subjectId: Number(selectedSubjectId),
      examType: selectedExamType,
      term: selectedTerm as TermOption,
      entries: rows.map((row) => ({ learnerId: row.learnerId, mark: row.mark })),
    };

    try {
      const response = await api.post(`/api/marks/${mode}`, payload);
      setRows(unwrapList<LearnerMarkRow>(response.data));
      setFeedback(mode === 'draft' ? 'Draft marks saved successfully.' : 'Marks submitted successfully.');
      setError('');
    } catch (err) {
      setError(apiErrorMessage(err, `Failed to ${mode} marks.`));
    }
  };

  return (
    <div className="page">
      <PageHeader title="Marks Entry" subtitle="Select class, exam type, subject, and term to enter learner marks." />
      {feedback ? <p className="success-text">{feedback}</p> : null}
      {error ? <p className="error-text">{error}</p> : null}

      {loading ? <LoadingState title="Loading marks setup..." /> : null}
      {!loading && isTeacher && !setup.teacherProfileLinked ? (
        <EmptyState title="Teacher profile is not linked to this account" message={setup.message || 'Your teacher account is not linked to a teacher profile. Please contact an administrator.'} />
      ) : null}
      {!loading && setup.teacherProfileLinked && setup.classes.length === 0 ? (
        <EmptyState title="No classes" message={setup.message || 'No classes available for marks entry.'} />
      ) : null}

      {!loading && !error && setup.teacherProfileLinked && setup.classes.length > 0 ? (
        <div className="card" style={{ marginBottom: 12 }}>
          <div className="grid">
            <label>Class
              <select value={selectedClassId} onChange={(event) => setSelectedClassId(event.target.value ? Number(event.target.value) : '')}>
                <option value="">Select class</option>
                {setup.classes.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
              </select>
            </label>

            <label>Exam Type
              <select value={selectedExamType} onChange={(event) => setSelectedExamType(event.target.value)}>
                <option value="">Select exam type</option>
                {setup.examTypes.map((item) => <option key={item} value={item}>{item}</option>)}
              </select>
            </label>

            <label>Subject
              <select value={selectedSubjectId} onChange={(event) => setSelectedSubjectId(event.target.value ? Number(event.target.value) : '')}>
                <option value="">Select subject</option>
                {setup.subjects.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
              </select>
            </label>

            <label>Term
              <select value={selectedTerm} onChange={(event) => setSelectedTerm(event.target.value as TermOption | '')}>
                <option value="">Select term</option>
                {setup.terms.map((item) => <option key={item} value={item}>{TERM_LABELS[item] ?? item}</option>)}
              </select>
            </label>
          </div>
        </div>
      ) : null}

      {!loading && allSelectorsChosen && tableLoading ? <LoadingState title="Loading learners..." /> : null}
      {!loading && allSelectorsChosen && !tableLoading && rows.length === 0 ? <EmptyState title="No learners found" message="Assign learners to this class to enter marks." /> : null}

      {!loading && !tableLoading && rows.length > 0 ? (
        <div className="card">
          <table className="table">
            <thead>
              <tr>
                <th>Learner Name</th>
                <th>Mark</th>
                <th>Grade</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.learnerId}>
                  <td>{row.learnerName}</td>
                  <td>
                    <input
                      type="number"
                      min={0}
                      max={100}
                      step={0.01}
                      value={row.mark ?? ''}
                      onChange={(event: ChangeEvent<HTMLInputElement>) => updateMark(row.learnerId, event.target.value)}
                      disabled={!canWrite}
                    />
                  </td>
                  <td>{row.grade ?? localGrade(row.mark)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="action-buttons" style={{ marginTop: 12 }}>
            <button type="button" onClick={() => void persist('draft')} disabled={!canWrite}>Save Draft</button>
            <button type="button" onClick={() => void persist('submit')} disabled={!canWrite}>Submit</button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
