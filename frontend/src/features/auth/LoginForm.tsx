import type { FormEvent } from 'react';
import { useState } from 'react';
import { clearApiCredentials, setApiCredentials, setSellerId, type ApiCredentials } from '../../shared/api/authSession';
import { getSellerSession, requestCsrfToken } from '../../shared/api/httpClient';

interface LoginFormProps {
  onAuthenticated: () => void;
}

export function LoginForm({ onAuthenticated }: LoginFormProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const credentials: ApiCredentials = { username: username.trim(), password };
    if (!credentials.username || !credentials.password) {
      setError('Enter both your seller username and password.');
      return;
    }

    setLoading(true);
    setError(null);
    clearApiCredentials();
    try {
      await requestCsrfToken(credentials, false);
      setApiCredentials(credentials);
      const session = await getSellerSession();
      setSellerId(session.sellerId);
      onAuthenticated();
    } catch {
      clearApiCredentials();
      setError('Unable to sign in with those credentials.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-shell">
      <section className="login-card">
        <p className="eyebrow">Seller workspace</p>
        <h1>Sign in to Commerce Operations</h1>
        <p>Credentials are used only for this browser session and are never part of the frontend build.</p>
        <form onSubmit={handleSubmit}>
          <label>
            Seller username
            <input value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" />
          </label>
          {error && <div className="alert" role="alert">{error}</div>}
          <button type="submit" disabled={loading}>{loading ? 'Signing in...' : 'Sign in'}</button>
        </form>
      </section>
    </main>
  );
}
