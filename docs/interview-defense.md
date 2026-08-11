# Platform Design Guide

This document captures the key design questions for the Commerce Operations Platform, along with concise answers and talking points.

## General Approach

- Lead with the **why**, not the **what**.
- Admit tradeoffs explicitly — it shows maturity.
- Connect every decision to a business reason, not just technical preference.
- If you don't know something, say so and explain how you'd figure it out.

---

## 1. Why did you choose this stack?

**Suggested answer:**

> I chose Java 21 with Spring Boot for a modern, productive REST backend, and React + TypeScript for a typed frontend that integrates cleanly with REST APIs. Vite provides fast development feedback.

**Talking points:**
- The stack supports a maintainable full-stack product.
- TypeScript adds safety without slowing development.
- Spring Boot provides production-ready features out of the box.

---

## 2. Why PostgreSQL with an H2 test path?

**Suggested answer:**

> PostgreSQL is the default runtime database and Flyway makes schema changes explicit and reviewable. H2 remains available through the `local` and `test` profiles so feedback stays fast and deterministic.

**Talking points:**
- Deterministic seed data on every startup.
- No installation required for evaluators.
- The same versioned migration is exercised against H2 tests and is intended for PostgreSQL runtime deployments.

---

## 3. How does the priority scoring work and why those factors?

**Suggested answer:**

> The business didn't define what "important" means, so I built an explainable scoring function with four independent factors:

```txt
priorityScore = waitingTimeScore + orderValueScore + keywordScore + productCategoryScore
```

> Waiting time matters because old unanswered questions damage trust. Order value matters because high-value customers need faster response. Keywords like "urgent" or "broken" signal severity. Product category matters because electronics issues tend to be more complex.

> The score maps to bands: LOW (0-39), MEDIUM (40-69), HIGH (70-89), CRITICAL (90+). Each result includes reason strings so the queue is auditable.

**Talking points:**
- Deliberately NOT machine learning — it's overkill for this scope.
- Easy to tune weights without rewriting code.
- Transparent to the business: they can challenge any score.

---

## 4. Why are answering and resolving separate actions?

**Suggested answer:**

> In real customer support, replying to a buyer doesn't close the case. The seller might answer with "we're looking into it" — that's ANSWERED. The case isn't RESOLVED until the buyer confirms or the issue is fully closed. Modeling this as two separate states gives Operations visibility into what's actually done vs. what's just been responded to.

**Talking points:**
- Single boolean (open/closed) doesn't map to real support workflows.
- Operations wants to see unresolved questions — RESOLVED is the true "done" state.
- Common pattern in ticketing systems (Jira, Zendesk).

---

## 5. Explain the notification system design.

**Suggested answer:**

> Notifications depend on a `NotificationChannel` interface, not on a concrete provider. The current implementation is `EmailNotificationChannel` that logs to console. When Marketing wants Slack next week, I implement `SlackNotificationChannel` and wire it in. The question workflow doesn't change.

```java
public interface NotificationChannel {
    void send(Notification notification);
}
```

> This follows the Open/Closed Principle: open for extension, closed for modification.

**Talking points:**
- Dependency inversion: business logic depends on abstraction.
- Adding Slack or SMS is one new class, not a refactor.
- Console logging is intentional — no external credentials needed for the demo.

---

## 6. Why did you organize the code by feature instead of by layer?

**Suggested answer:**

> Global layer organization (all controllers together, all services together) makes sense for tiny apps, but it scales poorly. When I need to discuss "orders," I have to look in four different packages. Feature-based organization puts domain, application, infrastructure, and API for orders in one place. It mirrors how the business thinks about capabilities.

**Talking points:**
- Same pattern in both backend and frontend.
- Easy to explain: "orders" is one concept, not scattered files.
- If the app grows, each feature can evolve independently.

---

## 7. What would you do differently if this were a production system?

**Suggested answer:**

> Three things:

> 1. **Persistence operations**: Add deployment-managed credentials, backups, and migration observability.

> 2. **Configuration**: Move priority weights and thresholds to `application.yml` so the business can tune them without redeployment.

> 3. **Auth hardening**: The MVP has a minimal boundary: Spring Security HTTP Basic with configuration-driven seller identities and ownership checks. For production I would replace in-memory identities with an external identity provider, short-lived tokens, secret rotation, and centralized tenant policy enforcement.

**Talking points:**
- Shows you know the difference between a portfolio boundary and production identity infrastructure.
- Doesn't criticize the current approach — it's appropriate for the scope.
- Concrete improvements, not vague "I'd add more tests."

---

## 8. How do you make the backend observable without exposing internals?

**Suggested answer:**

> I added Spring Boot Actuator but expose public `GET /actuator/health` and `GET /actuator/info`; metrics require seller authentication, and sensitive endpoints such as `/actuator/env`, `/actuator/beans`, and `/actuator/loggers` remain unavailable over the web. State-changing browser requests also require a cookie-backed CSRF token, so Basic authentication alone cannot authorize a cross-site POST.

> Each request accepts a bounded, character-allowlisted `X-Request-Id` or generates a UUID. The selected ID is returned in the response, stored in MDC, and included in a single request log with method, path, status, and duration. Bodies and secret-bearing headers are deliberately excluded.

