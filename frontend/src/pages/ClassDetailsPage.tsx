import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api from '../services/api';
import { FormModal } from '../components/FormModal';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { PageHeader } from '../components/PageHeader';
import { useAuth } from '../hooks/useAuth';
import {
  ClassRoster,
  addLearnerToClass,
  addSubjectToClass,
  assignClassTeacher,
  getClassById,
  removeSubjectFromClass,
  transferLearner,
  removeLearnerFromClass,
} from '../services/classService';
import { apiErrorMessage, unwrapList } from '../utils/apiHelpers';

interface Teacher { id: number; firstName: string; lastName: string; staffCode?: string; employeeNumber?: string }
interface Student { id: number; admissionNumber: string; fullName: string; classId: number }
interface Subject { id: number; code: string; name: string }

export function ClassDetailsPage() {
  const { id } = useParams();
  const classId = Number(id);
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [data, setData] = useState<ClassRoster | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');

  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [students, setStudents] = useState<Student[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);

  const [teacherId, setTeacherId] = useState('');
  const [learnerId, setLearnerId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [transfer, setTransfer] = useState<{ learnerId: number; targetClassId: string } | null>(null);
  const [classOptions, setClassOptions] = useState<Array<{ id: number; name: string }>>([]);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const roster = await getClassById(classId);
      if (!roster) {
        setData(null);
        return;
      }
      setData(roster);
      setTeacherId(roster.classTeacher?.id ? String(roster.classTeacher.id) : '');

      if (isAdmin) {
        const [teachersResponse, studentsResponse, subjectsResponse, classesResponse] = await Promise.all([
          api.get('/api/teachers'),
          api.get('/api/students'),
          api.get('/api/subjects'),
          api.get('/api/classes'),
        ]);
        setTeachers(unwrapList<Teacher>(teachersResponse.data));
        setStudents(unwrapList<Student>(studentsResponse.data));
        setSubjects(unwrapList<Subject>(subjectsResponse.data));
        setClassOptions(unwrapList<{ id: number; name: string }>(classesResponse.data).filter((item) => item.id !== classId));
      }
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load class details.'));
    } finally {
      setLoading(false);
    }
  }, [classId, isAdmin]);

  useEffect(() => {
    if (!Number.isFinite(classId)) return;
    void load();
  }, [classId, load]);

  const availableLearners = useMemo(() => students.filter((item) => item.classId !== classId), [classId, students]);
  const availableSubjects = useMemo(() => {
    const assigned = new Set(data?.subjects.map((item) => item.id) || []);
    return subjects.filter((item) => !assigned.has(item.id));
  }, [data?.subjects, subjects]);

  const saveTeacher = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await assignClassTeacher(classId, teacherId ? Number(teacherId) : null);
      setFeedback('Class teacher updated successfully.');
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to update class teacher.'));
    }
  };

  const addLearner = async (event: FormEvent) => {
    event.preventDefault();
    if (!learnerId) return;
    try {
      await addLearnerToClass(classId, Number(learnerId));
      setFeedback('Learner assigned successfully.');
      setLearnerId('');
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to assign learner.'));
    }
  };

  const moveLearner = async (event: FormEvent) => {
    event.preventDefault();
    if (!transfer?.targetClassId) return;
    try {
      await transferLearner(classId, transfer.learnerId, Number(transfer.targetClassId));
      setFeedback('Learner transferred successfully.');
      setTransfer(null);
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to transfer learner.'));
    }
  };


  const removeLearner = async (learnerIdToRemove: number) => {
    try {
      await removeLearnerFromClass(classId, learnerIdToRemove);
      setFeedback('Learner removed from class successfully.');
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to remove learner.'));
    }
  };

  const addSubject = async (event: FormEvent) => {
    event.preventDefault();
    if (!subjectId) return;
    try {
      await addSubjectToClass(classId, Number(subjectId));
      setFeedback('Subject assigned successfully.');
      setSubjectId('');
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to assign subject.'));
    }
  };

  const deleteSubject = async (idToRemove: number) => {
    try {
      await removeSubjectFromClass(classId, idToRemove);
      setFeedback('Subject removed successfully.');
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to remove subject.'));
    }
  };

  if (!Number.isFinite(classId)) return <div className="page"><ErrorState message="Invalid class id." /></div>;
  if (loading) return <div className="page"><LoadingState title="Loading class roster..." /></div>;
  if (error && !data) return <div className="page"><ErrorState message={error} onRetry={() => void load()} /></div>;
  if (!data) return <div className="page"><EmptyState title="Class not found" message="This class may have been removed." /></div>;

  return (
    <div className="page">
      <PageHeader title={`Class Roster: ${data.name}`} subtitle={`Grade ${data.level || '-'} • Stream ${data.stream || '-'} • ${data.academicYear || '-'} • ${data.status}`} />
      <Link to="/classes"><button type="button">Back to Classes</button></Link>
      {feedback ? <p className="success-text">{feedback}</p> : null}
      {error ? <p className="error-text">{error}</p> : null}

      <div className="card">
        <h3>Overview</h3>
        <p>Teacher: {data.classTeacher?.fullName || 'Unassigned'}</p>
        <p>Learners: {data.learnerCount}</p>
        <p>Capacity: {data.capacity ?? '-'} ({data.capacityUsagePercent}% used)</p>
        <p>Subjects: {data.subjects.length}</p>
        <p>Status: {data.status}</p>
      </div>

      <div className="card">
        <h3>Teacher</h3>
        {isAdmin ? (
          <form className="form-grid" onSubmit={saveTeacher}>
            <label>Class teacher
              <select value={teacherId} onChange={(e) => setTeacherId(e.target.value)}>
                <option value="">Unassigned</option>
                {teachers.map((teacher) => <option key={teacher.id} value={teacher.id}>{teacher.firstName} {teacher.lastName} ({teacher.staffCode || teacher.employeeNumber || 'N/A'})</option>)}
              </select>
            </label>
            <button type="submit">Save Teacher</button>
          </form>
        ) : <p>{data.classTeacher?.fullName || 'Unassigned'}</p>}
      </div>

      <div className="card">
        <h3>Learners</h3>
        {isAdmin ? (
          <form className="form-grid" onSubmit={addLearner}>
            <label>Add learner
              <select value={learnerId} onChange={(e) => setLearnerId(e.target.value)}>
                <option value="">Select learner</option>
                {availableLearners.map((student) => <option key={student.id} value={student.id}>{student.fullName} ({student.admissionNumber})</option>)}
              </select>
            </label>
            <button type="submit">Add Learner</button>
          </form>
        ) : null}
        <table className="table"><thead><tr><th>Admission #</th><th>Name</th><th>Status</th>{isAdmin ? <th>Actions</th> : null}</tr></thead><tbody>{data.learners.map((learner) => <tr key={learner.id}><td>{learner.admissionNumber}</td><td>{learner.fullName}</td><td>{learner.status}</td>{isAdmin ? <td><div className="action-buttons"><button type="button" onClick={() => setTransfer({ learnerId: learner.id, targetClassId: '' })}>Transfer</button><button type="button" className="danger-button" onClick={() => void removeLearner(learner.id)}>Remove</button></div></td> : null}</tr>)}</tbody></table>
      </div>

      <div className="card">
        <h3>Subjects</h3>
        {isAdmin ? (
          <form className="form-grid" onSubmit={addSubject}>
            <label>Add subject
              <select value={subjectId} onChange={(e) => setSubjectId(e.target.value)}>
                <option value="">Select subject</option>
                {availableSubjects.map((subject) => <option key={subject.id} value={subject.id}>{subject.code} - {subject.name}</option>)}
              </select>
            </label>
            <button type="submit">Add Subject</button>
          </form>
        ) : null}
        <table className="table"><thead><tr><th>Code</th><th>Name</th>{isAdmin ? <th>Actions</th> : null}</tr></thead><tbody>{data.subjects.map((subject) => <tr key={subject.id}><td>{subject.code}</td><td>{subject.name}</td>{isAdmin ? <td><button type="button" className="danger-button" onClick={() => void deleteSubject(subject.id)}>Remove</button></td> : null}</tr>)}</tbody></table>
      </div>

      <FormModal title="Transfer learner" open={transfer !== null && isAdmin} onClose={() => setTransfer(null)}>
        <form className="form-grid" onSubmit={moveLearner}>
          <label>Target class
            <select value={transfer?.targetClassId || ''} onChange={(e) => setTransfer((prev) => (prev ? { ...prev, targetClassId: e.target.value } : prev))}>
              <option value="">Select class</option>
              {classOptions.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
          </label>
          <button type="submit">Transfer</button>
        </form>
      </FormModal>
    </div>
  );
}
