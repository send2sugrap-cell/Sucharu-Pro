# Domain Event Architecture

## 1. Overview
The Sucharu Pro event architecture provides an asynchronous, decoupled, and immutable event layer for commercial printing operations.

Events capture indisputable historical facts (e.g. `OrderCreated`, `ProductionStarted`, `QcPassed`, `StockIssued`, `DeliveryDelivered`, `PaymentReceived`).

## 2. Dispatch Pipeline

```
Use Case / Domain Operation
         │
         ▼
Construct Domain Event (Pure Kotlin Model)
         │
         ▼
Wrap in Canonical EventEnvelope<T>
(Inject Server-Authoritative ProjectId, Actor, Trace Context)
         │
         ▼
DomainEventDispatcher
  ├── 1. Validate Aggregate Stream Version
  ├── 2. Verify Schema Version Match
  ├── 3. Evaluate Consumer Idempotency Store
  └── 4. Invoke Authorized Consumer & Record Audit Record
```

## 3. Publisher & Consumer Contracts
- **`DomainEventPublisher`**: Clean interface to publish single or batch events.
- **`DomainEventConsumer<T>`**: Strongly typed consumer interface declaring `consumerId`, `supportedEventType`, and `supportedVersion`.
- **`EventFailureClassification`**: Clear classification of transient (retryable) vs permanent (non-retryable) failures.
