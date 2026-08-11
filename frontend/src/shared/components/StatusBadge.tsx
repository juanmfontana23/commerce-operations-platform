import type { OrderStatus } from '../api/ordersApi';
import type { QuestionPriority, QuestionStatus } from '../api/questionsApi';
import { titleCase } from '../utils/formatters';

type BadgeTone = 'neutral' | 'positive' | 'warning' | 'danger' | 'info';

interface StatusBadgeProps {
  value: OrderStatus | QuestionStatus | QuestionPriority;
}

const tones: Record<string, BadgeTone> = {
  CREATED: 'neutral',
  PAID: 'positive',
  SHIPPED: 'info',
  DELIVERED: 'positive',
  CANCELLED: 'danger',
  OPEN: 'warning',
  ANSWERED: 'info',
  RESOLVED: 'positive',
  LOW: 'neutral',
  MEDIUM: 'info',
  HIGH: 'warning',
  CRITICAL: 'danger',
};

export function StatusBadge({ value }: StatusBadgeProps) {
  return <span className={`badge badge-${tones[value]}`}>{titleCase(value)}</span>;
}
