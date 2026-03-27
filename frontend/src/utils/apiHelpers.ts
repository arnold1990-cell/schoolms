import { AxiosError } from 'axios';

export function unwrapList<T>(payload: unknown): T[] {
  if (Array.isArray(payload)) return payload as T[];
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return unwrapList<T>((payload as { data?: unknown }).data);
  }
  return [];
}

export function unwrapItem<T>(payload: unknown): T | null {
  if (payload && typeof payload === 'object' && 'data' in payload) {
    return unwrapItem<T>((payload as { data?: unknown }).data);
  }
  return (payload as T) ?? null;
}

export function apiErrorMessage(error: unknown, fallback: string): string {
  const axiosError = error as AxiosError<{ message?: string }>;
  return axiosError.response?.data?.message ?? axiosError.message ?? fallback;
}
