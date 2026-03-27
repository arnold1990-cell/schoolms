import api from './api';
import { LoginResponse, MeResponse } from '../types';

export const authService = {
  async login(email: string, password: string): Promise<LoginResponse> {
    const { data } = await api.post('/api/auth/login', { email, password });
    return data.data;
  },
  async me(): Promise<MeResponse> {
    const { data } = await api.get('/api/auth/me');
    return data.data;
  }
};
