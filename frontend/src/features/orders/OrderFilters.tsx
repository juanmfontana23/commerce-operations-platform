import type { OrderFilters as OrderFiltersType, OrderStatus } from '../../shared/api/ordersApi';

const orderStatuses: Array<OrderStatus | ''> = ['', 'CREATED', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

interface OrderFiltersProps {
  filters: OrderFiltersType;
  onChange: (name: keyof OrderFiltersType, value: string) => void;
}

export function OrderFilters({ filters, onChange }: OrderFiltersProps) {
  return (
    <div className="filters" aria-label="Order filters">
      <label>
        Status
        <select value={filters.status ?? ''} onChange={(event) => onChange('status', event.target.value)}>
          {orderStatuses.map((status) => (
            <option key={status || 'all'} value={status}>
              {status || 'All statuses'}
            </option>
          ))}
        </select>
      </label>
      <label>
        Buyer
        <input value={filters.buyer ?? ''} onChange={(event) => onChange('buyer', event.target.value)} placeholder="Buyer name" />
      </label>
      <label>
        From
        <input type="date" value={filters.from ?? ''} onChange={(event) => onChange('from', event.target.value)} />
      </label>
      <label>
        To
        <input type="date" value={filters.to ?? ''} onChange={(event) => onChange('to', event.target.value)} />
      </label>
    </div>
  );
}
