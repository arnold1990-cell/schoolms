import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { AxiosError } from 'axios';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { apiErrorMessage, unwrapItem, unwrapList } from '../utils/apiHelpers';
import { useAuth } from '../hooks/useAuth';

interface Student {
  id: number;
  admissionNumber: string;
  firstName: string;
  middleName?: string;
  lastName: string;
  fullName: string;
  gender: string;
  grade: string;
  classId: number;
  className: string;
  classStream?: string;
  enrollmentDate: string;
  guardianName: string;
  guardianRelationship: string;
  guardianPhone: string;
  address: string;
  status: StudentStatus;
  dateOfBirth: string;
  [key: string]: unknown;
}

interface SchoolClass {
  id: number;
  name: string;
  stream?: string;
}

type StudentStatus = 'ACTIVE' | 'PENDING' | 'SUSPENDED' | 'TRANSFERRED' | 'GRADUATED';

const statuses: StudentStatus[] = ['ACTIVE', 'PENDING', 'SUSPENDED', 'TRANSFERRED', 'GRADUATED'];
const genders = ['MALE', 'FEMALE', 'OTHER'];
const guardianRelationships = ['MOTHER', 'FATHER', 'GUARDIAN', 'AUNT', 'UNCLE', 'SIBLING', 'OTHER'];
const residencyTypes = ['DAY_SCHOLAR', 'BOARDING', 'HOSTEL'];
const sponsorshipStatuses = ['SELF', 'SPONSORED', 'PARTIAL'];
const feeCategories = ['REGULAR', 'SCHOLARSHIP', 'SUBSIDIZED'];

const blankForm = {
  admissionNumber: '',
  firstName: '',
  middleName: '',
  lastName: '',
  preferredName: '',
  gender: 'MALE',
  dateOfBirth: '',
  grade: '',
  classId: '',
  enrollmentDate: '',
  guardianName: '',
  guardianRelationship: 'MOTHER',
  guardianPhone: '',
  address: '',
  status: 'ACTIVE' as StudentStatus,
  nationality: '',
  nationalId: '',
  passportNumber: '',
  previousSchool: '',
  phoneNumber: '',
  alternativePhoneNumber: '',
  email: '',
  addressLine1: '',
  addressLine2: '',
  city: '',
  district: '',
  postalCode: '',
  country: '',
  guardianAltPhone: '',
  guardianEmail: '',
  guardianOccupation: '',
  guardianAddress: '',
  emergencyContactName: '',
  emergencyContactPhone: '',
  emergencyContactRelationship: '',
  bloodGroup: '',
  allergies: '',
  medicalConditions: '',
  disabilities: '',
  medication: '',
  hospitalName: '',
  doctorName: '',
  doctorPhone: '',
  usesTransport: false,
  pickupPoint: '',
  routeName: '',
  driverAssignment: '',
  religion: '',
  homeLanguage: '',
  residencyType: '',
  sponsorshipStatus: '',
  feeCategory: '',
  notes: '',
};

type StudentForm = typeof blankForm;

interface ApiErrorData {
  message?: string;
  data?: Record<string, string>;
}