**Talking points:**
- Correlation IDs let an operator connect a client error to server logs.
- Validation prevents unbounded or log-injection-prone header values.
- The endpoint allowlist is safer than exposing every Actuator capability by default.

---

## 9. How do you handle errors in the API?

**Suggested answer:**

> Every error response follows a consistent JSON structure:

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Validation Failed",
  "message": "Answer must not be blank",
  "path": "/api/questions/1/answer"
}
```

> Bean Validation (`@NotBlank`, `@NotNull`) catches bad input at the boundary. Invalid status transitions return 400 with a clear message. Missing resources return 404. Unexpected errors return 500 but never leak stack traces to the client.

**Talking points:**
- Consistent contract makes frontend error handling easier.
- `ApiExceptionHandler` centralizes all error logic.
- No 500s for what should be 400s.

---

## 10. How do order status transitions work?

**Suggested answer:**

> Orders follow a valid lifecycle:

```txt
CREATED -> PAID -> SHIPPED -> DELIVERED
CREATED -> CANCELLED
PAID -> CANCELLED
```

> DELIVERED and CANCELLED are terminal states. The `OrderService.transitionStatus()` method validates transitions against an allowed-transitions map before applying them. Invalid transitions (like DELIVERED -> CREATED) return 400.

**Talking points:**
- Prevents data corruption from invalid state changes.
- Easy to extend: add new statuses or transitions by updating the map.
- Business rules are explicit, not hidden in UI logic.

---

## 11. Tell me about your test strategy.

**Suggested answer:**

> The test suite covers backend unit and integration behavior plus frontend API and component behavior. The integration tests also prove authentication and seller ownership, correlation ID propagation, replacement of unsafe IDs, and the Actuator endpoint allowlist.

> Backend: Unit tests for the priority calculator (11 tests covering all score bands), order service (filters + transitions), question service (lifecycle), and notification log privacy. Integration tests with MockMvc for authenticated ownership, unauthenticated rejection, cross-seller `403` responses, main API happy paths, validation errors, and 404s.

> Frontend: API client tests for correct endpoint calls and query param building. Component tests for filters, tables, badges, and loading states.

> I focused on behavior-first testing: every test asserts something observable, not implementation details.

**Talking points:**
- Priority calculator has the most tests because it's the most complex business logic.
- Integration tests prove the API contract works end-to-end.
- Frontend tests verify the UI renders correctly with data.

---

## 12. Why use a transactional outbox instead of sending the event in the request?

**Suggested answer:**

> The order state and its event are committed together. The request does not call a remote provider, so a provider outage cannot roll back or slow the order update, and a successful order cannot lose its event between the database write and an asynchronous handoff.

**Talking points:**
- `V2__create_transactional_outbox.sql` owns the event and delivery tables.
- A locked, bounded poll claims due rows and uses a lease so another worker can recover abandoned work.
- Five attempts use exponential backoff before the row becomes `FAILED` for operational inspection.
- This is intentionally not exactly-once delivery. The local unique delivery record handles normal duplicate claims; a real provider must support the event UUID as an idempotency key for the remote side.
- An external broker and a second service are future scaling choices, not hidden dependencies in this slice.

---

## 13. How do you prevent duplicate event side effects?

**Suggested answer:**

> Each outbox event has a UUID, and `outbox_deliveries.event_id` is unique. The handler checks that durable record before invoking the notifier and writes it in the same local transaction. A redelivered completed event is therefore a no-op.

The remaining external-call crash window is documented rather than hidden: provider adapters must pass the UUID as their own idempotency key. That is the honest boundary of an in-process PostgreSQL worker.

---

## Bonus: Quick Fire Questions

| Question | Short Answer |
| --- | --- |
| What Java version? | 21 — modern LTS baseline. |
| Why Spring Boot over raw Spring? | Auto-configuration, embedded server, production-ready defaults. |
| What's the H2 console for? | Quick data inspection during the explicit `local` demo profile. |
| Which operational endpoints are public? | `/actuator/health` and `/actuator/info`; `/actuator/metrics` requires authentication and sensitive Actuator endpoints are not exposed. |
| How do you trace a request? | Use the returned `X-Request-Id` to find the matching structured request log. |
| Why Vite over Create React App? | Faster, modern, better DX. CRA is deprecated. |
| What does the Vite proxy do? | Forwards `/api` requests to backend during development. |
| How do you handle CORS? | Vite proxy avoids CORS entirely in dev. In production I'd configure `@CrossOrigin` or a reverse proxy. |
| What's the seed data setup? | `CommandLineRunner` bean in `DataSeedConfig` that inserts test data on startup. |
| Can I add a new notification channel? | Yes — implement `NotificationChannel` interface and register it as a Spring bean. |

---

## Final Tips

1. **Be honest about tradeoffs.** Interviewers respect "I chose X because Y, and I know Z would be better in production" more than pretending everything is perfect.

2. **Lead with business reasoning.** "The business needs X" is stronger than "I thought it was cool."

3. **Show you can extend the system.** When asked "what if Marketing wants Slack," walk through adding `SlackNotificationChannel` — it takes 2 minutes to explain and proves the design works.

4. **Don't apologize for the scope.** H2 local/test support, console notification simulation, and configuration-driven demo credentials are intentional portfolio boundaries, while the API still enforces authentication and seller ownership.

5. **Know your tests.** If they ask "what does the priority calculator test?" you should be able to list the scenarios without looking at code.
