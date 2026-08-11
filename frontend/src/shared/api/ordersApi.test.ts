import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getSellerOrders, getOrder } from './ordersApi';

vi.mock('./httpClient', () => ({
  apiRequest: vi.fn(),
}));

import { apiRequest } from './httpClient';

const mockedApiRequest = vi.mocked(apiRequest);

beforeEach(() => {
  vi.clearAllMocks();
});

describe('ordersApi', () => {
  describe('getSellerOrders', () => {
    it('calls the correct endpoint without query params when filters are empty', async () => {
      mockedApiRequest.mockResolvedValue([]);

      await getSellerOrders(42, {});

      expect(mockedApiRequest).toHaveBeenCalledWith('/api/sellers/42/orders');
    });

    it('appends non-empty filters as query params', async () => {
      mockedApiRequest.mockResolvedValue([]);

      await getSellerOrders(42, { status: 'PAID', buyer: 'John' });

      expect(mockedApiRequest).toHaveBeenCalledWith('/api/sellers/42/orders?status=PAID&buyer=John');
    });

    it('skips empty filter values', async () => {
      mockedApiRequest.mockResolvedValue([]);

      await getSellerOrders(1, { status: 'SHIPPED', buyer: '', from: undefined });

      expect(mockedApiRequest).toHaveBeenCalledWith('/api/sellers/1/orders?status=SHIPPED');
    });

    it('includes all filter params when fully populated', async () => {
      mockedApiRequest.mockResolvedValue([]);

      await getSellerOrders(7, { status: 'CREATED', buyer: 'Jane', from: '2025-01-01', to: '2025-12-31' });

      expect(mockedApiRequest).toHaveBeenCalledWith(
        '/api/sellers/7/orders?status=CREATED&buyer=Jane&from=2025-01-01&to=2025-12-31',
      );
    });
  });

  describe('getOrder', () => {
    it('calls the correct endpoint with the order id', async () => {
      mockedApiRequest.mockResolvedValue({ id: 99 });

      const result = await getOrder(99);

      expect(mockedApiRequest).toHaveBeenCalledWith('/api/orders/99');
      expect(result).toEqual({ id: 99 });
    });
  });
});
