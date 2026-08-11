import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { OrdersTable } from './OrdersTable';
import type { OrderSummaryDto } from '../../shared/api/ordersApi';

const baseOrder: OrderSummaryDto & { itemsCount: number; openQuestionsCount: number } = {
  id: 1,
  status: 'PAID',
  placedAt: '2025-06-15T10:30:00Z',
  buyer: { id: 10, name: 'Jane Doe', email: 'jane@example.com' },
  totalAmount: 159.99,
  itemsCount: 3,
  openQuestionsCount: 1,
};

const secondOrder: OrderSummaryDto & { itemsCount: number; openQuestionsCount: number } = {
  id: 2,
  status: 'SHIPPED',
  placedAt: '2025-06-20T14:00:00Z',
  buyer: { id: 20, name: 'John Smith', email: 'john@example.com' },
  totalAmount: 49.5,
  itemsCount: 1,
  openQuestionsCount: 0,
};

describe('OrdersTable', () => {
  it('renders order rows with correct IDs', () => {
    render(<OrdersTable orders={[baseOrder, secondOrder]} selectedOrderId={null} onSelectOrder={vi.fn()} />);

    expect(screen.getByText('#1')).toBeInTheDocument();
    expect(screen.getByText('#2')).toBeInTheDocument();
  });

  it('renders buyer names', () => {
    render(<OrdersTable orders={[baseOrder, secondOrder]} selectedOrderId={null} onSelectOrder={vi.fn()} />);

    expect(screen.getByText('Jane Doe')).toBeInTheDocument();
    expect(screen.getByText('John Smith')).toBeInTheDocument();
  });

  it('renders status badges for each order', () => {
    render(<OrdersTable orders={[baseOrder, secondOrder]} selectedOrderId={null} onSelectOrder={vi.fn()} />);

    expect(screen.getByText('Paid')).toBeInTheDocument();
    expect(screen.getByText('Shipped')).toBeInTheDocument();
  });

  it('renders formatted amounts', () => {
    render(<OrdersTable orders={[baseOrder]} selectedOrderId={null} onSelectOrder={vi.fn()} />);

    expect(screen.getByText('$159.99')).toBeInTheDocument();
  });

  it('renders item counts and open questions count', () => {
    render(<OrdersTable orders={[baseOrder]} selectedOrderId={null} onSelectOrder={vi.fn()} />);

    expect(screen.getByText('3 items')).toBeInTheDocument();
    expect(screen.getByText('1 open questions')).toBeInTheDocument();
  });

  it('highlights the selected order row', () => {
    render(<OrdersTable orders={[baseOrder, secondOrder]} selectedOrderId={1} onSelectOrder={vi.fn()} />);

    const row1 = screen.getByText('#1').closest('button')!;
    const row2 = screen.getByText('#2').closest('button')!;

    expect(row1.className).toContain('order-row-active');
    expect(row2.className).not.toContain('order-row-active');
  });

  it('calls onSelectOrder when a row is clicked', async () => {
    const onSelect = vi.fn();
    render(<OrdersTable orders={[baseOrder, secondOrder]} selectedOrderId={null} onSelectOrder={onSelect} />);

    screen.getByText('#2').closest('button')!.click();

    expect(onSelect).toHaveBeenCalledWith(2);
  });

  it('renders empty list when orders array is empty', () => {
    const { container } = render(<OrdersTable orders={[]} selectedOrderId={null} onSelectOrder={vi.fn()} />);

    expect(container.querySelector('.orders-list')!.children.length).toBe(0);
  });
});
