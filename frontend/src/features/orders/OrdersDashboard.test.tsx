import { render, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OrdersDashboard } from './OrdersDashboard';

const mocks = vi.hoisted(() => ({
  getSellerOrders: vi.fn(),
  getSellerUnresolvedQuestions: vi.fn(),
}));

vi.mock('../../shared/api/ordersApi', async () => {
  const actual = await vi.importActual<typeof import('../../shared/api/ordersApi')>('../../shared/api/ordersApi');
  return { ...actual, getSellerOrders: mocks.getSellerOrders };
});
vi.mock('../../shared/api/questionsApi', async () => {
  const actual = await vi.importActual<typeof import('../../shared/api/questionsApi')>('../../shared/api/questionsApi');
  return { ...actual, getSellerUnresolvedQuestions: mocks.getSellerUnresolvedQuestions };
});

beforeEach(() => {
  vi.clearAllMocks();
  mocks.getSellerOrders.mockResolvedValue([]);
  mocks.getSellerUnresolvedQuestions.mockResolvedValue([]);
});

describe('OrdersDashboard seller scope', () => {
  it.each([{ username: 'seller1', sellerId: 1 }, { username: 'seller2', sellerId: 2 }])(
    'uses the authenticated scope for $username', async ({ sellerId }) => {
      render(<OrdersDashboard sellerId={sellerId} onLogout={vi.fn()} />);

      await waitFor(() => expect(mocks.getSellerOrders).toHaveBeenCalledWith(sellerId, {}));
      expect(mocks.getSellerUnresolvedQuestions).toHaveBeenCalledWith(sellerId);
    },
  );
});
