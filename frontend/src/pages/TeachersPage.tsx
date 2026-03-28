import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { AxiosError } from 'axios';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { apiErrorMessage, unwrapList } from '../utils/apiHelpers';
import { useAuth } from '../hooks/useAuth';

interface Teacher {
  id: number;
  employeeNumber: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  fullName: string;
  gender: string;
  dateOfBirth?: string;
  phoneNumber: string;
  email: string;
  nationalId?: string;
  passportNumber?: string;
  department: string;
  specialization: string;
  employmentType: string;
  hireDate: string;
  status: TeacherStatus;
  address: string;
  title?: string;
  alternativePhoneNumber?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelationship?: string;
  qualification?: string;
  highestEducationLevel?: string;
  yearsOfExperience?: number;
  staffRole?: string;
  salaryGrade?: string;
  notes?: string;
  profilePhotoUrl?: string;
  enabled: boolean;
}

interface Subject {
  id: number;
  name: string;
}

type TeacherStatus = 'ACTIVE' | 'INACTIVE' | 'ON_LEAVE' | 'RETIRED' | 'TERMINATED';

const departments = ['Sciences', 'Mathematics', 'Languages', 'Humanities', 'ICT', 'Arts', 'Sports'];
const genders = ['MALE', 'FEMALE', 'OTHER'];
const employmentTypes = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'TEMPORARY', 'INTERN'];
const statuses: TeacherStatus[] = ['ACTIVE', 'INACTIVE', 'ON_LEAVE', 'RETIRED', 'TERMINATED'];
const titles = ['MR', 'MRS', 'MISS', 'MS', 'DR', 'PROF'];

const blankForm = {
  employeeNumber: '',
  firstName: '',
  middleName: '',
  lastName: '',
  gender: 'MALE',
  dateOfBirth: '',
  phoneNumber: '',
  alternativePhoneNumber: '',
  email: '',
  nationalId: '',
  passportNumber: '',
  department: departments[0],
  specialization: '',
  employmentType: 'FULL_TIME',
  hireDate: '',
  status: 'ACTIVE' as TeacherStatus,
  address: '',
  title: 'MR',
  emergencyContactName: '',
  emergencyContactPhone: '',
  emergencyContactRelationship: '',
  qualification: '',
  highestEducationLevel: '',
  yearsOfExperience: '',
  staffRole: '',
  salaryGrade: '',
  notes: '',
  profilePhotoUrl: '',
  password: 'Teacher123!',
  enabled: true,
};

type TeacherForm = typeof blankForm;

interface ApiErrorData {
  message?: string;
  data?: Record<string, string>;
}

