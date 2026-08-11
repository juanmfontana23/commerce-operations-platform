# Technical Design: Commerce Operations Platform

This document explains the core technical decisions behind the marketplace seller operations platform so a reviewer can evaluate both implementation and tradeoffs quickly.

## Overview

The system is a small full-stack dashboard for seller operations:

- Backend: Spring Boot API with PostgreSQL persistence, Flyway migrations, profile-scoped H2 seed data, configuration-driven HTTP Basic seller authentication/ownership checks, validation, priority scoring, notification simulation, transactional outbox processing, and bounded request observability.
- Frontend: React + TypeScript + Vite dashboard that consumes the backend through a `/api` dev proxy.
- Main domain: sellers, buyers, products, orders, order items, questions, and notifications.

## Features

| Capability | Design intent |
| --- | --- |
| Order browsing | Help the seller inspect recent operational context quickly. |
| Order filters | Support common review paths by status, buyer, and date range. |
| Question handling | Let the seller answer and close buyer issues without conflating both actions. |
| Priority queue | Put unresolved questions with higher business risk first. |
| Notifications | Alert high-priority/critical cases while keeping delivery provider-agnostic. |
| Order event delivery | Decouple successful order writes from asynchronous notification work without introducing a broker. |

## Tech Stack

| Layer | Choice | Reason |
| --- | --- | --- |
| Backend | Java 21 + Spring Boot 3.5 | Modern Java baseline with productive REST/JPA support. |
| Persistence | PostgreSQL + Flyway + Spring Data JPA | Production-like durability with versioned, reviewable schema changes. |
| Validation | Jakarta Validation through Spring Boot | Request constraints live at API boundaries. |
| Frontend | React + TypeScript + Vite | Typed UI with fast local development and build verification. |
| Tooling | Maven Wrapper, npm scripts | Evaluators can run the app without globally installed Maven. |
| Observability | Spring Boot Actuator + servlet correlation filter | Exposes only safe operational checks and makes one request traceable across logs and responses. |
| Security | Spring Security HTTP Basic + configuration-bound in-memory identities | Minimal portfolio boundary: the browser collects credentials at runtime, every order/question endpoint requires authentication, and the configured seller ID owns the resource. |

## How to Run Backend

```bash
cd backend
docker compose up -d
./mvnw test
./mvnw spring-boot:run
```

Backend URL: `http://localhost:8080`.

### Observability Contract

The public management surface is intentionally limited to these exact endpoints:

| Endpoint | Behavior |
| --- | --- |
| `GET /actuator/health` | Returns `UP`/`DOWN` without component details. |
| `GET /actuator/info` | Returns safe application metadata when present. |
| `GET /actuator/metrics` | Authenticated metric names and values; values can be queried at `/actuator/metrics/{metricName}`. |

`/actuator/env`, `/actuator/beans`, `/actuator/configprops`, and `/actuator/loggers` are not web-exposed. Health and info remain public, while metrics require seller authentication so operational metadata is not available anonymously.

### Security Boundary

Spring Security requires authentication for `/api/**` and denies all other non-Actuator routes. Only `GET /actuator/health` and `/actuator/info` are public; metrics require authentication and unexposed Actuator endpoints return `404`. Basic identities are loaded from `security.users` and map a username to a seller ID. The frontend validates credentials against `GET /api/csrf-token` at runtime and keeps them in memory only; no credential is supplied by the Vite build. Controllers invoke one shared `SellerAuthorization` boundary before returning or mutating an order/question, so a seller cannot access another seller's list, order, or question. The default profile requires `SECURITY_SELLER*_ID` and `SECURITY_SELLER*_PASSWORD`; local/test profiles own deterministic demo credentials. State-changing API requests use the `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header through `GET /api/csrf-token`.

Requests use `X-Request-Id` for correlation. A value is accepted only when it is 1-64 characters, starts with an alphanumeric character, and contains only alphanumerics or `.`, `_`, `~`, and `-`. Otherwise the backend generates a UUID, returns the selected value in `X-Request-Id`, stores it in MDC as `correlationId`, and emits one structured request log with method, URI path, status, duration in milliseconds, and correlation ID. It intentionally excludes request bodies and headers that may contain secrets.

The default datasource is `jdbc:postgresql://localhost:5432/commerce_operations`. Flyway applies migrations from `backend/src/main/resources/db/migration`, then Hibernate validates the mapped schema with `ddl-auto=validate`.

### Transactional Outbox Contract

`OrderService.transitionStatus()` is the write boundary. After validating and changing the status, it saves the order and an `OutboxEvent` before the transaction commits. A rollback removes both records, so an event cannot describe an order state that was not committed.

