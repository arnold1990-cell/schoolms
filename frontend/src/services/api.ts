import axios from 'axios';

const resolvedBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || 'http://localhost:8080';

export const UNAUTHORIZED_EVENT = 'schoolms:unauthorized';

const api = axios.create({ baseURL: resolvedBaseUrl });

function shouldResetSessionOnUnauthorized(error: unknown): boolean {
  const status = (error as { response?: { status?: number } })?.response?.status;
  if (status !== 401) return false;

  const configUrl = ((error as { config?: { url?: string } })?.config?.url ?? '').toLowerCase();
  if (configUrl.includes('/api/auth/me')) {
    return true;
  }

  const responseData = (error as { response?: { data?: { message?: string; error?: string } } })?.response?.data;
  const authMessage = `${responseData?.message ?? ''} ${responseData?.error ?? ''}`.toLowerCase();
  return authMessage.includes('invalid token')
    || authMessage.includes('expired')
    || authMessage.includes('jwt')
    || authMessage.includes('unauthorized');
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    if (shouldResetSessionOnUnauthorized(error)) {
      localStorage.removeItem('accessToken');
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    if (status === 403 && import.meta.env.DEV) {
      console.warn('[API] Access denied for request', error?.config?.url);
    }
    if (import.meta.env.DEV) {
      console.error('[API] Request failed', {
        url: error?.config?.url,
        method: error?.config?.method,
        status,
        data: error?.response?.data,
      });
    }
    return Promise.reject(error);
  }
);

export default api;
