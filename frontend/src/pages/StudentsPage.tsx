import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { AxiosError } from 'axios';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { apiErrorMessage, unwrapItem, unwrapList } from '../utils/apiHelpers';
import { useAuth } from '../hooks/useAuth';

type StudentStatus = 'ACTIVE' | 'PENDING' | 'SUSPENDED' | 'TRANSFERRED' | 'GRADUATED';

interface Student {
  id: number;
  firstName: string;
  middleName?: string | null;
  lastName: string;
  fullName: string;
  preferredName?: string | null;
  admissionNumber: string;
  gender: string;
  dateOfBirth?: string | null;
  grade: string;
  enrollmentDate: string;
  guardianName?: string | null;
  guardianPhone?: string | null;
  status: StudentStatus;
  notes?: string | null;
  schoolClassId?: number | null;
  schoolClassName?: string | null;
  schoolClassStream?: string | null;
  [key: string]: unknown;
}

interface SchoolClass {
  id: number;
  name: string;
  stream?: string;
  status?: 'ACTIVE' | 'INACTIVE';
}

interface ApiErrorData {
  message?: string;
  data?: Record<string, string>;
}

const statuses: StudentStatus[] = ['ACTIVE', 'PENDING', 'SUSPENDED', 'TRANSFERRED', 'GRADUATED'];

const blankForm = {
  firstName: '',
  middleName: '',
  lastName: '',
  preferredName: '',
  admissionNumber: '',
  gender: '',
  dateOfBirth: '',
  grade: '',
  enrollmentDate: '',
  status: '',
  schoolClassId: '',
  guardianName: '',
  guardianRelationship: '',
  guardianPhone: '',
  address: '',
  email: '',
  phoneNumber: '',
  notes: '',
};

type StudentForm = typeof blankForm;

const blankDeleteConfirmation = {
  firstName: '',
  lastName: '',
  admissionNumber: '',
  gender: '',
  grade: '',
  enrollmentDate: '',
  status: '',
};

type DeleteConfirmForm = typeof blankDeleteConfirmation;

const optionalStringOrNull = (value: string) => {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
};

