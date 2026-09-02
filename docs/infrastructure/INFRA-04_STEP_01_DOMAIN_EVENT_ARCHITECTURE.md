# INFRA-04 STEP 01 — Domain Event & Event Envelope Foundation Architecture

**System**: Sucharu Pro — Commercial Printing ERP  
**Module**: INFRA-04 (Event Foundation & Asynchronous Communication Layer)  
**Step**: Step 01 (Production-Grade Domain Event & Event Envelope Foundation)  
**Status**: COMPLETE & VERIFIED  

---

## 1. Executive Summary

This architecture defines the canonical, production-grade Domain Event and Event Envelope foundation for Sucharu Pro. The event foundation serves as the common event integration layer connecting domain operations with authorized downstream consumers, notifications (in-app, email, SMS, push), real-time updates, n8n automations, and the Sucharu AI Agent Platform.

```
DOMAIN OPERATION
      ↓
DOMAIN EVENT (Immutable Fact)
      ↓
EVENT ENVELOPE (Canonical Container)
      ↓
EVENT DISPATCHER (Tenant Scoped & Idempotency Checked)
      ↓
AUTHORIZED CONSUMERS
      ↓
Notifications / AI Agent / n8n / Real-Time / Audit
```

---

## 2. Core Architectural Principles

1. **Facts, Not Commands**: Events represent business occurrences that have already taken place (e.g., `OrderCreated`, `PaymentReceived`). They never represent commands or mutating requests.
2. **Server-Authoritative Tenant Scoping**: Every event envelope is bound to a `projectId` verified against the authenticated tenant context. Cross-tenant event publication and consumption are denied.
3. **Immutability & Integrity**: `EventEnvelope` and all domain event models are pure Kotlin immutable data classes.
4. **Traceability**: All events propagate `correlationId` (spanning business workflows), `causationId` (identifying parent events), and `requestId` (tracking originating HTTP requests).
5. **Idempotency & At-Least-Once Delivery**: Downstream consumers track unique `(projectId, consumerId, eventId)` execution records to prevent duplicate side effects.
6. **Aggregate Stream Ordering**: Events carry `aggregateVersion` to detect stale or out-of-order deliveries.
7. **Domain Purity**: Core domain event models contain zero framework, UI, JDBC, or third-party broker dependencies.

---

## 3. Canonical Event Envelope (`EventEnvelope<T>`)

| Field | Type | Description |
|---|---|---|
| `eventId` | `String` (UUIDv4) | Globally unique, collision-resistant event identifier |
| `eventType` | `DomainEventType` | Strongly typed enum representing the domain event |
| `eventVersion` | `String` | Explicit schema version (e.g., `"v1"`) |
| `occurredAt` | `Long` | Monotonic epoch millisecond timestamp when the fact occurred |
| `publishedAt` | `Long` | Epoch millisecond timestamp when dispatched |
| `projectId` | `String` | Server-authoritative tenant project identifier |
| `aggregateType` | `String` | Domain aggregate classification (`"ORDER"`, `"CUSTOMER"`, etc.) |
| `aggregateId` | `String` | Primary aggregate business identity (`"ORD-1001"`, etc.) |
| `aggregateVersion` | `Long` | Monotonic aggregate version number |
| `actorType` | `PrincipalType` | Classification: `HUMAN`, `AI_AGENT`, `SYSTEM`, `PUBLIC` |
| `actorId` | `String` | Authoritative actor identifier |
| `principalType` | `PrincipalType` | Authoritative principal type |
| `correlationId` | `String` | Workflow correlation identifier |
| `causationId` | `String?` | Preceding event identifier |
| `requestId` | `String?` | Originating API request identifier |
| `source` | `String` | Originating subsystem identifier |
| `payload` | `T : DomainEvent` | Strongly typed domain event payload |
| `metadata` | `Map<String, String>` | Non-domain routing tags and tracing attributes |

---

## 4. Bounded Context Integration

### 4.1 AI Agent Boundary (`AiAgentEventBoundary`)
- Hard blocks AI agents from subscribing to sensitive security and raw financial events.
- Enforces strict tenant containment (`principal.projectId == envelope.projectId`).
- Requires explicit granular capabilities (e.g. `AI_READ_ORDER_CONTEXT`).
- Sanitizes metadata to prevent secret leakage.

### 4.2 n8n Automation Boundary (`N8nIntegrationBoundary`)
- Completely decoupled adapter producing sanitized `N8nWebhookPayload`.
- Hard-blocks internal security events from leaving the system boundary.
- Zero hardcoded URLs or external credentials in code.

### 4.3 Notification Readiness (`NotificationEventBoundary`)
- Maps domain event types to appropriate notification channels (`IN_APP`, `EMAIL`, `SMS`, `PUSH`).
- Constructs decoupled `NotificationIntent` objects ready for downstream dispatchers.

### 4.4 Real-Time Streaming Readiness (`RealTimeEventBoundary`)
- Prepares structured `RealTimeEventFrame` with canonical tenant topics (e.g., `tenant.sucharu_main.order.ORD-1001`) for WebSocket/SSE.

---

## 5. Persistence & Outbox Readiness

- **`EventStore`**: Append-only contract enforcing tenant isolation, duplicate detection, and aggregate stream ordering queries.
- **`TransactionalOutboxStore`**: Transactional outbox abstraction storing `OutboxEventRecord` inside the active business database transaction to guarantee zero lost events.
