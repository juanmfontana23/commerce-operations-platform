# Commerce Operations Platform

Full-stack marketplace seller operations platform. It lets a seller review orders, inspect buyer questions, prioritize unresolved questions, and trigger extensible notifications for urgent cases.

## Quick Start

1. Start PostgreSQL from `backend` with `docker compose up -d`.
2. Start the backend from `backend` with `./mvnw spring-boot:run`.
3. Start the frontend from `frontend` with `npm install` and `npm run dev`.
4. Open `http://127.0.0.1:5173`, sign in with the local seller credentials below, and verify that orders and priority questions load through the Vite `/api` proxy.
5. Use seller `1` with 6 seeded orders and 8 questions across different statuses and priorities.

The API uses HTTP Basic authentication for this portfolio slice. The default profile has no credential fallbacks and fails at startup unless seller credentials are supplied through environment variables. The browser collects credentials at runtime, keeps them only in memory, and never receives them from Vite environment variables or a built asset. The `local` and `test` profiles define explicit deterministic demo credentials only.

## Overview

The app models a small seller operations dashboard:

- Recent orders with buyer and item-level purchase snapshots.
- Buyer questions linked to orders and optionally to products.
- Explainable priority scoring for unresolved questions.
- Answer and resolve actions as separate lifecycle steps.
- Notification delivery behind a channel abstraction.

## Features

| Area | Implemented behavior |
| --- | --- |
| Orders | List seller orders, filter by status/date/buyer, inspect order details, and transition order status. |
| Questions | List order questions and unresolved seller questions ordered by priority. |
| Question actions | Answer a question without forcing resolution; resolve explicitly when closed. |
| Priority | Score unresolved questions by waiting time, order value, support keywords, and product category. |
| Notifications | Simulate email notifications for high-priority and critical questions through an extensible channel interface. |
| Order events | Persist order status events transactionally and process them asynchronously with a bounded PostgreSQL-backed worker. |
| Validation | Bean Validation on DTOs, structured error responses, and order status transition enforcement. |

## Tech Stack

| Layer | Choice |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5, Spring Web, Spring Data JPA, Validation, Actuator |
| Persistence | PostgreSQL with Flyway migrations; H2 remains available for local/demo tests |
| Backend tests | Maven Wrapper, JUnit 5, Mockito, MockMvc, Flyway, transactional outbox coverage |
| Frontend | React, TypeScript, Vite |
| Frontend tests | vitest, @testing-library/react, jsdom (42 tests) |

## How to Run Backend

```bash
cd backend
docker compose up -d
./mvnw test
./mvnw spring-boot:run
```

The default runtime uses PostgreSQL and applies pending migrations before Hibernate validates the schema. Set `DB_USERNAME`, `DB_PASSWORD`, and the seller credential variables before starting it; `DB_URL` and `DB_POOL_SIZE` remain configurable through environment variables.

### Local Authentication

The backend maps each configured username to exactly one seller ID. Safe local credentials are supplied only in `application-local.yml` and test resources; no production secret is stored in the repository:

| Identity | Local credential | Seller scope |
| --- | --- | --- |
| `seller1` | `seller1-local` | Seller `1` |
| `seller2` | `seller2-local` | Seller `2` |

Supply deployment credentials with `SECURITY_SELLER1_PASSWORD`, `SECURITY_SELLER2_PASSWORD`, and the corresponding `SECURITY_SELLER*_ID` variables before starting the default profile. Startup fails closed when any configured seller credential is missing. Do not add API credentials to `frontend/.env.example` or any `VITE_*` variable: Vite embeds those values in browser code. Sign in through the UI instead; signing out clears the in-memory credential and CSRF cookie.

PostgreSQL runtime details:

| Item | Value |
| --- | --- |
| Base URL | `http://localhost:8080` |
| JDBC URL | `jdbc:postgresql://localhost:5432/commerce_operations` |
| Username | `commerce` |
| Password | `commerce` |
| Migrations | `backend/src/main/resources/db/migration` |

## Backend Observability

The backend exposes a deliberately small public management surface under `/actuator`:

| Endpoint | Purpose | Exposure decision |
| --- | --- | --- |
| `GET /actuator/health` | Liveness/readiness-style application health status. | Public; component details stay hidden. |
| `GET /actuator/info` | Safe application metadata when configured. | Public; no environment or build secrets are exposed. |
| `GET /actuator/metrics` | Lists available application and JVM metric names. | Authenticated; individual metric values use `/actuator/metrics/{metricName}`. |

Other Actuator endpoints, including `/actuator/env`, `/actuator/beans`, and `/actuator/loggers`, are not exposed and return `404`. Metrics require seller Basic authentication so operational metadata is not available anonymously.

### Browser CSRF protection

