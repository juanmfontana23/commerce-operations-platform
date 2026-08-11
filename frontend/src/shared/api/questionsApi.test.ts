import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getOrderQuestions, getSellerUnresolvedQuestions, answerQuestion, resolveQuestion } from './questionsApi';

vi.mock('./httpClient', () => ({
  apiRequest: vi.fn(),
}));

import { apiRequest } from './httpClient';

const mockedApiRequest = vi.mocked(apiRequest);

beforeEach(() => {
  vi.clearAllMocks();
});

describe('questionsApi', () => {
  describe('getOrderQuestions', () => {
    it('calls the correct endpoint for an order', async () => {
      mockedApiRequest.mockResolvedValue([]);

      await getOrderQuestions(10);

      expect(mockedApiRequest).toHaveBeenCalledWith('/api/orders/10/questions');
    });
  });

  describe('getSellerUnresolvedQuestions', () => {
    it('calls the correct endpoint for seller unresolved questions', async () => {
      mockedApiRequest.mockResolvedValue([]);

      await getSellerUnresolvedQuestions(5);

      expect(mockedApiRequest).toHaveBeenCalledWith('/api/sellers/5/questions/unresolved');
    });
  });

  describe('answerQuestion', () => {
    it('sends a POST with the answer payload', async () => {
      mockedApiRequest.mockResolvedValue({});

      await answerQuestion(3, 'Thanks for your patience');

      expect(mockedApiRequest).toHaveBeenCalledWith('/api/questions/3/answer', {
        method: 'POST',
        body: JSON.stringify({ answer: 'Thanks for your patience' }),
      });
    });
  });

  describe('resolveQuestion', () => {
    it('sends a POST to the resolve endpoint', async () => {
      mockedApiRequest.mockResolvedValue({});

      await resolveQuestion(7);

      expect(mockedApiRequest).toHaveBeenCalledWith('/api/questions/7/resolve', {
        method: 'POST',
      });
    });
  });
});
