import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { OrderFilters } from './OrderFilters';

describe('OrderFilters', () => {
  const defaultProps = {
    filters: {},
    onChange: vi.fn(),
  };

  it('renders all four filter inputs', () => {
    render(<OrderFilters {...defaultProps} />);

    expect(screen.getByLabelText('Status')).toBeInTheDocument();
    expect(screen.getByLabelText('Buyer')).toBeInTheDocument();
    expect(screen.getByLabelText('From')).toBeInTheDocument();
    expect(screen.getByLabelText('To')).toBeInTheDocument();
  });

  it('renders the status select with all options', () => {
    render(<OrderFilters {...defaultProps} />);

    const select = screen.getByLabelText('Status') as HTMLSelectElement;
    expect(select.tagName).toBe('SELECT');
    expect(select.options.length).toBe(6); // All + 5 statuses
    expect(select.options[0].text).toBe('All statuses');
  });

  it('renders buyer input as text', () => {
    render(<OrderFilters {...defaultProps} />);

    const input = screen.getByLabelText('Buyer') as HTMLInputElement;
    expect(input.type).toBe('text');
    expect(input.placeholder).toBe('Buyer name');
  });

  it('renders date inputs for From and To', () => {
    render(<OrderFilters {...defaultProps} />);

    const from = screen.getByLabelText('From') as HTMLInputElement;
    const to = screen.getByLabelText('To') as HTMLInputElement;
    expect(from.type).toBe('date');
    expect(to.type).toBe('date');
  });

  it('displays current filter values', () => {
    render(
      <OrderFilters
        filters={{ status: 'PAID', buyer: 'Jane', from: '2025-01-01', to: '2025-06-30' }}
        onChange={vi.fn()}
      />,
    );

    expect((screen.getByLabelText('Status') as HTMLSelectElement).value).toBe('PAID');
    expect((screen.getByLabelText('Buyer') as HTMLInputElement).value).toBe('Jane');
    expect((screen.getByLabelText('From') as HTMLInputElement).value).toBe('2025-01-01');
    expect((screen.getByLabelText('To') as HTMLInputElement).value).toBe('2025-06-30');
  });

  it('has the correct aria-label on the filter container', () => {
    render(<OrderFilters {...defaultProps} />);

    const container = document.querySelector('[aria-label="Order filters"]');
    expect(container).toBeInTheDocument();
  });
});