export function StudentsPage() {
  const { user, authReady } = useAuth();
  const canManage = useMemo(() => user?.role === 'ADMIN', [user?.role]);

  const [rows, setRows] = useState<Student[]>([]);
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [form, setForm] = useState<StudentForm>(blankForm);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [editingId, setEditingId] = useState<number | null>(null);

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteStudent, setDeleteStudent] = useState<Student | null>(null);
  const [deleteForm, setDeleteForm] = useState<DeleteConfirmForm>(blankDeleteConfirmation);

  const loadStudents = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/students');
      const students = unwrapList<Student>(response.data);
      setRows(students);
      if (selectedStudent) {
        setSelectedStudent(students.find((student) => student.id === selectedStudent.id) ?? null);
      }
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load students.'));
    } finally {
      setLoading(false);
    }
  }, [selectedStudent]);

  const loadClasses = useCallback(async () => {
    const response = await api.get('/api/classes');
    const allClasses = unwrapList<SchoolClass>(response.data);
    setClasses(allClasses.filter((item) => (item.status ?? 'ACTIVE') === 'ACTIVE'));
  }, []);

  useEffect(() => {
    if (!authReady || !user) return;
    void Promise.all([loadStudents(), loadClasses()]);
  }, [authReady, loadClasses, loadStudents, user]);

  const setField = (field: keyof StudentForm, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setFormErrors((prev) => ({ ...prev, [field]: '' }));
  };

  const validate = () => {
    const errors: Record<string, string> = {};
    const required: Array<[keyof StudentForm, string]> = [
      ['firstName', 'First name is required.'],
      ['lastName', 'Last name is required.'],
      ['admissionNumber', 'Admission number is required.'],
      ['gender', 'Gender is required.'],
      ['grade', 'Grade is required.'],
      ['enrollmentDate', 'Enrollment date is required.'],
      ['status', 'Status is required.'],
    ];

    required.forEach(([field, message]) => {
      if (!String(form[field] ?? '').trim()) errors[field] = message;
    });

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const resetForm = () => {
    setEditingId(null);
    setForm(blankForm);
    setFormErrors({});
  };

  const fillFormFromStudent = (student: Student) => {
    setEditingId(student.id);
    setForm({
      ...blankForm,
      firstName: student.firstName ?? '',
      middleName: String(student.middleName ?? ''),
      lastName: student.lastName ?? '',
      preferredName: String(student.preferredName ?? ''),
      admissionNumber: student.admissionNumber ?? '',
      gender: student.gender ?? '',
      dateOfBirth: String(student.dateOfBirth ?? ''),
      grade: student.grade ?? '',
      enrollmentDate: String(student.enrollmentDate ?? ''),
      status: student.status ?? '',
      schoolClassId: student.schoolClassId ? String(student.schoolClassId) : '',
      guardianName: String(student.guardianName ?? ''),
      guardianRelationship: String(student.guardianRelationship ?? ''),
      guardianPhone: String(student.guardianPhone ?? ''),
      address: String(student.address ?? ''),
      email: String(student.email ?? ''),
      phoneNumber: String(student.phoneNumber ?? ''),
      notes: String(student.notes ?? ''),
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setFeedback('');
    setError('');
    if (!validate()) return;

    const payload = {
      firstName: form.firstName.trim(),
      middleName: optionalStringOrNull(form.middleName),
      lastName: form.lastName.trim(),
      preferredName: optionalStringOrNull(form.preferredName),
      admissionNumber: form.admissionNumber.trim(),
      gender: form.gender.trim(),
      dateOfBirth: optionalStringOrNull(form.dateOfBirth),
      grade: form.grade.trim(),
      enrollmentDate: form.enrollmentDate,
      status: form.status,
      schoolClassId: form.schoolClassId ? Number(form.schoolClassId) : null,
      guardianName: optionalStringOrNull(form.guardianName),
      guardianRelationship: optionalStringOrNull(form.guardianRelationship),
      guardianPhone: optionalStringOrNull(form.guardianPhone),
      address: optionalStringOrNull(form.address),
      email: optionalStringOrNull(form.email),
      phoneNumber: optionalStringOrNull(form.phoneNumber),
      notes: optionalStringOrNull(form.notes),
    };

    try {
      setSaving(true);
      if (editingId) {
        await api.put(`/api/students/${editingId}`, payload);
        setFeedback('Student updated successfully.');
      } else {
        await api.post('/api/students', payload);
        setFeedback('Student created successfully.');
      }
      resetForm();
      await loadStudents();
    } catch (err) {
      const axiosError = err as AxiosError<ApiErrorData>;
      const backendErrors = axiosError.response?.data?.data;
      if (backendErrors) {
        setFormErrors(backendErrors);
      }
      setError(apiErrorMessage(err, 'Failed to save student.'));
    } finally {
      setSaving(false);
    }
  };

  const openDeleteConfirmation = (student: Student) => {
    setDeleteStudent(student);
    setDeleteForm(blankDeleteConfirmation);
    setShowDeleteConfirm(true);
  };

  const closeDeleteConfirmation = () => {
    setShowDeleteConfirm(false);
    setDeleteStudent(null);
    setDeleteForm(blankDeleteConfirmation);
  };

  const canDelete = Object.values(deleteForm).every((value) => value.trim().length > 0);

  const confirmDelete = async () => {
    if (!deleteStudent || !canDelete) return;

    try {
      await api.delete(`/api/students/${deleteStudent.id}/confirm-delete`, {
        data: {
          firstName: deleteForm.firstName.trim(),
          lastName: deleteForm.lastName.trim(),
          admissionNumber: deleteForm.admissionNumber.trim(),
          gender: deleteForm.gender.trim(),
          grade: deleteForm.grade.trim(),
          enrollmentDate: deleteForm.enrollmentDate,
          status: deleteForm.status,
        },
      });
      setFeedback('Student deleted successfully.');
      closeDeleteConfirmation();
      if (selectedStudent?.id === deleteStudent.id) {
        setSelectedStudent(null);
      }
      await loadStudents();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to delete student.'));
    }
  };

  if (!authReady) {
    return <div className="page"><LoadingState title="Restoring session..." /></div>;
  }

  return (
    <div className="page">
      <PageHeader title="Students" subtitle="Create, edit, review, and safely delete students with confirmation." />
      {feedback ? <p className="success-text">{feedback}</p> : null}
      {error ? <p className="error-text">{error}</p> : null}

      <div className="card">
        <h3>{editingId ? 'Edit Student' : 'Create Student'}</h3>
        <form className="form-grid learner-form" onSubmit={submit}>
          <div className="grid two-col-grid">
            <label>First name<input value={form.firstName} onChange={(e) => setField('firstName', e.target.value)} />{formErrors.firstName ? <span className="field-error">{formErrors.firstName}</span> : null}</label>
            <label>Middle name<input value={form.middleName} onChange={(e) => setField('middleName', e.target.value)} /></label>
            <label>Last name<input value={form.lastName} onChange={(e) => setField('lastName', e.target.value)} />{formErrors.lastName ? <span className="field-error">{formErrors.lastName}</span> : null}</label>
            <label>Preferred name<input value={form.preferredName} onChange={(e) => setField('preferredName', e.target.value)} /></label>
            <label>Admission number<input value={form.admissionNumber} onChange={(e) => setField('admissionNumber', e.target.value)} />{formErrors.admissionNumber ? <span className="field-error">{formErrors.admissionNumber}</span> : null}</label>
            <label>Gender<input value={form.gender} onChange={(e) => setField('gender', e.target.value)} />{formErrors.gender ? <span className="field-error">{formErrors.gender}</span> : null}</label>
            <label>Grade<input value={form.grade} onChange={(e) => setField('grade', e.target.value)} />{formErrors.grade ? <span className="field-error">{formErrors.grade}</span> : null}</label>
            <label>Enrollment date<input type="date" value={form.enrollmentDate} onChange={(e) => setField('enrollmentDate', e.target.value)} />{formErrors.enrollmentDate ? <span className="field-error">{formErrors.enrollmentDate}</span> : null}</label>
            <label>Status<select value={form.status} onChange={(e) => setField('status', e.target.value)}><option value="">Select status</option>{statuses.map((status) => <option key={status}>{status}</option>)}</select>{formErrors.status ? <span className="field-error">{formErrors.status}</span> : null}</label>
            <label>Date of birth<input type="date" value={form.dateOfBirth} onChange={(e) => setField('dateOfBirth', e.target.value)} /></label>
            <label>School class (optional)
              <select value={form.schoolClassId} onChange={(e) => setField('schoolClassId', e.target.value)}>
                <option value="">No class</option>
                {classes.map((schoolClass) => (
                  <option key={schoolClass.id} value={schoolClass.id}>{schoolClass.name}{schoolClass.stream ? ` ${schoolClass.stream}` : ''}</option>
                ))}
              </select>
            </label>
            <label>Guardian name<input value={form.guardianName} onChange={(e) => setField('guardianName', e.target.value)} /></label>
            <label>Guardian relationship<input value={form.guardianRelationship} onChange={(e) => setField('guardianRelationship', e.target.value)} /></label>
            <label>Guardian phone<input value={form.guardianPhone} onChange={(e) => setField('guardianPhone', e.target.value)} /></label>
            <label>Phone number<input value={form.phoneNumber} onChange={(e) => setField('phoneNumber', e.target.value)} /></label>
            <label>Email<input value={form.email} onChange={(e) => setField('email', e.target.value)} /></label>
            <label>Address<input value={form.address} onChange={(e) => setField('address', e.target.value)} /></label>
            <label className="full-width">Notes<textarea value={form.notes} onChange={(e) => setField('notes', e.target.value)} /></label>
          </div>
          <div className="toolbar-row">
            <button type="submit" disabled={!canManage || saving}>{saving ? 'Saving...' : editingId ? 'Update Student' : 'Create Student'}</button>
            <button type="button" className="text-button" onClick={resetForm}>Reset</button>
          </div>
        </form>
      </div>

      <div className="card">
        <h3>Student List</h3>
        {loading ? <LoadingState title="Loading students..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={() => void loadStudents()} /> : null}
        {!loading && !error && rows.length === 0 ? <EmptyState title="No students found" message="Create students to get started." /> : null}

        {!loading && !error && rows.length > 0 ? (
          <table className="table">
            <thead>
              <tr>
                <th>Admission #</th>
                <th>Name</th>
                <th>Gender</th>
                <th>Grade</th>
                <th>Status</th>
                <th>Class</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((student) => (
                <tr key={student.id}>
                  <td>{student.admissionNumber}</td>
                  <td>{student.fullName}</td>
                  <td>{student.gender}</td>
                  <td>{student.grade}</td>
                  <td>{student.status}</td>
                  <td>{student.schoolClassName ? `${student.schoolClassName}${student.schoolClassStream ? ` ${student.schoolClassStream}` : ''}` : '-'}</td>
                  <td>
                    <button type="button" onClick={async () => {
                      const response = await api.get(`/api/students/${student.id}`);
                      const details = unwrapItem<Student>(response.data);
                      if (details) {
                        setSelectedStudent(details);
                        fillFormFromStudent(details);
                      }
                    }}>Edit</button>
                    <button type="button" className="text-button" onClick={async () => {
                      const response = await api.get(`/api/students/${student.id}`);
                      const details = unwrapItem<Student>(response.data);
                      if (details) setSelectedStudent(details);
                    }}>Details</button>
                    <button type="button" className="text-button" disabled={!canManage} onClick={() => openDeleteConfirmation(student)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </div>

      {selectedStudent ? (
        <div className="card">
          <h3>Student details</h3>
          <p><strong>Name:</strong> {selectedStudent.fullName}</p>
          <p><strong>Admission:</strong> {selectedStudent.admissionNumber}</p>
          <p><strong>Status:</strong> {selectedStudent.status}</p>
          <p><strong>Grade:</strong> {selectedStudent.grade}</p>
          <p><strong>Enrollment:</strong> {selectedStudent.enrollmentDate}</p>
          <p><strong>Class:</strong> {selectedStudent.schoolClassName ? `${selectedStudent.schoolClassName}${selectedStudent.schoolClassStream ? ` ${selectedStudent.schoolClassStream}` : ''}` : '-'}</p>
        </div>
      ) : null}

      {showDeleteConfirm && deleteStudent ? (
        <div className="card">
          <h3>Confirm Student Delete</h3>
          <p>Re-enter core student details exactly to delete <strong>{deleteStudent.fullName}</strong>.</p>
          <div className="grid two-col-grid">
            <label>First name<input value={deleteForm.firstName} onChange={(e) => setDeleteForm((prev) => ({ ...prev, firstName: e.target.value }))} /></label>
            <label>Last name<input value={deleteForm.lastName} onChange={(e) => setDeleteForm((prev) => ({ ...prev, lastName: e.target.value }))} /></label>
            <label>Admission number<input value={deleteForm.admissionNumber} onChange={(e) => setDeleteForm((prev) => ({ ...prev, admissionNumber: e.target.value }))} /></label>
            <label>Gender<input value={deleteForm.gender} onChange={(e) => setDeleteForm((prev) => ({ ...prev, gender: e.target.value }))} /></label>
            <label>Grade<input value={deleteForm.grade} onChange={(e) => setDeleteForm((prev) => ({ ...prev, grade: e.target.value }))} /></label>
            <label>Enrollment date<input type="date" value={deleteForm.enrollmentDate} onChange={(e) => setDeleteForm((prev) => ({ ...prev, enrollmentDate: e.target.value }))} /></label>
            <label>Status<select value={deleteForm.status} onChange={(e) => setDeleteForm((prev) => ({ ...prev, status: e.target.value }))}><option value="">Select status</option>{statuses.map((status) => <option key={status}>{status}</option>)}</select></label>
          </div>
          <div className="toolbar-row">
            <button type="button" className="danger-button" disabled={!canDelete} onClick={() => void confirmDelete()}>Confirm Delete</button>
            <button type="button" className="text-button" onClick={closeDeleteConfirmation}>Cancel</button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
