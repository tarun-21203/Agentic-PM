import { getAuthToken } from './authService';
import { publishGlobalError } from '../utils/globalMessages';

const API_BASE = (import.meta as any).env?.VITE_API_BASE ?? '';

const DEFAULT_USER_ID = 'default-user';

function getUserId(): string {
  try {
    return localStorage.getItem('agenticPmUserId') || DEFAULT_USER_ID;
  } catch {
    return DEFAULT_USER_ID;
  }
}

export function setUserId(userId: string) {
  localStorage.setItem('agenticPmUserId', userId);
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const url = path.startsWith('http') ? path : `${API_BASE}${path}`;
  const token = getAuthToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    'X-User-Id': getUserId(),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers as Record<string, string>),
  };
  const res = await fetch(url, { ...options, headers });
  if (!res.ok) {
    const body = await res.text();
    let message = body;
    try {
      const j = JSON.parse(body);
      if (j.message) message = j.message;
    } catch {
      // use body as message
    }
    const finalMessage =
      message || (res.status === 401 || res.status === 403 ? 'Your session is invalid or expired.' : `HTTP ${res.status}`);
    publishGlobalError(finalMessage);
    throw new Error(finalMessage);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export function getJson<T>(path: string): Promise<T> {
  return apiRequest<T>(path, { method: 'GET' });
}

export function postJson<T, B = unknown>(path: string, body: B): Promise<T> {
  return apiRequest<T>(path, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function putJson<T, B = unknown>(path: string, body: B): Promise<T> {
  return apiRequest<T>(path, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}