State-changing API requests require a CSRF token in addition to Basic authentication. After runtime sign-in, the frontend requests `GET /api/csrf-token`, reads the `XSRF-TOKEN` cookie, and sends it as `X-XSRF-TOKEN` on POST requests. API clients must perform the same flow; do not disable CSRF protection.

## Transactional Outbox

Every successful `POST /api/orders/{orderId}/transition` writes the order update and one `ORDER_STATUS_CHANGED` row to `outbox_events` in the same transaction. The event stores its UUID, deterministic idempotency key, aggregate identity, JSON payload, status, attempt count, next available time, timestamps, and the last failure. The unique key is `ORDER_STATUS_CHANGED:{orderId}:{newStatus}`; it prevents duplicate publication of the same state transition.

An in-process worker polls due `PENDING` or lease-expired `PROCESSING` rows in batches of 20. PostgreSQL row locks claim work safely across multiple application instances, and `available_at` acts as a 30-second claim lease. Failures use exponential backoff (2, 4, 8 seconds, capped at 300 seconds) and become `FAILED` after 5 attempts. The handler delegates to an `OrderEventNotifier`, which is a logging implementation in this slice.

`outbox_deliveries.event_id` is unique and is written in the same transaction as the handler call. A redelivery therefore skips an already completed event and does not repeat the modeled side effect. An external provider must also honor the event UUID as its idempotency key to close the crash window between an external side effect and the local delivery commit. Set `OUTBOX_WORKER_ENABLED=false` for deterministic tests or manual inspection.

Useful metrics include `outbox.events.processed` tagged by `result=success|failure` and `outbox.events.retries` tagged by `result=scheduled|dead_letter`. Worker failures include event ID and attempt count in logs without changing the existing API contract.

Every request accepts a bounded `X-Request-Id` containing only letters, digits, `.`, `_`, `~`, or `-` (up to 64 characters). Invalid or missing values are replaced with a generated UUID. The selected ID is returned in the same response header, placed in the logging context, and included in one request log containing method, path, status, and duration. Request bodies and secret-bearing headers are never logged.

For a fast in-memory demo without Docker, use the explicit `local` profile. It runs the same Flyway migration against H2 and loads deterministic seed data:

```bash
cd backend
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

The H2 console is available at `http://localhost:8080/h2-console` for the `local` profile with JDBC URL `jdbc:h2:mem:commerce-operations`, user `sa`, and an empty password. Seed data is disabled unless the `local` or `test` profile is active.

## How to Run Frontend

```bash
cd frontend
npm install
npm run build
npm run dev -- --host 127.0.0.1
```

Frontend runtime details:

| Item | Value |
| --- | --- |
| App URL | `http://127.0.0.1:5173` |
| API proxy | `/api` -> `http://localhost:8080` |

## Full-Stack Validation Path

- [ ] Backend is running on `http://localhost:8080`.
- [ ] Frontend is running on `http://127.0.0.1:5173`.
- [ ] `http://127.0.0.1:5173` renders seeded orders for seller `1`.
- [ ] `GET http://localhost:8080/api/sellers/1/orders` returns seeded orders.
- [ ] `GET http://127.0.0.1:5173/api/sellers/1/orders` also works through the Vite proxy.
- [ ] Unresolved questions appear ordered by computed priority.

Useful API smoke checks:

```bash
  curl -u seller1:seller1-local http://localhost:8080/api/sellers/1/orders
  curl -u seller1:seller1-local http://localhost:8080/api/orders/1
  curl -u seller1:seller1-local http://localhost:8080/api/orders/1/questions
  curl -u seller1:seller1-local http://localhost:8080/api/sellers/1/questions/unresolved
```

## Seed Data / Useful IDs

Seed data is created at backend startup in `DataSeedConfig` only for the `local` and `test` profiles.

| Type | Details |
| --- | --- |
| Seller | `1` - Electro Shop BA |
| Buyers | 6 buyers with varied profiles |
| Products | 8 products across electronics, home, and accessories |
| Orders | 6 orders in different statuses: CREATED, PAID, SHIPPED, DELIVERED, CANCELLED |
| Questions | 8 questions in OPEN, ANSWERED, and RESOLVED states with different priorities |

Edge cases included: question without product, 15-day-old unresolved question, urgent keyword matches, high-value order questions.

The `local` H2 database is recreated on each backend start, so these IDs are stable for a fresh run. PostgreSQL data persists in the Docker volume and is not seeded automatically.

