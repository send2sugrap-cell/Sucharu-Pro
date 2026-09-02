# Event Consumer Orchestration Architecture

## Core Concepts
The consumer orchestration layer routes versioned domain events to registered internal and external consumers while guaranteeing idempotency, failure isolation, and strict tenant separation.

## Components
1. **`ConsumerSubscription`**:
   - `consumerId`: Unique consumer identifier (e.g. `inventory.order_created`).
   - `supportedEventType`: Strongly-typed `DomainEventType`.
   - `supportedVersion`: Schema version string (e.g. `v1`).
   - `integrationType`: `INTERNAL`, `NOTIFICATION`, `REAL_TIME`, `N8N`, or `AI_AGENT`.
   - `executionMode`: `SYNC` or `ASYNC`.
   - `orderingRequirement`: `AGGREGATE_STRICT` or `UNORDERED`.
   - `idempotencyRequired`: Boolean flag controlling idempotency checks.

2. **`EventConsumerRegistry`**:
   - Thread-safe registry mapping `(eventType, version)` pairs to subscriptions.
   - Prevents wildcard subscriptions (`*`).
   - Rejects conflicting duplicate registrations.

3. **`EventConsumerExecutionEngine`**:
   - Performs deduplication checks using `PostgresEventIdempotencyStore`.
   - Executes the consumer safely with exception trapping.
   - Records processing status in idempotency store.
   - Writes persistent audit records to `PostgresIntegrationDeliveryRepository`.

4. **`EventConsumerRouter`**:
   - Dispatches envelopes across matching subscriptions.
   - Collects per-consumer outcomes into `RouterDispatchReport`.
