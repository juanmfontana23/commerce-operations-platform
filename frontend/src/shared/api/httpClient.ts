import { basicAuthorization, getApiCredentials, type ApiCredentials } from './authSession';

export async function apiRequest<T>(path: string, options?: RequestInit): Promise<T> {
  const { headers: customHeaders, ...rest } = options ?? {};
  const credentials = getApiCredentials();
  if (!credentials) {
    throw new Error('Sign in with a seller account to continue');
  }
  const csrfToken = rest.method && rest.method !== 'GET' && rest.method !== 'HEAD' && rest.method !== 'OPTIONS'
    ? await requestCsrfToken(credentials)
    : readCookie('XSRF-TOKEN');
  const response = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      Authorization: basicAuthorization(credentials),
      ...(csrfToken ? { 'X-XSRF-TOKEN': csrfToken } : {}),
      ...customHeaders,
    },
    credentials: 'same-origin',
    ...rest,
  });

  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export function getSellerSession(): Promise<{ sellerId: number }> {
  return apiRequest<{ sellerId: number }>('/api/session');
}

export async function requestCsrfToken(credentials: ApiCredentials, reuseCookie = true): Promise<string> {
  const existingToken = readCookie('XSRF-TOKEN');
  if (reuseCookie && existingToken) {
    return existingToken;
  }
  const response = await fetch('/api/csrf-token', {
    headers: { Authorization: basicAuthorization(credentials) },
    credentials: 'same-origin',
  });
  if (!response.ok) {
    throw new Error(`CSRF token request failed with status ${response.status}`);
  }
  const body = (await response.json()) as { token: string };
  return body.token;
}

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find(cookie => cookie.startsWith(`${name}=`))
    ?.split('=')[1];
}

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string };
    return body.message ?? '';
  } catch {
    return '';
  }
}