## API Endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/sellers/{sellerId}/orders` | Authenticated owner only. Supports `status`, `buyer`, `from`, and `to` query parameters. |
| `GET` | `/api/orders/{orderId}` | Authenticated owning seller only. Get order detail with item snapshots. |
| `GET` | `/api/orders/{orderId}/questions` | Authenticated owning seller only. List questions for one order. |
| `GET` | `/api/sellers/{sellerId}/questions/unresolved` | Authenticated owner only. List unresolved questions ordered by priority. |
| `POST` | `/api/questions/{questionId}/answer` | Authenticated owner only. Answer with JSON body `{"answer":"..."}`. |
| `POST` | `/api/questions/{questionId}/resolve` | Authenticated owner only. Mark a question as resolved. |
| `POST` | `/api/orders/{orderId}/transition` | Authenticated owner only. Transition with JSON body `{"status":"PAID"}`. |

Operational endpoints are documented separately in [Backend Observability](#backend-observability).

Example filtered request:

```bash
curl "http://localhost:8080/api/sellers/1/orders?status=PAID&buyer=sofia"
```

## Business Assumptions

- PostgreSQL is the default runtime database and Flyway owns schema changes; H2 remains an explicit fast path for local demos and tests.
- Order items snapshot product title and unit price because orders are historical facts.
- Questions belong to an order and may also reference a specific product.
- Answering and resolving are different actions: a seller can reply while keeping the issue open.
- Priority scoring is deliberately explainable rather than opaque, so operations teams can audit each result.
- Order status transitions follow a valid lifecycle: CREATED -> PAID -> SHIPPED -> DELIVERED, with CANCELLED as a terminal state from CREATED or PAID.

## Priority Scoring Explanation

```txt
priorityScore = waitingTimeScore + orderValueScore + keywordScore + productCategoryScore
```

| Factor | Rule |
| --- | --- |
| Waiting time | Older unresolved questions receive more points. |
| Order value | Higher-value orders receive more points. |
| Keywords | Words such as `urgent`, `refund`, `cancel`, `not received`, and `broken` increase priority. |
| Product category | Electronics and home products add category weight. |

| Score | Priority |
| --- | --- |
| `0-39` | `LOW` |
| `40-69` | `MEDIUM` |
| `70-89` | `HIGH` |
| `90+` | `CRITICAL` |

The response includes priority reasons so the dashboard can show why a question is important.

## Error Handling

All error responses follow a consistent JSON structure:

```json
{
  "timestamp": "2026-08-03T15:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "answer must not be blank",
  "path": "/api/questions/1/answer"
}
```

| Status | When |
| --- | --- |
| `400` | Invalid request body, malformed JSON, invalid query/path parameter, constraint violation, or invalid status transition. |
| `404` | Resource not found (order, question, seller). |
| `500` | Unexpected server error. |

Malformed JSON and invalid query, path, or date parameters use the same structure and return `400` rather than being treated as unexpected server failures. Error messages are safe for clients and do not expose parser internals.

## Tests

```bash
# Backend focused persistence test
cd backend && ./mvnw -q -Dtest=PersistenceIntegrationTest test

# Backend full suite
cd backend && ./mvnw test

# Frontend tests
cd frontend && npm run test

# Frontend production build
cd frontend && npm run build
```

| Layer | Framework | Coverage |
| --- | --- | --- |
| Backend unit | JUnit 5 + Mockito | Priority calculator, order service, question service |
| Backend integration | MockMvc + Spring Boot Test | Main API happy paths, validation, error codes, Flyway schema, repository persistence |
| Frontend unit | vitest + @testing-library/react | API clients, filters, tables, badges, components |

## Notification Extensibility

Notifications depend on `NotificationChannel`, not on a concrete delivery provider. The current implementation simulates email delivery, while future channels such as Slack or SMS can be added by implementing the same interface and wiring it into the notification service.

## Architecture Rationale

- Feature-based backend packages make orders, questions, and notifications easy to discuss independently.
- The frontend mirrors domain capabilities with `features/orders`, `features/questions`, and shared API utilities.
- DTOs keep HTTP responses separate from JPA entities.
- The priority calculator is isolated so scoring can be tuned without rewriting controllers or persistence.
- The notification abstraction follows dependency inversion and keeps question workflows provider-agnostic.

## Tradeoffs and Future Improvements

| Tradeoff | Why it is acceptable for the MVP | Future improvement |
| --- | --- | --- |
| PostgreSQL persistence | Production-like durability and explicit schema history. | Add operational backups and deployment-managed credentials. |
| H2 local/test profile | Fast reviewer setup and deterministic seed data. | Keep it limited to demo and automated test feedback loops. |
| Console email simulation | Demonstrates the integration point without external credentials. | Add real email, Slack, or SMS adapters. |
| Simple scoring rules | Explainable and interview-friendly. | Move weights to configuration and add analytics feedback. |
| Minimal frontend state management | Keeps the MVP focused on API integration. | Add query caching and optimistic mutations if the UI grows. |

## Documentation

- [Technical design](docs/technical-design.md)
