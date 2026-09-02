# Sucharu Pro Event Ordering & Consumer Idempotency

## 1. Aggregate Stream Ordering Guarantees
- **Monotonic Aggregate Versioning**: Each event envelope carries `aggregateVersion` strictly tracking the state version of the emitting business entity.
- **Database Engine Unique Index**:
  ```sql
  CREATE UNIQUE INDEX idx_event_store_stream_version 
  ON event_store(project_id, aggregate_type, aggregate_id, aggregate_version);
  ```
- **Dispatcher In-Order Execution**: The `OutboxDispatcher` sorts pending records by `aggregate_version ASC` and defers subsequent version dispatches if a preceding version failed or is awaiting retry.

## 2. Consumer Idempotency (`PostgresEventIdempotencyStore`)
Because distributed event delivery is **at-least-once**, consumers must be idempotent.

The table `event_processing_records` tracks consumer execution:
- Primary key / unique constraint: `(project_id, consumer_id, event_id)`
- Lifecycle states: `PROCESSING` $\rightarrow$ `PROCESSED` or `FAILED`
- Method `isProcessed(eventId, consumerId, projectId)`: Checks if the consumer already completed processing this exact event.
- If already processed, duplicate delivery is safely ignored without executing duplicate business effects.
