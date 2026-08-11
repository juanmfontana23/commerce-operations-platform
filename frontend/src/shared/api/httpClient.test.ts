import { describe, it, expect, vi, beforeEach } from 'vitest';
import { apiRequest } from './httpClient';
import { basicAuthorization, clearApiCredentials, setApiCredentials } from './authSession';

beforeEach(() => {
  vi.restoreAllMocks();
  setApiCredentials({ username: 'runtime-user', password: 'runtime-password' });
  document.cookie = 'XSRF-TOKEN=; Max-Age=0';
});

describe('apiRequest', () => {
  it('makes a GET request with default JSON headers', async () => {
    const mockData = { hello: 'world' };
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(mockData), { status: 200, headers: { 'Content-Type': 'application/json' } }),
    );

    const result = await apiRequest<{ hello: string }>('/api/test');

    expect(fetchSpy).toHaveBeenCalledOnce();
    expect(fetchSpy.mock.calls[0][0]).toBe('/api/test');
    expect(fetchSpy.mock.calls[0][1]?.headers).toMatchObject({
      'Content-Type': 'application/json',
      Authorization: basicAuthorization({ username: 'runtime-user', password: 'runtime-password' }),
    });
    expect(result).toEqual(mockData);
  });

  it('builds correct URLs with query params', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response('[]', { status: 200, headers: { 'Content-Type': 'application/json' } }),
    );

    const params = new URLSearchParams({ status: 'PAID', buyer: 'John' });
    await apiRequest(`/api/sellers/1/orders?${params.toString()}`);

    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/sellers/1/orders?status=PAID&buyer=John',
      expect.anything(),
    );
  });

  it('merges custom headers with default headers', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response('null', { status: 200, headers: { 'Content-Type': 'application/json' } }),
    );

    await apiRequest('/api/test', { headers: { Authorization: 'Bearer token' } });

    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/test',
      {
        headers: { 'Content-Type': 'application/json', Authorization: 'Bearer token' },
        credentials: 'same-origin',
      },
    );
  });

  it('sends the cookie CSRF token with state-changing requests', async () => {
    document.cookie = 'XSRF-TOKEN=test-token';
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response('null', { status: 200, headers: { 'Content-Type': 'application/json' } }),
    );

    await apiRequest('/api/questions/1/resolve', { method: 'POST' });

    expect(fetchSpy.mock.calls[0][1]?.headers).toMatchObject({ 'X-XSRF-TOKEN': 'test-token' });
  });

  it('fails closed when no runtime credentials are active', async () => {
    clearApiCredentials();

    await expect(apiRequest('/api/test')).rejects.toThrow('Sign in');
  });

  it('throws on non-OK response with JSON error message', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ message: 'Not found' }), {
        status: 404,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    await expect(apiRequest('/api/missing')).rejects.toThrow('Not found');
  });

  it('throws generic message when response body has no message field', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ error: 'bad' }), {
        status: 500,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    await expect(apiRequest('/api/fail')).rejects.toThrow('Request failed with status 500');
  });

  it('throws generic message when response body is not JSON', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response('plain text error', { status: 502 }),
    );

    await expect(apiRequest('/api/gateway')).rejects.toThrow('Request failed with status 502');
  });
});
