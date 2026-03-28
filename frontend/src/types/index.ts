export type Role = 'ADMIN' | 'TEACHER';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}

export interface MeResponse {
  id: number;
  email: string;
  role: Role;
}

export interface LoginResponse {
  accessToken: string;
  id: number | null;
  email: string;
  role: Role;
}
