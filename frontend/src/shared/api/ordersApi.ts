import { apiRequest } from './httpClient';

export type OrderStatus = 'CREATED' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface BuyerDto {
  id: number;
  name: string;
  email: string;
}

export interface OrderSummaryDto {
  id: number;
  status: OrderStatus;
  placedAt: string;
  buyer: BuyerDto;
  totalAmount: number;
}

export interface OrderItemDto {
  productId: number;
  productTitle: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface OrderDetailDto {
  id: number;
  sellerId: number;
  status: OrderStatus;
  placedAt: string;
  buyer: BuyerDto;
  items: OrderItemDto[];
  totalAmount: number;
}

export interface OrderFilters {
  status?: OrderStatus;
  buyer?: string;
  from?: string;
  to?: string;
}

export function getSellerOrders(sellerId: number, filters: OrderFilters): Promise<OrderSummaryDto[]> {
  const params = new URLSearchParams();

  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      params.set(key, value);
    }
  });

  const query = params.toString();
  return apiRequest<OrderSummaryDto[]>(`/api/sellers/${sellerId}/orders${query ? `?${query}` : ''}`);
}

export function getOrder(orderId: number): Promise<OrderDetailDto> {
  return apiRequest<OrderDetailDto>(`/api/orders/${orderId}`);
}
