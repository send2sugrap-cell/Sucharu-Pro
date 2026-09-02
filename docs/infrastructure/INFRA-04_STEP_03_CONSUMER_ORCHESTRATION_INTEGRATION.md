# INFRA-04 Step 03: Production-Grade Event Consumer Orchestration & Integration Layer

## Overview
INFRA-04 Step 03 implements the production-grade consumer orchestration and safe integration layer for **Sucharu Pro — Commercial Printing ERP**. It reliably routes domain events from the transactional outbox and persistent event store to:
1. **Internal Domain Consumers** (Inventory, Order, Finance, Production, QC)
2. **Notification System** (In-App, Push, SMS, Email)
3. **Real-Time Streaming** (WebSocket / SSE tenant channels)
4. **n8n Automation Engine** (HMAC-SHA256 signed webhooks)
5. **Sucharu AI Agent Platform** (Strict capability-based read-only boundaries)
6. **Integration Delivery Repository** (`integration_delivery_records` with PostgreSQL RLS)

---

## Architectural Components

### 1. Consumer Orchestration Engine
- `ConsumerSubscription`: Explicit, strongly-typed registration specifying `consumerId`, `supportedEventType`, `supportedVersion`, `integrationType`, `executionMode`, `orderingRequirement`, `idempotencyRequired`, and `maxRetries`. Wildcard subscriptions (`*`) are strictly prohibited.
- `EventConsumerRegistry`: Thread-safe registry mapping versioned domain events to active subscriptions.
- `EventConsumerExecutionEngine`: Isolated execution runtime that enforces idempotency checks in `PostgresEventIdempotencyStore`, classifies outcomes, and persists delivery attempts in `PostgresIntegrationDeliveryRepository`.
- `EventConsumerRouter`: Central dispatcher evaluating envelopes and collecting individual consumer outcomes into `RouterDispatchReport`.

### 2. Multi-Channel Notification Integration
- `NotificationIntentResolver`: Translates domain event payloads into human-readable, sanitized `NotificationIntent`s (e.g. `ORDER_CREATED`, `DELIVERY_DISPATCHED`, `PAYMENT_RECEIVED`).
- `NotificationDispatchService`: Resolves recipients, enforces user channel preferences and quiet hours, and dispatches across registered `NotificationProvider`s (`IN_APP`, `PUSH`, `EMAIL`, `SMS`).
- **Idempotency**: Keyed by `(projectId, eventId, recipientId, channel)`.
- **Zero Secrets**: Payment secrets, auth tokens, and internal credentials are never emitted in notification bodies.

### 3. Real-Time Event Streaming
- `RealTimeSubscriptionRegistry`: Manages active WebSocket / SSE client sessions with strict tenant scoping.
- **Tenant Topic Partitioning**: Topics follow canonical format `tenant.{projectId}.{aggregateType}.{aggregateId}`. Cross-tenant subscriptions are rejected at the registry boundary.
- `RealTimeDeliveryService`: Emits data-minimized `RealTimeEventFrame`s. Sensitive authentication and security events are strictly filtered from stream frames.
- **Safe Disconnection**: Client disconnects cleanly unregister without causing database retries.

### 4. n8n Automation Integration
- `N8nPayloadBuilder`: Converts domain envelopes into sanitized `N8nWebhookPayload`s with HMAC-SHA256 signature verification headers (`X-Sucharu-Signature`).
- `N8nAutomationDispatcher`: Handles HTTP webhooks with granular failure classification:
  - `5xx` / Connection timeouts $\rightarrow$ `TRANSIENT` (retryable)
  - `4xx` (Bad Request / Unauthorized) $\rightarrow$ `VALIDATION` / `SECURITY` (non-retryable)
  - Security events $\rightarrow$ `SECURITY` (blocked)

### 5. Sucharu AI Agent Platform Integration
- `AiAgentEventConsumer`: Secure read-only consumer bridging ERP domain events to AI Agent tools.
- `AiAgentEventBoundary.evaluateAccess()`: Enforces machine principal role validation, strict tenant matching, and capability mapping:
  - `ORDER_CREATED` / `ORDER_UPDATED` $\rightarrow$ `AI_READ_ORDER_CONTEXT`
  - `CUSTOMER_CREATED` $\rightarrow$ `AI_READ_CUSTOMER_CONTEXT`
  - `AFFILIATE_COMMISSION_EARNED` $\rightarrow$ `AI_READ_AFFILIATE_CONTEXT`
  - `INVOICE_CREATED` $\rightarrow$ `AI_READ_INVOICE`
- **Security Protections**: Direct database mutations are impossible; sensitive security events and payment tokens are blocked and stripped. Human confirmation metadata is preserved for high-impact AI workflows.

### 6. Persistence & Observability
- **Flyway Migration `V20260906__create_integration_delivery_records.sql`**: Dedicated table with composite primary key `(project_id, delivery_id)`, unique idempotency index `(project_id, consumer_id, event_id)`, and Row-Level Security (RLS) enforcement.
- `IntegrationMetrics`: Tracks real-time throughput, latency, consumer executions, duplicate suppressions, and channel-specific delivery counts.
- `IntegrationAuditLogger`: Immutable audit log with automatic secret redaction.

---

## Verification & Test Results
- **123/123 Unit Tests Passing**:
  - `ConsumerOrchestrationTest` (4 tests)
  - `NotificationIntegrationTest` (3 tests)
  - `RealTimeIntegrationTest` (4 tests)
  - `N8nIntegrationTest` (4 tests)
  - `AiAgentEventIntegrationTest` (4 tests)
  - `IntegrationSecurityTest` (3 tests)
  - `IntegrationConcurrencyTest` (2 tests)
  - Full Outbox, Event Store, Serialization, and Security suite
- **Android Compilation**: `./gradlew.bat assembleDebug` completed successfully (`BUILD SUCCESSFUL in 57s`).
