import axios from 'axios';

const resolvedBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || 'http://localhost:8080';
const ACCESS_TOKEN_KEY = 'accessToken';
const AUTH_USER_KEY = 'authUser';

export const UNAUTHORIZED_EVENT = 'schoolms:unauthorized';

const api = axios.create({ baseURL: resolvedBaseUrl });
let sessionValidationPromise: Promise<boolean> | null = null;

function forceLogoutAfterInvalidSession() {
  if (import.meta.env.DEV) {
    console.debug('[API] unauthorized event fired; removing accessToken and notifying auth context');
  }
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
  window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
}

async function validateActiveSession(token: string): Promise<boolean> {
  if (!sessionValidationPromise) {
    sessionValidationPromise = axios.get(`${resolvedBaseUrl}/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(() => true)
      .catch(() => false)
      .finally(() => {
        sessionValidationPromise = null;
      });
  }
  return sessionValidationPromise;
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
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
    const isAuthRequest = isAuthMeRequest || isAuthLoginRequest;
    const hasToken = Boolean(localStorage.getItem(ACCESS_TOKEN_KEY));
    if (status === 401 && hasToken && !isAuthRequest) {
      const token = localStorage.getItem(ACCESS_TOKEN_KEY);
      if (token) {
        const hasValidSession = await validateActiveSession(token);
        if (!hasValidSession) {
          forceLogoutAfterInvalidSession();
        }
      }
    }

    if (status === 401 && import.meta.env.DEV) {
      console.warn('[API] 401 response received', {
        url: error?.config?.url,
        hasToken,
        isAuthRequest,
      });
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
