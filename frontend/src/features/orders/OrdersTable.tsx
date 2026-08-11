import type { OrderSummaryDto } from '../../shared/api/ordersApi';
import { StatusBadge } from '../../shared/components/StatusBadge';
import { formatDate, formatMoney } from '../../shared/utils/formatters';

interface OrdersTableProps {
  orders: Array<OrderSummaryDto & { itemsCount: number; openQuestionsCount: number }>;
  selectedOrderId: number | null;
  onSelectOrder: (orderId: number) => void;
}

export function OrdersTable({ orders, selectedOrderId, onSelectOrder }: OrdersTableProps) {
  return (
    <div className="orders-list">
      {orders.map((order) => (
        <button
          className={`order-row ${selectedOrderId === order.id ? 'order-row-active' : ''}`}
          key={order.id}
          onClick={() => onSelectOrder(order.id)}
          type="button"
        >
          <span className="order-id">#{order.id}</span>
          <span>
            <strong>{order.buyer.name}</strong>
            <small>{formatDate(order.placedAt)}</small>
          </span>
          <StatusBadge value={order.status} />
          <span>{formatMoney(order.totalAmount)}</span>
          <span>{order.itemsCount} items</span>
          <span>{order.openQuestionsCount} open questions</span>
        </button>
      ))}
    </div>
  );
}