export function StudentsPage() {
  const { user, authReady } = useAuth();
  const canManage = useMemo(() => user?.role === 'ADMIN', [user?.role]);

  const [rows, setRows] = useState<Student[]>([]);
  const [classes, setClasses] = useState<SchoolClass[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const [form, setForm] = useState<StudentForm>(blankForm);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [editingId, setEditingId] = useState<number | null>(null);

  const [keyword, setKeyword] = useState('');
  const [gradeFilter, setGradeFilter] = useState('');
  const [classFilter, setClassFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const classOptions = useMemo(() => classes.map((item) => ({
    ...item,
    label: `${item.name}${item.stream ? ` ${item.stream}` : ''}`,
  })), [classes]);

  const grades = useMemo(
    () => Array.from(new Set(classes.map((item) => item.name))).sort((a, b) => a.localeCompare(b)),
    [classes]
  );

  const classOptionsForGrade = useMemo(() => {
    if (!form.grade) {
      return classOptions;
    }
    return classOptions.filter((item) => item.name === form.grade);
  }, [classOptions, form.grade]);

  const loadClasses = useCallback(async () => {
    const response = await api.get('/api/classes');
    const rowsData = unwrapList<SchoolClass>(response.data);
    setClasses(rowsData);
    if (rowsData.length > 0) {
      setForm((prev) => ({
        ...prev,
        grade: prev.grade || rowsData[0].name,
        classId: prev.classId || String(rowsData[0].id),
      }));
    }
  }, []);

  const loadStudents = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params: Record<string, string | number> = {};
      if (keyword.trim()) params.keyword = keyword.trim();
      if (gradeFilter) params.grade = gradeFilter;
      if (classFilter) params.classId = Number(classFilter);
      if (statusFilter) params.status = statusFilter;
      const response = await api.get('/api/students', { params });
      setRows(unwrapList<Student>(response.data));
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load students.'));
    } finally {
      setLoading(false);
    }
  }, [classFilter, gradeFilter, keyword, statusFilter]);

  useEffect(() => {
    if (!authReady || !user) return;
    void Promise.all([loadClasses(), loadStudents()]);
  }, [authReady, loadClasses, loadStudents, user]);

  const setField = (field: keyof StudentForm, value: string | boolean) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setFormErrors((prev) => ({ ...prev, [field]: '' }));
  };

  const validate = () => {
    const next: Record<string, string> = {};
    const requiredFields: Array<[keyof StudentForm, string]> = [
      ['admissionNumber', 'Admission number is required.'],
      ['firstName', 'First name is required.'],
      ['lastName', 'Last name is required.'],
      ['gender', 'Gender is required.'],
      ['dateOfBirth', 'Date of birth is required.'],
      ['grade', 'Grade is required.'],
      ['classId', 'Class is required.'],
      ['enrollmentDate', 'Enrollment date is required.'],
      ['guardianName', 'Guardian name is required.'],
      ['guardianRelationship', 'Guardian relationship is required.'],
      ['guardianPhone', 'Guardian phone is required.'],
      ['address', 'Address is required.'],
      ['status', 'Status is required.'],
    ];

    requiredFields.forEach(([field, message]) => {
      const value = String(form[field] ?? '').trim();
      if (!value) next[field] = message;
    });

    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      next.email = 'Email must be valid.';
    }

    if (form.guardianEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.guardianEmail)) {
      next.guardianEmail = 'Guardian email must be valid.';
    }

    const selectedClass = classes.find((item) => item.id === Number(form.classId));
    if (form.classId && !selectedClass) {
      next.classId = 'Selected class is invalid.';
    }
    if (form.grade && selectedClass && selectedClass.name !== form.grade) {
      next.classId = 'Selected class does not belong to the chosen grade.';
    }

    if (form.dateOfBirth) {
      const dob = new Date(`${form.dateOfBirth}T00:00:00`);
      const now = new Date();
      if (Number.isNaN(dob.getTime()) || dob >= now) {
        next.dateOfBirth = 'Date of birth must be in the past.';
      }
    }
    if (form.enrollmentDate) {
      const enrollment = new Date(`${form.enrollmentDate}T00:00:00`);
      const today = new Date();
      const todayOnly = new Date(today.getFullYear(), today.getMonth(), today.getDate());
      if (Number.isNaN(enrollment.getTime())) {
        next.enrollmentDate = 'Enrollment date is invalid.';
      } else if (enrollment > todayOnly) {
        next.enrollmentDate = 'Enrollment date cannot be in the future.';
      } else if (form.dateOfBirth) {
        const dob = new Date(`${form.dateOfBirth}T00:00:00`);
        if (!Number.isNaN(dob.getTime()) && enrollment < dob) {
          next.enrollmentDate = 'Enrollment date cannot be before date of birth.';
        }
      }
    }

    setFormErrors(next);
    return Object.keys(next).length === 0;
  };

  const resetForm = () => {
    setEditingId(null);
    const defaultClass = classes[0];
    setForm({
      ...blankForm,
      grade: defaultClass?.name || '',
      classId: defaultClass ? String(defaultClass.id) : '',
    });
    setFormErrors({});
  };

  const fillFormFromStudent = (student: Student) => {
    setEditingId(student.id);
    setForm((prev) => ({
      ...prev,
      ...blankForm,
      ...student,
      classId: String(student.classId || ''),
      dateOfBirth: student.dateOfBirth || '',
      enrollmentDate: student.enrollmentDate || '',
      status: (student.status || 'ACTIVE') as StudentStatus,
      usesTransport: Boolean(student.usesTransport),
    }));
    setFormErrors({});
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setFeedback('');
    setError('');

    if (!validate()) {
      return;
    }

    const payload = {
      ...form,
      admissionNumber: form.admissionNumber.trim(),
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      guardianName: form.guardianName.trim(),
      guardianRelationship: form.guardianRelationship.trim(),
      guardianPhone: form.guardianPhone.trim(),
      address: form.address.trim(),
      grade: form.grade.trim(),
      classId: Number(form.classId),
      addressLine1: form.addressLine1 || form.address,
      email: form.email || null,
      guardianEmail: form.guardianEmail || null,
    };
    if (import.meta.env.DEV) {
      console.debug('[Learner registration] request payload', payload);
    }

    try {
      setSaving(true);
      if (editingId) {
        await api.put(`/api/students/${editingId}`, payload);
        setFeedback('Learner updated successfully.');
      } else {
        await api.post('/api/students', payload);
        setFeedback('Learner registered successfully.');
      }
      resetForm();
      await loadStudents();
    } catch (err) {
      const axiosError = err as AxiosError<ApiErrorData>;
      const backendErrors = axiosError.response?.data?.data;
      if (backendErrors && typeof backendErrors === 'object') {
        setFormErrors(backendErrors);
      }
      setError(apiErrorMessage(err, 'Failed to save learner.'));
    } finally {
      setSaving(false);
    }
  };

  const search = async (event: FormEvent) => {
    event.preventDefault();
    await loadStudents();
  };

  const clearFilters = async () => {
    setKeyword('');
    setGradeFilter('');
    setClassFilter('');
    setStatusFilter('');
    setTimeout(() => {
      void loadStudents();
    }, 0);
  };

  if (!authReady) {
    return <div className="page"><LoadingState title="Restoring session..." /></div>;
  }

  return (
    <div className="page">
      <PageHeader
        title="Learner Registration"
        subtitle="Register learners with required information first, then capture more details as needed."
      />

      {feedback ? <p className="success-text">{feedback}</p> : null}
      {error ? <p className="error-text">{error}</p> : null}

      <div className="card">
        <h3>{editingId ? 'Update Learner' : 'Register Learner'}</h3>
        <form className="form-grid learner-form" onSubmit={submit}>
          <div className="form-section">
            <h4>Required learner details</h4>
            <div className="grid two-col-grid">
              <label>Admission number
                <input value={form.admissionNumber} onChange={(e) => setField('admissionNumber', e.target.value)} />
                {formErrors.admissionNumber ? <span className="field-error">{formErrors.admissionNumber}</span> : null}
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
                {formErrors.dateOfBirth ? <span className="field-error">{formErrors.dateOfBirth}</span> : null}
              </label>
              <label>Grade
                <select value={form.grade} onChange={(e) => {
                  setField('grade', e.target.value);
                  const firstForGrade = classOptions.find((cls) => cls.name === e.target.value);
                  setField('classId', firstForGrade ? String(firstForGrade.id) : '');
                }}>
                  {grades.map((item) => <option key={item}>{item}</option>)}
                </select>
                {formErrors.grade ? <span className="field-error">{formErrors.grade}</span> : null}
              </label>
              <label>Class
                <select value={form.classId} onChange={(e) => setField('classId', e.target.value)}>
                  <option value="">Select class</option>
                  {classOptionsForGrade.map((item) => <option key={item.id} value={item.id}>{item.label}</option>)}
                </select>
                {formErrors.classId ? <span className="field-error">{formErrors.classId}</span> : null}
              </label>
              <label>Enrollment date
                <input type="date" value={form.enrollmentDate} onChange={(e) => setField('enrollmentDate', e.target.value)} />
                {formErrors.enrollmentDate ? <span className="field-error">{formErrors.enrollmentDate}</span> : null}
              </label>
              <label>Guardian name
                <input value={form.guardianName} onChange={(e) => setField('guardianName', e.target.value)} />
                {formErrors.guardianName ? <span className="field-error">{formErrors.guardianName}</span> : null}
              </label>
              <label>Guardian relationship
                <select value={form.guardianRelationship} onChange={(e) => setField('guardianRelationship', e.target.value)}>
                  {guardianRelationships.map((item) => <option key={item}>{item}</option>)}
                </select>
                {formErrors.guardianRelationship ? <span className="field-error">{formErrors.guardianRelationship}</span> : null}
              </label>
              <label>Guardian phone
                <input value={form.guardianPhone} onChange={(e) => setField('guardianPhone', e.target.value)} />
                {formErrors.guardianPhone ? <span className="field-error">{formErrors.guardianPhone}</span> : null}
              </label>
              <label>Address
                <input value={form.address} onChange={(e) => setField('address', e.target.value)} />
                {formErrors.address ? <span className="field-error">{formErrors.address}</span> : null}
              </label>
              <label>Status
                <select value={form.status} onChange={(e) => setField('status', e.target.value)}>{statuses.map((item) => <option key={item}>{item}</option>)}</select>
                {formErrors.status ? <span className="field-error">{formErrors.status}</span> : null}
              </label>
            </div>
          </div>

          <details className="accordion-card">
            <summary>Additional Personal Information</summary>
            <div className="grid two-col-grid">
              <label>Middle name<input value={form.middleName} onChange={(e) => setField('middleName', e.target.value)} /></label>
              <label>Preferred name<input value={form.preferredName} onChange={(e) => setField('preferredName', e.target.value)} /></label>
              <label>Nationality<input value={form.nationality} onChange={(e) => setField('nationality', e.target.value)} /></label>
              <label>National ID<input value={form.nationalId} onChange={(e) => setField('nationalId', e.target.value)} /></label>
              <label>Passport number<input value={form.passportNumber} onChange={(e) => setField('passportNumber', e.target.value)} /></label>
              <label>Previous school<input value={form.previousSchool} onChange={(e) => setField('previousSchool', e.target.value)} /></label>
            </div>
          </details>

          <details className="accordion-card">
            <summary>Contact Information</summary>
            <div className="grid two-col-grid">
              <label>Phone number<input value={form.phoneNumber} onChange={(e) => setField('phoneNumber', e.target.value)} /></label>
              <label>Alternative phone<input value={form.alternativePhoneNumber} onChange={(e) => setField('alternativePhoneNumber', e.target.value)} /></label>
              <label>Email<input value={form.email} onChange={(e) => setField('email', e.target.value)} />{formErrors.email ? <span className="field-error">{formErrors.email}</span> : null}</label>
              <label>Address line 1<input value={form.addressLine1} onChange={(e) => setField('addressLine1', e.target.value)} /></label>
              <label>Address line 2<input value={form.addressLine2} onChange={(e) => setField('addressLine2', e.target.value)} /></label>
              <label>City<input value={form.city} onChange={(e) => setField('city', e.target.value)} /></label>
              <label>District<input value={form.district} onChange={(e) => setField('district', e.target.value)} /></label>
              <label>Postal code<input value={form.postalCode} onChange={(e) => setField('postalCode', e.target.value)} /></label>
              <label>Country<input value={form.country} onChange={(e) => setField('country', e.target.value)} /></label>
            </div>
          </details>

          <details className="accordion-card">
            <summary>Guardian and Emergency Details</summary>
            <div className="grid two-col-grid">
              <label>Guardian alt phone<input value={form.guardianAltPhone} onChange={(e) => setField('guardianAltPhone', e.target.value)} /></label>
              <label>Guardian email<input value={form.guardianEmail} onChange={(e) => setField('guardianEmail', e.target.value)} /></label>
              {formErrors.guardianEmail ? <span className="field-error">{formErrors.guardianEmail}</span> : null}
              <label>Guardian occupation<input value={form.guardianOccupation} onChange={(e) => setField('guardianOccupation', e.target.value)} /></label>
              <label>Guardian address<input value={form.guardianAddress} onChange={(e) => setField('guardianAddress', e.target.value)} /></label>
              <label>Emergency contact name<input value={form.emergencyContactName} onChange={(e) => setField('emergencyContactName', e.target.value)} /></label>
              <label>Emergency contact phone<input value={form.emergencyContactPhone} onChange={(e) => setField('emergencyContactPhone', e.target.value)} /></label>
              <label>Emergency contact relationship<input value={form.emergencyContactRelationship} onChange={(e) => setField('emergencyContactRelationship', e.target.value)} /></label>
            </div>
          </details>

          <details className="accordion-card">
            <summary>Health Information</summary>
            <div className="grid two-col-grid">
              <label>Blood group<input value={form.bloodGroup} onChange={(e) => setField('bloodGroup', e.target.value)} /></label>
              <label>Allergies<input value={form.allergies} onChange={(e) => setField('allergies', e.target.value)} /></label>
              <label>Medical conditions<input value={form.medicalConditions} onChange={(e) => setField('medicalConditions', e.target.value)} /></label>
              <label>Disabilities<input value={form.disabilities} onChange={(e) => setField('disabilities', e.target.value)} /></label>
              <label>Medication<input value={form.medication} onChange={(e) => setField('medication', e.target.value)} /></label>
              <label>Hospital name<input value={form.hospitalName} onChange={(e) => setField('hospitalName', e.target.value)} /></label>
              <label>Doctor name<input value={form.doctorName} onChange={(e) => setField('doctorName', e.target.value)} /></label>
              <label>Doctor phone<input value={form.doctorPhone} onChange={(e) => setField('doctorPhone', e.target.value)} /></label>
            </div>
          </details>

          <details className="accordion-card">
            <summary>Transport Information</summary>
            <div className="grid two-col-grid">
              <label className="check-row">Uses transport
                <input type="checkbox" checked={form.usesTransport} onChange={(e) => setField('usesTransport', e.target.checked)} />
              </label>
              <label>Pickup point<input value={form.pickupPoint} onChange={(e) => setField('pickupPoint', e.target.value)} /></label>
              <label>Route name<input value={form.routeName} onChange={(e) => setField('routeName', e.target.value)} /></label>
              <label>Driver assignment<input value={form.driverAssignment} onChange={(e) => setField('driverAssignment', e.target.value)} /></label>
            </div>
          </details>

          <details className="accordion-card">
            <summary>Other School Information</summary>
            <div className="grid two-col-grid">
              <label>Religion<input value={form.religion} onChange={(e) => setField('religion', e.target.value)} /></label>
              <label>Home language<input value={form.homeLanguage} onChange={(e) => setField('homeLanguage', e.target.value)} /></label>
              <label>Residency type<select value={form.residencyType} onChange={(e) => setField('residencyType', e.target.value)}><option value="">Select</option>{residencyTypes.map((item) => <option key={item}>{item}</option>)}</select></label>
              <label>Sponsorship status<select value={form.sponsorshipStatus} onChange={(e) => setField('sponsorshipStatus', e.target.value)}><option value="">Select</option>{sponsorshipStatuses.map((item) => <option key={item}>{item}</option>)}</select></label>
              <label>Fee category<select value={form.feeCategory} onChange={(e) => setField('feeCategory', e.target.value)}><option value="">Select</option>{feeCategories.map((item) => <option key={item}>{item}</option>)}</select></label>
              <label>Notes<textarea value={form.notes} onChange={(e) => setField('notes', e.target.value)} /></label>
            </div>
          </details>

          <div className="toolbar-row">
            <button type="submit" disabled={!canManage || saving}>{saving ? 'Saving...' : editingId ? 'Update Learner' : 'Register Learner'}</button>
            <button type="button" className="text-button" onClick={resetForm} disabled={saving}>Reset</button>
          </div>
        </form>
      </div>

      <div className="card">
        <h3>Learners</h3>
        <form className="toolbar-row" onSubmit={search}>
          <input placeholder="Search admission #, learner name, grade, class, status" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
          <select value={gradeFilter} onChange={(e) => setGradeFilter(e.target.value)}>
            <option value="">All grades</option>
            {grades.map((item) => <option key={item}>{item}</option>)}
          </select>
          <select value={classFilter} onChange={(e) => setClassFilter(e.target.value)}>
            <option value="">All classes</option>
            {classOptions.map((item) => <option key={item.id} value={item.id}>{item.label}</option>)}
          </select>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All status</option>
            {statuses.map((item) => <option key={item}>{item}</option>)}
          </select>
          <button type="submit">Search</button>
          <button type="button" className="text-button" onClick={clearFilters}>Clear</button>
        </form>

        {loading ? <LoadingState title="Loading learners..." /> : null}
        {!loading && error ? <ErrorState message={error} onRetry={() => void loadStudents()} /> : null}
        {!loading && !error && rows.length === 0 ? <EmptyState title="No learners found" message="Register learners to begin attendance and assessment workflows." /> : null}

        {!loading && !error && rows.length > 0 ? (
          <table className="table">
            <thead>
              <tr>
                <th>Admission #</th>
                <th>Full name</th>
                <th>Gender</th>
                <th>Grade</th>
                <th>Class</th>
                <th>Guardian name</th>
                <th>Guardian phone</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((student) => (
                <tr key={student.id}>
                  <td>{student.admissionNumber}</td>
                  <td>{student.fullName}</td>
                  <td>{student.gender}</td>
                  <td>{student.grade || '-'}</td>
                  <td>{student.className ? `${student.className}${student.classStream ? ` ${student.classStream}` : ''}` : '-'}</td>
                  <td>{student.guardianName || '-'}</td>
                  <td>{student.guardianPhone || '-'}</td>
                  <td>{student.status || '-'}</td>
                  <td><button type="button" disabled={!canManage} onClick={async () => {
                    const response = await api.get(`/api/students/${student.id}`);
                    const details = unwrapItem<Student>(response.data);
                    if (details) fillFormFromStudent(details);
                  }}>Edit</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </div>
    </div>
  );
}
