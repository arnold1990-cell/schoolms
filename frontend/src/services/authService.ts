import api from './api';
import { ApiResponse, LoginResponse, MeResponse } from '../types';

export const authService = {
  async login(email: string, password: string): Promise<LoginResponse> {
    const payload = { email: email.trim(), password };
    const { data } = await api.post<ApiResponse<LoginResponse>>('/api/auth/login', payload);
    if (!data?.data) {
      throw new Error('Malformed login response');
    }
    return data.data;
  },
  async me(): Promise<MeResponse> {
    const { data } = await api.get<ApiResponse<MeResponse>>('/api/auth/me');
    if (!data?.data) {
      throw new Error('Malformed me response');
    }
    return data.data;
  }
};
