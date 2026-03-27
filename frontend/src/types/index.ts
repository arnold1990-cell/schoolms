export type Role = 'ADMIN' | 'TEACHER';

export interface MeResponse {
  id: number;
  email: string;
  role: Role;
}

export interface LoginResponse {
  accessToken: string;
  email: string;
  role: Role;
}
