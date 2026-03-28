import api from './api';
import { unwrapItem, unwrapList } from '../utils/apiHelpers';

export interface SubjectDto {
  id: number;
  code: string;
  name: string;
  assignedTeacher?: {
    id: number;
    firstName?: string;
    lastName?: string;
    staffCode?: string;
  };
}

export async function listSubjects(): Promise<SubjectDto[]> {
  const response = await api.get('/api/subjects');
  return unwrapList<SubjectDto>(response.data);
}

function requireSubject(payload: unknown): SubjectDto {
  const subject = unwrapItem<SubjectDto>(payload);
  if (!subject) {
    throw new Error('Malformed subject response');
  }
  return subject;
}

export async function createSubject(payload: { code: string; name: string }): Promise<SubjectDto> {
  const response = await api.post('/api/subjects', payload);
  return requireSubject(response.data);
}

export async function assignSubjectTeacher(subjectId: number, teacherId: number | null): Promise<SubjectDto> {
  const response = await api.post(`/api/subjects/${subjectId}/assign-teacher`, { teacherId });
  return requireSubject(response.data);
}
