import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { getOrder, getSellerOrders, type OrderDetailDto, type OrderFilters, type OrderSummaryDto } from '../../shared/api/ordersApi';
import { answerQuestion, getOrderQuestions, getSellerUnresolvedQuestions, resolveQuestion, type QuestionDto } from '../../shared/api/questionsApi';
import { StatusBadge } from '../../shared/components/StatusBadge';
import { formatMoney } from '../../shared/utils/formatters';
import { QuestionsPanel } from '../questions/QuestionsPanel';
import { UnresolvedPriorityPanel } from '../questions/UnresolvedPriorityPanel';
import { OrderFilters as OrderFiltersComponent } from './OrderFilters';
import { OrdersTable } from './OrdersTable';
import { Spinner } from '../../shared/components/Spinner';

interface OrdersDashboardProps {
  sellerId: number;
  onLogout: () => void;
}

interface EnrichedOrder extends OrderSummaryDto {
  itemsCount: number;
  openQuestionsCount: number;
}

export function OrdersDashboard({ sellerId, onLogout }: OrdersDashboardProps) {
  const [filters, setFilters] = useState<OrderFilters>({});
  const [orders, setOrders] = useState<EnrichedOrder[]>([]);
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<OrderDetailDto | null>(null);
  const [orderQuestions, setOrderQuestions] = useState<QuestionDto[]>([]);
  const [unresolvedQuestions, setUnresolvedQuestions] = useState<QuestionDto[]>([]);
  const [answerDrafts, setAnswerDrafts] = useState<Record<number, string>>({});
  const [ordersLoading, setOrdersLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;

    async function loadOrders() {
      setOrdersLoading(true);
      setError(null);

      try {
        const summaries = await getSellerOrders(sellerId, filters);
        const enriched = await Promise.all(
          summaries.map(async (order) => {
            const [detail, questions] = await Promise.all([getOrder(order.id), getOrderQuestions(order.id)]);
            return {
              ...order,
              itemsCount: detail.items.reduce((sum, item) => sum + item.quantity, 0),
              openQuestionsCount: questions.filter((question) => question.status === 'OPEN').length,
            };
          }),
        );

        if (!ignore) {
          setOrders(enriched);
          setSelectedOrderId((current) =>
            current && enriched.some((order) => order.id === current) ? current : (enriched[0]?.id ?? null),
          );
        }
      } catch (err) {
        if (!ignore) {
          setError(err instanceof Error ? err.message : 'Unable to load orders.');
        }
      } finally {
        if (!ignore) {
          setOrdersLoading(false);
        }
      }
    }

    loadOrders();

    return () => {
      ignore = true;
    };
  }, [filters, sellerId]);

  useEffect(() => {
    let ignore = false;

    async function loadDetail() {
      if (!selectedOrderId) {
        setSelectedOrder(null);
        setOrderQuestions([]);
        return;
      }

      setDetailLoading(true);
      setError(null);

      try {
        const [detail, questions] = await Promise.all([getOrder(selectedOrderId), getOrderQuestions(selectedOrderId)]);
        if (!ignore) {
          setSelectedOrder(detail);
          setOrderQuestions(questions);
        }
      } catch (err) {
        if (!ignore) {
          setError(err instanceof Error ? err.message : 'Unable to load order detail.');
        }
      } finally {
        if (!ignore) {
          setDetailLoading(false);
        }
      }
    }

    loadDetail();

    return () => {
      ignore = true;
    };
  }, [selectedOrderId]);

  useEffect(() => {
    refreshUnresolvedQuestions();
  }, [sellerId]);

  async function refreshUnresolvedQuestions() {
    try {
      const questions = await getSellerUnresolvedQuestions(sellerId);
      setUnresolvedQuestions(questions);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load unresolved questions.');
    }
  }

  function updateFilter(name: keyof OrderFilters, value: string) {
    setFilters((current) => ({ ...current, [name]: value || undefined }));
  }

  async function refreshCurrentData() {
    if (!selectedOrderId) {
      await refreshUnresolvedQuestions();
      return;
    }

    const [questions, unresolved] = await Promise.all([
      getOrderQuestions(selectedOrderId),
      getSellerUnresolvedQuestions(sellerId),
    ]);
    setOrderQuestions(questions);
    setUnresolvedQuestions(unresolved);
    setOrders((current) =>
      current.map((order) =>
        order.id === selectedOrderId
          ? { ...order, openQuestionsCount: questions.filter((question) => question.status === 'OPEN').length }
          : order,
      ),
    );
  }

  async function handleAnswer(questionId: number, event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const answer = answerDrafts[questionId]?.trim();
    if (!answer) {
      return;
    }

    setActionLoadingId(questionId);
    setError(null);
    try {
      await answerQuestion(questionId, answer);
      setAnswerDrafts((current) => ({ ...current, [questionId]: '' }));
      await refreshCurrentData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to answer the question.');
    } finally {
      setActionLoadingId(null);
    }
  }

  async function handleResolve(questionId: number) {
    setActionLoadingId(questionId);
    setError(null);
    try {
      await resolveQuestion(questionId);
      await refreshCurrentData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to resolve the question.');
    } finally {
      setActionLoadingId(null);
    }
  }

  return (
    <main className="dashboard-shell">
      <section className="hero-card">
        <div>
          <p className="eyebrow">Seller workspace</p>
          <h1>Orders and buyer questions</h1>
          <p>Track recent sales, prioritize unanswered questions, and close customer loops from one dashboard.</p>
        </div>
        <div className="hero-metrics" aria-label="Dashboard summary">
          <Metric label="Orders" value={orders.length.toString()} />
          <Metric label="Unresolved" value={unresolvedQuestions.length.toString()} />
          <Metric label="Seller ID" value={sellerId.toString()} />
          <button type="button" onClick={onLogout}>Sign out</button>
        </div>
      </section>

      {error && <div className="alert">{error}</div>}

      <section className="content-grid">
        <div className="stack">
          <section className="panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">Order search</p>
                <h2>Seller orders</h2>
              </div>
            </div>
            <OrderFiltersComponent filters={filters} onChange={updateFilter} />

            {ordersLoading ? (
              <p className="empty-state"><Spinner /> Loading orders...</p>
            ) : orders.length === 0 ? (
              <p className="empty-state">No orders match the current filters.</p>
            ) : (
              <OrdersTable orders={orders} selectedOrderId={selectedOrderId} onSelectOrder={setSelectedOrderId} />
            )}
          </section>

          <section className="panel">
            <div className="panel-header">
              <div>
                <p className="eyebrow">Selected order</p>
                <h2>Order detail</h2>
              </div>
            </div>
            {detailLoading ? (
              <p className="empty-state"><Spinner /> Loading order detail...</p>
            ) : selectedOrder ? (
              <OrderDetail order={selectedOrder} />
            ) : (
              <p className="empty-state">Select an order to inspect purchased items.</p>
            )}
          </section>
        </div>

        <aside className="stack">
          <UnresolvedPriorityPanel questions={unresolvedQuestions} onSelectOrder={setSelectedOrderId} />
          <QuestionsPanel
            actionLoadingId={actionLoadingId}
            answerDrafts={answerDrafts}
            onAnswer={handleAnswer}
            onDraftChange={(questionId, value) => setAnswerDrafts((current) => ({ ...current, [questionId]: value }))}
            onResolve={handleResolve}
            questions={orderQuestions}
          />
        </aside>
      </section>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

function OrderDetail({ order }: { order: OrderDetailDto }) {
  return (
    <div className="detail-card">
      <div className="detail-summary">
        <div>
          <span>Order #{order.id}</span>
          <strong>{order.buyer.name}</strong>
          <small>{order.buyer.email}</small>
        </div>
        <StatusBadge value={order.status} />
      </div>
      <div className="items-table">
        <div className="items-head">
          <span>Product</span>
          <span>Quantity</span>
          <span>Unit price</span>
          <span>Subtotal</span>
        </div>
        {order.items.map((item) => (
          <div className="items-row" key={item.productId}>
            <span>{item.productTitle}</span>
            <span>{item.quantity}</span>
            <span>{formatMoney(item.unitPrice)}</span>
            <span>{formatMoney(item.subtotal)}</span>
          </div>
        ))}
      </div>
      <div className="detail-total">
        <span>Total</span>
        <strong>{formatMoney(order.totalAmount)}</strong>
      </div>
    </div>
  );
}
