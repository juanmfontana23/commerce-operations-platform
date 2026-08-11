import { apiRequest } from './httpClient';

export type QuestionStatus = 'OPEN' | 'ANSWERED' | 'RESOLVED';
export type QuestionPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface PriorityDto {
  score: number;
  priority: QuestionPriority;
  reasons: string[];
}

export interface QuestionDto {
  id: number;
  orderId: number;
  buyerId: number;
  buyerName: string;
  productId: number | null;
  productTitle: string | null;
  message: string;
  answer: string | null;
  status: QuestionStatus;
  priority: PriorityDto;
  createdAt: string;
  answeredAt: string | null;
  resolvedAt: string | null;
}

export function getOrderQuestions(orderId: number): Promise<QuestionDto[]> {
  return apiRequest<QuestionDto[]>(`/api/orders/${orderId}/questions`);
}

export function getSellerUnresolvedQuestions(sellerId: number): Promise<QuestionDto[]> {
  return apiRequest<QuestionDto[]>(`/api/sellers/${sellerId}/questions/unresolved`);
}

export function answerQuestion(questionId: number, answer: string): Promise<QuestionDto> {
  return apiRequest<QuestionDto>(`/api/questions/${questionId}/answer`, {
    method: 'POST',
    body: JSON.stringify({ answer }),
  });
}

export function resolveQuestion(questionId: number): Promise<QuestionDto> {
  return apiRequest<QuestionDto>(`/api/questions/${questionId}/resolve`, {
    method: 'POST',
  });
}
