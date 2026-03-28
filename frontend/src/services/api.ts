import axios from 'axios';

const resolvedBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || 'http://localhost:8080';

export const UNAUTHORIZED_EVENT = 'schoolms:unauthorized';

const api = axios.create({ baseURL: resolvedBaseUrl });

function forceLogoutAfterInvalidSession() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('authUser');
  window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error?.response?.status;
    const requestUrl = String(error?.config?.url ?? '').toLowerCase();
    const isAuthMeRequest = requestUrl.includes('/api/auth/me');
    const isAuthLoginRequest = requestUrl.includes('/api/auth/login');
    const hasToken = Boolean(localStorage.getItem('accessToken'));

    if (status === 401 && hasToken && isAuthMeRequest) {
      forceLogoutAfterInvalidSession();
    }

    if (status === 401 && !hasToken && !isAuthLoginRequest && import.meta.env.DEV) {
      console.warn('[API] Received 401 without an accessToken. Keeping auth state untouched for this request.', error?.config?.url);
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