export function TeachersPage() {
  const { user, authReady } = useAuth();
  const canManage = useMemo(() => user?.role === 'ADMIN', [user?.role]);

  const [rows, setRows] = useState<Teacher[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [form, setForm] = useState<TeacherForm>(blankForm);
  const [editingId, setEditingId] = useState<number | null>(null);

  const [keyword, setKeyword] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('');
  const [subjectFilter, setSubjectFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const loadSubjects = useCallback(async () => {
    const response = await api.get('/api/subjects');
    setSubjects(unwrapList<Subject>(response.data));
  }, []);

  const loadTeachers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const endpoint = keyword.trim() ? '/api/teachers/search' : '/api/teachers';
      const response = await api.get(endpoint, { params: keyword.trim() ? { keyword: keyword.trim() } : {} });
      setRows(unwrapList<Teacher>(response.data));
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load teachers.'));
    } finally {
      setLoading(false);
    }
  }, [keyword]);

  useEffect(() => {
    if (!authReady || !user) return;
    void Promise.all([loadSubjects(), loadTeachers()]);
  }, [authReady, loadSubjects, loadTeachers, user]);

  const setField = (field: keyof TeacherForm, value: string | boolean) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setFormErrors((prev) => ({ ...prev, [field]: '' }));
  };

  const validate = () => {
    const next: Record<string, string> = {};
    const required: Array<[keyof TeacherForm, string]> = [
      ['employeeNumber', 'Employee number is required.'],
      ['firstName', 'First name is required.'],
      ['lastName', 'Last name is required.'],
      ['gender', 'Gender is required.'],
      ['phoneNumber', 'Phone number is required.'],
      ['email', 'Email is required.'],
      ['department', 'Department is required.'],
      ['specialization', 'Main subject/specialization is required.'],
      ['hireDate', 'Hire date is required.'],
      ['status', 'Status is required.'],
      ['address', 'Address is required.'],
    ];

    required.forEach(([field, message]) => {
      if (!String(form[field] ?? '').trim()) next[field] = message;
    });

    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      next.email = 'Email must be valid.';
    }

    if (form.profilePhotoUrl && !/^https?:\/\//i.test(form.profilePhotoUrl)) {
      next.profilePhotoUrl = 'Profile photo URL must start with http:// or https://';
    }

    setFormErrors(next);
    return Object.keys(next).length === 0;
  };

  const resetForm = () => {
    setEditingId(null);
    setForm(blankForm);
    setFormErrors({});
  };

  const fillForm = (teacher: Teacher) => {
    setEditingId(teacher.id);
    setForm({
      ...blankForm,
      ...teacher,
      yearsOfExperience: teacher.yearsOfExperience ? String(teacher.yearsOfExperience) : '',
      dateOfBirth: teacher.dateOfBirth || '',
      hireDate: teacher.hireDate || '',
      password: '',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setFeedback('');

    if (!validate()) return;

    const payload = {
      ...form,
      yearsOfExperience: form.yearsOfExperience ? Number(form.yearsOfExperience) : null,
      dateOfBirth: form.dateOfBirth || null,
      nationalId: form.nationalId || null,
      passportNumber: form.passportNumber || null,
      alternativePhoneNumber: form.alternativePhoneNumber || null,
      password: form.password || null,
    };

    try {
      setSaving(true);
      if (editingId) {
        await api.put(`/api/teachers/${editingId}`, payload);
        setFeedback('Teacher updated successfully.');
      } else {
        await api.post('/api/teachers', payload);
        setFeedback('Teacher registered successfully.');
      }
      resetForm();
      await loadTeachers();
    } catch (err) {
      const axiosError = err as AxiosError<ApiErrorData>;
      const backendErrors = axiosError.response?.data?.data;
      if (backendErrors && typeof backendErrors === 'object') {
        setFormErrors(backendErrors);
      }
      const message = apiErrorMessage(err, 'Failed to save teacher.');
      if (message.toLowerCase().includes('employee number')) {
        setFormErrors((prev) => ({ ...prev, employeeNumber: message }));
      }
      if (message.toLowerCase().includes('email')) {
        setFormErrors((prev) => ({ ...prev, email: message }));
      }
      setError(message);
    } finally {
      setSaving(false);
    }
  };

  const onDelete = async (teacher: Teacher) => {
    try {
      setError('');
      await api.delete(`/api/teachers/${teacher.id}`);
      setFeedback('Teacher deactivated successfully.');
      await loadTeachers();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to deactivate teacher.'));
    }
  };

  const filteredRows = useMemo(() => rows.filter((item) => {
    const matchesDepartment = !departmentFilter || item.department === departmentFilter;
    const matchesSubject = !subjectFilter || item.specialization === subjectFilter;
    const matchesStatus = !statusFilter || item.status === statusFilter;
    return matchesDepartment && matchesSubject && matchesStatus;
  }), [departmentFilter, rows, statusFilter, subjectFilter]);

  if (!authReady) {
    return <div className="page"><LoadingState title="Restoring session..." /></div>;
  }

  return (
    <div className="page">
      <PageHeader
        title="Teacher Registration"
        subtitle="Register teachers with required staffing details first, then add more profile information as needed."
      />

      {feedback ? <p className="success-text">{feedback}</p> : null}
      {error ? <p className="error-text">{error}</p> : null}

      <div className="card">
        <h3>{editingId ? 'Update Teacher' : 'Register Teacher'}</h3>
        <form className="form-grid learner-form" onSubmit={submit}>
          <div className="form-section">
            <h4>Core teacher details</h4>
            <div className="grid two-col-grid">
              <label>Employee number
                <input value={form.employeeNumber} onChange={(e) => setField('employeeNumber', e.target.value)} />
                {formErrors.employeeNumber ? <span className="field-error">{formErrors.employeeNumber}</span> : null}
              </label>
              <label>First name
                <input value={form.firstName} onChange={(e) => setField('firstName', e.target.value)} />
                {formErrors.firstName ? <span className="field-error">{formErrors.firstName}</span> : null}
              </label>
              <label>Last name
                <input value={form.lastName} onChange={(e) => setField('lastName', e.target.value)} />
                {formErrors.lastName ? <span className="field-error">{formErrors.lastName}</span> : null}
              </label>
              <label>Gender
                <select value={form.gender} onChange={(e) => setField('gender', e.target.value)}>{genders.map((item) => <option key={item}>{item}</option>)}</select>
                {formErrors.gender ? <span className="field-error">{formErrors.gender}</span> : null}
              </label>
              <label>Date of birth
                <input type="date" value={form.dateOfBirth} onChange={(e) => setField('dateOfBirth', e.target.value)} />
              </label>
              <label>Phone number
                <input value={form.phoneNumber} onChange={(e) => setField('phoneNumber', e.target.value)} />
                {formErrors.phoneNumber ? <span className="field-error">{formErrors.phoneNumber}</span> : null}
              </label>
              <label>Email address
                <input type="email" value={form.email} onChange={(e) => setField('email', e.target.value)} />
                {formErrors.email ? <span className="field-error">{formErrors.email}</span> : null}
              </label>
              <label>National ID / Passport number
                <div className="grid two-col-grid">
                  <input placeholder="National ID" value={form.nationalId} onChange={(e) => setField('nationalId', e.target.value)} />
                  <input placeholder="Passport number" value={form.passportNumber} onChange={(e) => setField('passportNumber', e.target.value)} />
                </div>
              </label>
              <label>Department
                <select value={form.department} onChange={(e) => setField('department', e.target.value)}>
                  {departments.map((item) => <option key={item}>{item}</option>)}
                </select>
                {formErrors.department ? <span className="field-error">{formErrors.department}</span> : null}
              </label>
              <label>Main subject / specialization
                <select value={form.specialization} onChange={(e) => setField('specialization', e.target.value)}>
                  <option value="">Select specialization</option>
                  {subjects.map((item) => <option key={item.id} value={item.name}>{item.name}</option>)}
                </select>
                {formErrors.specialization ? <span className="field-error">{formErrors.specialization}</span> : null}
              </label>
              <label>Employment type
                <select value={form.employmentType} onChange={(e) => setField('employmentType', e.target.value)}>{employmentTypes.map((item) => <option key={item}>{item}</option>)}</select>
              </label>
              <label>Hire date
                <input type="date" value={form.hireDate} onChange={(e) => setField('hireDate', e.target.value)} />
                {formErrors.hireDate ? <span className="field-error">{formErrors.hireDate}</span> : null}
              </label>
              <label>Status
                <select value={form.status} onChange={(e) => setField('status', e.target.value)}>{statuses.map((item) => <option key={item}>{item}</option>)}</select>
                {formErrors.status ? <span className="field-error">{formErrors.status}</span> : null}
              </label>
              <label>Address
                <input value={form.address} onChange={(e) => setField('address', e.target.value)} />
                {formErrors.address ? <span className="field-error">{formErrors.address}</span> : null}
              </label>
            </div>
          </div>

          <details className="accordion-card">
            <summary>Additional Personal Information</summary>
            <div className="grid two-col-grid">
              <label>Middle name<input value={form.middleName} onChange={(e) => setField('middleName', e.target.value)} /></label>
              <label>Title
                <select value={form.title} onChange={(e) => setField('title', e.target.value)}>{titles.map((item) => <option key={item}>{item}</option>)}</select>
              </label>
            </div>
          </details>

          <details className="accordion-card">
            <summary>Contact and Emergency Details</summary>
            <div className="grid two-col-grid">
              <label>Alternative phone number<input value={form.alternativePhoneNumber} onChange={(e) => setField('alternativePhoneNumber', e.target.value)} /></label>
              <label>Emergency contact name<input value={form.emergencyContactName} onChange={(e) => setField('emergencyContactName', e.target.value)} /></label>
              <label>Emergency contact phone<input value={form.emergencyContactPhone} onChange={(e) => setField('emergencyContactPhone', e.target.value)} /></label>
              <label>Emergency relationship<input value={form.emergencyContactRelationship} onChange={(e) => setField('emergencyContactRelationship', e.target.value)} /></label>
            </div>
          </details>

          <details className="accordion-card">
            <summary>Professional Information</summary>
            <div className="grid two-col-grid">
              <label>Qualification<input value={form.qualification} onChange={(e) => setField('qualification', e.target.value)} /></label>
              <label>Highest education level<input value={form.highestEducationLevel} onChange={(e) => setField('highestEducationLevel', e.target.value)} /></label>
              <label>Years of experience<input type="number" min={0} value={form.yearsOfExperience} onChange={(e) => setField('yearsOfExperience', e.target.value)} /></label>
              <label>Staff role<input value={form.staffRole} onChange={(e) => setField('staffRole', e.target.value)} /></label>
              <label>Salary grade<input value={form.salaryGrade} onChange={(e) => setField('salaryGrade', e.target.value)} /></label>
            </div>
          </details>

          <details className="accordion-card">
            <summary>Notes and Profile</summary>
            <div className="grid two-col-grid">
              <label>Profile photo URL
                <input value={form.profilePhotoUrl} onChange={(e) => setField('profilePhotoUrl', e.target.value)} />
                {formErrors.profilePhotoUrl ? <span className="field-error">{formErrors.profilePhotoUrl}</span> : null}
              </label>
              <label>Password (for new account / reset)
                <input type="password" value={form.password} onChange={(e) => setField('password', e.target.value)} />
              </label>
              <label className="check-row"><input type="checkbox" checked={form.enabled} onChange={(e) => setField('enabled', e.target.checked)} /> Enabled account</label>
            </div>
            <label>Notes<textarea value={form.notes} onChange={(e) => setField('notes', e.target.value)} /></label>
          </details>

          <div className="toolbar-row">
            <button type="submit" disabled={saving || !canManage}>{saving ? 'Saving...' : editingId ? 'Update Teacher' : 'Save Teacher'}</button>
            {editingId ? <button type="button" className="text-button" onClick={resetForm}>Cancel edit</button> : null}
          </div>
        </form>
      </div>

      <div className="card" style={{ marginTop: 12 }}>
        <h3>Teachers</h3>
        <form className="toolbar-row" onSubmit={(e) => { e.preventDefault(); void loadTeachers(); }}>
          <input placeholder="Search by employee number or teacher name" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
          <select value={departmentFilter} onChange={(e) => setDepartmentFilter(e.target.value)}>
            <option value="">All departments</option>
            {departments.map((item) => <option key={item}>{item}</option>)}
          </select>
          <select value={subjectFilter} onChange={(e) => setSubjectFilter(e.target.value)}>
            <option value="">All subjects</option>
            {subjects.map((item) => <option key={item.id} value={item.name}>{item.name}</option>)}
          </select>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All statuses</option>
            {statuses.map((item) => <option key={item}>{item}</option>)}
          </select>
          <button type="submit">Search</button>
        </form>

        {loading ? <LoadingState title="Loading teachers..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={() => void loadTeachers()} /> : null}
        {!loading && !error && filteredRows.length === 0 ? <EmptyState title="No teachers found" message="Adjust filters or add a teacher record." /> : null}

        {!loading && !error && filteredRows.length > 0 ? (
          <table className="table">
            <thead>
              <tr>
                <th>Employee #</th>
                <th>Full name</th>
                <th>Department</th>
                <th>Specialization / Subject</th>
                <th>Phone</th>
                <th>Email</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredRows.map((teacher) => (
                <tr key={teacher.id}>
                  <td>{teacher.employeeNumber}</td>
                  <td>{teacher.fullName || `${teacher.firstName} ${teacher.lastName}`}</td>
                  <td>{teacher.department}</td>
                  <td>{teacher.specialization}</td>
                  <td>{teacher.phoneNumber || '-'}</td>
                  <td>{teacher.email}</td>
                  <td><span className={teacher.enabled ? 'badge active' : 'badge'}>{teacher.status}</span></td>
                  <td>
                    <div className="toolbar-row">
                      <button type="button" onClick={() => fillForm(teacher)}>Edit</button>
                      <button type="button" onClick={() => void onDelete(teacher)}>{teacher.enabled ? 'Deactivate' : 'Deactivate Again'}</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </div>
  );
}