| Concern | Decision |
| --- | --- |
| Event identity | Random UUID for tracing and provider idempotency; deterministic unique key for one order/status transition. |
| Claiming | `PESSIMISTIC_WRITE` query over due `PENDING` and lease-expired `PROCESSING` rows, limited to 20. |
| Lease | `available_at` is moved 30 seconds into the future while processing. A crashed worker leaves reclaimable work. |
| Retry | Attempts increment on failure; delays are 2, 4, 8 seconds and then capped at 300 seconds; attempt 5 is terminal `FAILED`. An authenticated operator workflow can requeue one event by ID, resetting its retry budget while retaining `last_error` for diagnosis. |
| Side effect | `OrderEventHandler` delegates to `OrderEventNotifier`; the current notifier logs rather than calling an external service. |
| Idempotency | `outbox_deliveries.event_id` has a unique constraint and is checked before handling. |

This is at-least-once delivery. The unique delivery record prevents duplicate modeled side effects during normal redelivery. A real external provider must accept the event UUID as an idempotency key because no local database transaction can atomically commit with a remote API call. Terminal failures remain observable and are not silently discarded: an authenticated operator job or internal command runner must call `OutboxProcessingService.requeueFailed(eventId, now)` for a reviewed event, then the worker retries it. The operation locks the row, accepts only `FAILED`, resets the attempt budget, and preserves `last_error` until successful delivery. There is intentionally no unauthenticated management endpoint. The trade-off is operational safety and explicit review at the cost of manual intervention; a broker, multiple event types, and provider integration remain out of scope.

For a fast H2 demo, run `SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run`. This uses the same migration and enables the H2 console at `http://localhost:8080/h2-console`; only `local` and `test` profiles load seed data.

## How to Run Frontend

```bash
cd frontend
npm install
npm run build
npm run dev -- --host 127.0.0.1
```

Frontend URL: `http://127.0.0.1:5173`.

Vite proxies `/api` to `http://localhost:8080`.

## Full-Stack Validation Path

- [ ] `./mvnw -q -Dtest=PersistenceIntegrationTest test` passes in `backend`.
- [ ] `./mvnw test` passes in `backend`.
- [ ] `npm run build` passes in `frontend`.
- [ ] Backend serves `GET /api/sellers/1/orders` and returns `X-Request-Id`.
- [ ] Public health/info return `200`; unauthenticated metrics returns `401`; `/actuator/env` is not exposed after authentication.
- [ ] An order transition leaves a `PENDING` outbox row with the matching aggregate ID and JSON payload.
- [ ] A duplicate event ID produces one delivery record and one notifier call.
- [ ] Frontend renders orders and unresolved questions from backend seed data.
- [ ] Vite proxy serves `GET /api/sellers/1/orders` from the frontend origin.

## Seed Data / Useful IDs

Seed data is interview-friendly with realistic edge cases and is loaded only in the `local` and `test` profiles.

| Item | Value |
| --- | --- |
| Seller | `1` - Electro Shop BA |
| Orders | 6 orders in different statuses (CREATED, PAID, SHIPPED, DELIVERED, CANCELLED) |
| Buyers | 6 buyers with varied profiles |
| Products | 8 products across electronics, home, and accessories |
| Questions | 8 questions in OPEN, ANSWERED, and RESOLVED states |

Edge cases: question without product, 15-day-old unresolved question, urgent keyword matches, high-value order questions.

IDs are stable on a fresh `local` backend start because H2 is recreated from startup seed data. PostgreSQL is persistent and intentionally does not load demo data automatically.

## API Endpoints

| Method | Endpoint | Notes |
| --- | --- | --- |
| `GET` | `/api/sellers/{sellerId}/orders` | Authenticated owner only. Optional filters: `status`, `buyer`, `from`, `to`. |
| `GET` | `/api/orders/{orderId}` | Authenticated owning seller only. Returns order detail and item snapshots. |
| `GET` | `/api/orders/{orderId}/questions` | Authenticated owning seller only. Returns questions for an order with priority data. |
| `GET` | `/api/sellers/{sellerId}/questions/unresolved` | Authenticated owner only. Returns unresolved questions ordered by priority score. |
| `POST` | `/api/questions/{questionId}/answer` | Authenticated owner only. Body: `{"answer":"..."}`. Moves `OPEN` questions to `ANSWERED`. |
| `POST` | `/api/questions/{questionId}/resolve` | Authenticated owner only. Moves the question to `RESOLVED`. |
| `POST` | `/api/orders/{orderId}/transition` | Authenticated owner only. Body: `{"status":"PAID"}`. Enforces valid status transitions. |

## Business Assumptions

- Seller operations need the latest order context and open buyer questions in one place.
- Product title and unit price are copied into `OrderItem` so past orders remain accurate after catalog changes.
- A question can be related to the whole order or to one product inside that order.
- Answering a question means the seller replied; resolving means the support case is closed.
- Priority should be transparent enough for a seller, reviewer, or support lead to challenge it.
- Order status transitions follow a valid lifecycle: CREATED -> PAID -> SHIPPED -> DELIVERED, with CANCELLED as terminal.

