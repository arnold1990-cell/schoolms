import axios from 'axios';

const resolvedBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || 'http://localhost:8080';

export const UNAUTHORIZED_EVENT = 'schoolms:unauthorized';

const api = axios.create({ baseURL: resolvedBaseUrl });

const authProbeClient = axios.create({ baseURL: resolvedBaseUrl });
let isSessionProbeInFlight = false;

function shouldSkipSessionProbe(configUrl: string): boolean {
  return configUrl.includes('/api/auth/login') || configUrl.includes('/api/auth/me');
}

async function forceLogoutAfterInvalidSession() {
  localStorage.removeItem('accessToken');
  window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
}

async function shouldForceLogoutFor401(error: unknown): Promise<boolean> {
  const status = (error as { response?: { status?: number } })?.response?.status;
  if (status !== 401) {
    return false;
  }

  const configUrl = ((error as { config?: { url?: string } })?.config?.url ?? '').toLowerCase();
  if (shouldSkipSessionProbe(configUrl)) {
    return !configUrl.includes('/api/auth/login');
  }

  const token = localStorage.getItem('accessToken');
  if (!token) {
    return true;
  }

  if (isSessionProbeInFlight) {
    return false;
  }

  isSessionProbeInFlight = true;
  try {
    await authProbeClient.get('/api/auth/me', {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    return false;
  } catch (probeError) {
    const probeStatus = (probeError as { response?: { status?: number } })?.response?.status;
    return probeStatus === 401;
  } finally {
    isSessionProbeInFlight = false;
  }
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
  async (error) => {
    const status = error?.response?.status;
    if (await shouldForceLogoutFor401(error)) {
      await forceLogoutAfterInvalidSession();
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
