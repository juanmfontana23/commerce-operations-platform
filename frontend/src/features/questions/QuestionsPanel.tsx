import type { FormEvent } from 'react';
import type { QuestionDto } from '../../shared/api/questionsApi';
import { StatusBadge } from '../../shared/components/StatusBadge';
import { formatDate } from '../../shared/utils/formatters';

interface QuestionsPanelProps {
  actionLoadingId: number | null;
  answerDrafts: Record<number, string>;
  onAnswer: (questionId: number, event: FormEvent<HTMLFormElement>) => void;
  onDraftChange: (questionId: number, value: string) => void;
  onResolve: (questionId: number) => void;
  questions: QuestionDto[];
}

export function QuestionsPanel({
  actionLoadingId,
  answerDrafts,
  onAnswer,
  onDraftChange,
  onResolve,
  questions,
}: QuestionsPanelProps) {
  return (
    <section className="panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Customer care</p>
          <h2>Order questions</h2>
        </div>
      </div>

      {questions.length === 0 ? (
        <p className="empty-state">This order has no questions.</p>
      ) : (
        <div className="question-list">
          {questions.map((question) => (
            <article className="question-card" key={question.id}>
              <div className="question-meta">
                <StatusBadge value={question.status} />
                <StatusBadge value={question.priority.priority} />
                <span>{question.priority.score} pts</span>
              </div>
              <p>{question.message}</p>
              <dl>
                <div>
                  <dt>Buyer</dt>
                  <dd>{question.buyerName}</dd>
                </div>
                <div>
                  <dt>Product</dt>
                  <dd>{question.productTitle ?? 'General order question'}</dd>
                </div>
                <div>
                  <dt>Created</dt>
                  <dd>{formatDate(question.createdAt)}</dd>
                </div>
              </dl>
              {question.answer && (
                <div className="answer-box">
                  <span>Answer</span>
                  <p>{question.answer}</p>
                </div>
              )}
              {question.status === 'OPEN' && (
                <form className="answer-form" onSubmit={(event) => onAnswer(question.id, event)}>
                  <textarea
                    onChange={(event) => onDraftChange(question.id, event.target.value)}
                    placeholder="Write a clear customer answer"
                    value={answerDrafts[question.id] ?? ''}
                  />
                  <button disabled={actionLoadingId === question.id} type="submit">
                    {actionLoadingId === question.id ? 'Saving...' : 'Send answer'}
                  </button>
                </form>
              )}
              {question.status !== 'RESOLVED' && (
                <button className="secondary-action" disabled={actionLoadingId === question.id} onClick={() => onResolve(question.id)} type="button">
                  Mark resolved
                </button>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
