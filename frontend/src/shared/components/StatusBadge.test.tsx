import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { StatusBadge } from './StatusBadge';

describe('StatusBadge', () => {
  it('renders CREATED with neutral tone', () => {
    render(<StatusBadge value="CREATED" />);
    const badge = screen.getByText('Created');
    expect(badge).toBeInTheDocument();
    expect(badge.className).toContain('badge-neutral');
  });

  it('renders PAID with positive tone', () => {
    render(<StatusBadge value="PAID" />);
    const badge = screen.getByText('Paid');
    expect(badge.className).toContain('badge-positive');
  });

  it('renders SHIPPED with info tone', () => {
    render(<StatusBadge value="SHIPPED" />);
    const badge = screen.getByText('Shipped');
    expect(badge.className).toContain('badge-info');
  });

  it('renders DELIVERED with positive tone', () => {
    render(<StatusBadge value="DELIVERED" />);
    const badge = screen.getByText('Delivered');
    expect(badge.className).toContain('badge-positive');
  });

  it('renders CANCELLED with danger tone', () => {
    render(<StatusBadge value="CANCELLED" />);
    const badge = screen.getByText('Cancelled');
    expect(badge.className).toContain('badge-danger');
  });

  it('renders OPEN (question status) with warning tone', () => {
    render(<StatusBadge value="OPEN" />);
    const badge = screen.getByText('Open');
    expect(badge.className).toContain('badge-warning');
  });

  it('renders ANSWERED with info tone', () => {
    render(<StatusBadge value="ANSWERED" />);
    const badge = screen.getByText('Answered');
    expect(badge.className).toContain('badge-info');
  });

  it('renders RESOLVED with positive tone', () => {
    render(<StatusBadge value="RESOLVED" />);
    const badge = screen.getByText('Resolved');
    expect(badge.className).toContain('badge-positive');
  });

  it('renders LOW priority with neutral tone', () => {
    render(<StatusBadge value="LOW" />);
    const badge = screen.getByText('Low');
    expect(badge.className).toContain('badge-neutral');
  });

  it('renders MEDIUM priority with info tone', () => {
    render(<StatusBadge value="MEDIUM" />);
    const badge = screen.getByText('Medium');
    expect(badge.className).toContain('badge-info');
  });

  it('renders HIGH priority with warning tone', () => {
    render(<StatusBadge value="HIGH" />);
    const badge = screen.getByText('High');
    expect(badge.className).toContain('badge-warning');
  });

  it('renders CRITICAL priority with danger tone', () => {
    render(<StatusBadge value="CRITICAL" />);
    const badge = screen.getByText('Critical');
    expect(badge.className).toContain('badge-danger');
  });

  it('applies the base badge class', () => {
    render(<StatusBadge value="PAID" />);
    expect(screen.getByText('Paid').className).toContain('badge');
  });
});
