import axios from 'axios';

const resolvedBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || 'http://localhost:8080';

export const UNAUTHORIZED_EVENT = 'schoolms:unauthorized';

const api = axios.create({ baseURL: resolvedBaseUrl });

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
    if (status === 401) {
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
