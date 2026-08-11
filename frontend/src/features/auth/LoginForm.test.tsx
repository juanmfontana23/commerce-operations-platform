import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { LoginForm } from './LoginForm';
import { clearApiCredentials, getSellerId, setSellerId } from '../../shared/api/authSession';

const mocks = vi.hoisted(() => ({ requestCsrfToken: vi.fn(), getSellerSession: vi.fn() }));
vi.mock('../../shared/api/httpClient', () => mocks);

beforeEach(() => {
  clearApiCredentials();
  mocks.requestCsrfToken.mockReset();
  mocks.getSellerSession.mockReset();
});

describe('LoginForm', () => {
  it('authenticates with credentials entered at runtime', async () => {
    mocks.requestCsrfToken.mockResolvedValue('csrf-token');
    mocks.getSellerSession.mockResolvedValue({ sellerId: 2 });
    const onAuthenticated = vi.fn();
    render(<LoginForm onAuthenticated={onAuthenticated} />);

    fireEvent.change(screen.getByLabelText('Seller username'), { target: { value: 'runtime-user' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'runtime-password' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => expect(onAuthenticated).toHaveBeenCalledOnce());
    expect(mocks.requestCsrfToken).toHaveBeenCalledWith({ username: 'runtime-user', password: 'runtime-password' }, false);
    expect(mocks.getSellerSession).toHaveBeenCalledOnce();
  });

  it('does not activate a session when authentication fails', async () => {
    setSellerId(1);
    mocks.requestCsrfToken.mockRejectedValue(new Error('Unauthorized'));
    render(<LoginForm onAuthenticated={vi.fn()} />);

    fireEvent.change(screen.getByLabelText('Seller username'), { target: { value: 'runtime-user' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong-password' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to sign in');
    expect(mocks.getSellerSession).not.toHaveBeenCalled();
    expect(getSellerId()).toBeNull();
  });
});
