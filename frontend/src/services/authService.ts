import api from './api';
import { ApiResponse, LoginResponse, MeResponse } from '../types';
import { unwrapItem } from '../utils/apiHelpers';

function asNonEmptyString(value: unknown): string | null {
  return typeof value === 'string' && value.trim().length > 0 ? value : null;
}

function normalizeLoginPayload(payload: unknown): LoginResponse {
  const candidate = unwrapItem<Partial<LoginResponse>>(payload);
  const accessToken = asNonEmptyString(candidate?.accessToken);
  const email = asNonEmptyString(candidate?.email);
  const role = candidate?.role;

  if (!accessToken || !email || (role !== 'ADMIN' && role !== 'TEACHER')) {
    throw new Error('Malformed login response');
  }

  return { accessToken, email, role };
}

function normalizeMePayload(payload: unknown): MeResponse {
  const candidate = unwrapItem<Partial<MeResponse>>(payload);
  if (!candidate || typeof candidate.id !== 'number') {
    throw new Error('Malformed me response');
  }
  const email = asNonEmptyString(candidate.email);
  const role = candidate.role;
  if (!email || (role !== 'ADMIN' && role !== 'TEACHER')) {
    throw new Error('Malformed me response');
  }
  return { id: candidate.id, email, role };
}

export const authService = {
  async login(email: string, password: string): Promise<LoginResponse> {
    const payload = { email: email.trim(), password };
    const { data } = await api.post<ApiResponse<LoginResponse>>('/api/auth/login', payload);
    return normalizeLoginPayload(data);
  },
  async me(): Promise<MeResponse> {
    const { data } = await api.get<ApiResponse<MeResponse>>('/api/auth/me');
    return normalizeMePayload(data);
  }
};
