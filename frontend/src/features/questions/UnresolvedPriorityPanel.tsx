import type { QuestionDto } from '../../shared/api/questionsApi';
import { StatusBadge } from '../../shared/components/StatusBadge';
import { formatDate } from '../../shared/utils/formatters';

interface UnresolvedPriorityPanelProps {
  onSelectOrder: (orderId: number) => void;
  questions: QuestionDto[];
}

export function UnresolvedPriorityPanel({ onSelectOrder, questions }: UnresolvedPriorityPanelProps) {
  return (
    <section className="panel priority-panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Backend priority queue</p>
          <h2>Unresolved questions</h2>
        </div>
      </div>
      {questions.length === 0 ? (
        <p className="empty-state">No unresolved questions remain.</p>
      ) : (
        <div className="priority-list">
          {questions.map((question) => (
            <button className="priority-item" key={question.id} onClick={() => onSelectOrder(question.orderId)} type="button">
              <span className="priority-score">{question.priority.score}</span>
              <span>
                <strong>{question.buyerName}</strong>
                <small>Order #{question.orderId} · {formatDate(question.createdAt)}</small>
              </span>
              <StatusBadge value={question.priority.priority} />
            </button>
          ))}
        </div>
      )}
    </section>
  );
}
