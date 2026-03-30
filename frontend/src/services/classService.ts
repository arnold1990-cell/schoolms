import api from './api';
import { unwrapItem, unwrapList } from '../utils/apiHelpers';

export type ClassStatus = 'ACTIVE' | 'INACTIVE';

export interface SchoolClassSummary {
  id: number;
  name: string;
  code: string;
  level?: string;
  academicYear?: string;
  stream?: string;
  capacity?: number;
  status: ClassStatus;
  classTeacherId?: number;
  classTeacherName?: string;
  learnerCount: number;
  subjectCount: number;
}

export interface ClassRoster {
  id: number;
  name: string;
  code: string;
  level?: string;
  academicYear?: string;
  stream?: string;
  capacity?: number;
  status: ClassStatus;
  classTeacher?: { id: number; employeeNumber?: string; fullName?: string };
  learners: Array<{ id: number; admissionNumber: string; fullName: string; status: string }>;
  subjects: Array<{ id: number; code: string; name: string }>;
  learnerCount: number;
  capacityUsagePercent: number;
}

export interface ClassUpsertPayload {
  gradeLevel: string;
  streamSection: string;
  academicYear: string;
  capacity?: number | null;
  status?: ClassStatus;
}

export async function listClasses(options?: { includeInactive?: boolean }) {
  const response = await api.get('/api/classes', {
    params: options?.includeInactive ? { includeInactive: true } : undefined,
  });
  return unwrapList<SchoolClassSummary>(response.data);
}

export async function getClassById(id: number) {
  const response = await api.get(`/api/classes/${id}`);
  return unwrapItem<ClassRoster>(response.data);
}

export async function createClass(payload: ClassUpsertPayload) {
  const response = await api.post('/api/classes', payload);
  return unwrapItem<SchoolClassSummary>(response.data);
}

export async function updateClass(id: number, payload: ClassUpsertPayload) {
  const response = await api.put(`/api/classes/${id}`, payload);
  return unwrapItem<SchoolClassSummary>(response.data);
}

export async function deleteClass(id: number) {
  await api.delete(`/api/classes/${id}`);
}

export async function assignClassTeacher(classId: number, teacherId: number | null) {
  const response = await api.put(`/api/classes/${classId}/teacher`, { teacherId });
  return unwrapItem<ClassRoster>(response.data);
}

export async function addLearnerToClass(classId: number, learnerId: number) {
  const response = await api.post(`/api/classes/${classId}/learners/${learnerId}`);
  return unwrapItem<ClassRoster>(response.data);
}

export async function removeLearnerFromClass(classId: number, learnerId: number) {
  const response = await api.delete(`/api/classes/${classId}/learners/${learnerId}`);
  return unwrapItem<ClassRoster>(response.data);
}

export async function transferLearner(classId: number, learnerId: number, targetClassId: number) {
  const response = await api.put(`/api/classes/${classId}/learners/${learnerId}/transfer/${targetClassId}`);
  return unwrapItem<ClassRoster>(response.data);
}

export async function addSubjectToClass(classId: number, subjectId: number) {
  const response = await api.post(`/api/classes/${classId}/subjects/${subjectId}`);
  return unwrapItem<ClassRoster>(response.data);
}

export async function removeSubjectFromClass(classId: number, subjectId: number) {
  const response = await api.delete(`/api/classes/${classId}/subjects/${subjectId}`);
  return unwrapItem<ClassRoster>(response.data);
}