## Validation and Error Handling

Bean Validation annotations enforce request constraints at API boundaries:

- `@NotBlank` on required text fields.
- `@NotNull` on required objects/enums.

All error responses follow a consistent JSON structure:

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "answer must not be blank",
  "path": "/api/questions/1/answer"
}
```

Invalid status transitions, malformed JSON, and invalid query/path/date parameters return `400` with a descriptive message. Parser details are intentionally not exposed to clients.

## Test Coverage

| Layer | Framework | What is tested |
| --- | --- | --- |
| Backend unit | JUnit 5 + Mockito | Priority calculator scoring, order service filters/transitions, question service lifecycle |
| Backend integration | MockMvc + Spring Boot Test | API happy paths, authentication/ownership denial, validation errors, 404s, status transitions, correlation IDs, and Actuator exposure |
| Frontend unit | vitest + @testing-library/react | API clients, filter components, table rendering, badge states, loading spinner |

## Priority Scoring Explanation

Priority is computed by `QuestionPriorityCalculator` as a sum of independent, explainable factors.

```txt
priorityScore = waitingTimeScore + orderValueScore + keywordScore + productCategoryScore
```

| Factor | Current rule |
| --- | --- |
| Waiting time | 48h+: `35`, 24h+: `25`, 8h+: `15`, recent: `5`. |
| Order value | 500+: `25`, 250+: `15`, 100+: `8`. |
| Keywords | Urgent operational keywords add `25`; administrative keywords add `10`. |
| Product category | Electronics adds `15`; home adds `8`. |

| Score | Priority |
| --- | --- |
| `0-39` | `LOW` |
| `40-69` | `MEDIUM` |
| `70-89` | `HIGH` |
| `90+` | `CRITICAL` |

Each result includes reason strings such as `Waiting more than 48 hours`, `High-value order`, or `Message contains urgent support keywords`. That makes the queue auditable instead of a black box.

## Notification Extensibility

Question workflows depend on the `NotificationChannel` abstraction:

```java
public interface NotificationChannel {
    void send(Notification notification);
}
```

Current implementation: `EmailNotificationChannel` logs/simulates email delivery.

Future providers such as Slack, SMS, or real email can be added as new adapters without rewriting question priority logic.

## Architecture Rationale

The backend is organized by business capability instead of global technical layers.

```txt
backend/src/main/java/com/example/commerceoperations/
  orders/
    domain/
    application/
    infrastructure/
    api/
  questions/
    domain/
    application/
    infrastructure/
    api/
  notifications/
    domain/
    application/
    infrastructure/
  shared/
```

The frontend mirrors the same capability split.

```txt
frontend/src/
  features/
    orders/
    questions/
  shared/
    api/
    components/
    utils/
```

Why this matters in an interview:

- Business boundaries are visible from the folder structure.
- Application services own use cases; controllers stay thin.
- Repositories hide persistence details from use-case orchestration.
- DTOs protect API contracts from accidental JPA exposure.
- Priority and notification rules are isolated, testable seams.

## Tradeoffs and Future Improvements

| Decision | Tradeoff | Future improvement |
| --- | --- | --- |
| PostgreSQL + Flyway persistence | More realistic runtime and explicit schema history, with a local database dependency. | Add deployment-managed secrets and backups. |
| H2 local/test persistence | Fast and deterministic feedback loop. | Keep it scoped to local demos and automated tests. |
| Fixed scoring weights in code | Easy to inspect, but requires deployment to tune. | Move weights and thresholds to configuration. |
| Console email notification | No provider credentials needed; logs contain only a seller identifier, never the recipient email. | Add real provider adapters behind `NotificationChannel`. |
| Compact frontend state | Simple MVP, but limited caching and mutation ergonomics. | Add a query library if interactions expand. |
| Feature-based modules | Clear domain boundaries, but small apps may look slightly more structured than necessary. | Keep structure only while it continues to reduce cognitive load. |
| In-memory seed data | Deterministic for demos, but resets on restart. | Add database persistence with migration scripts. |

## Interview Defense Points

- The project is not accidental CRUD: it encodes order snapshots, question lifecycle, priority rules, and notification extension points.
- Price/title snapshots protect historical order integrity.
- Priority scoring is explainable and auditable before it is sophisticated.
- Answering and resolving are separated because customer support state is more nuanced than a single boolean.
- Feature-based architecture makes the same business capabilities visible in backend and frontend.
- Order status transitions enforce a valid business lifecycle, not just free-form state changes.
- Bean Validation and structured error responses show attention to API contract quality.
- 81 tests across backend and frontend demonstrate behavior-first development and confidence in the implementation.
