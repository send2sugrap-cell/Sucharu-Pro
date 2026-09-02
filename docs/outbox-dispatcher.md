# Sucharu Pro Outbox Dispatcher

## 1. Responsibilities
`OutboxDispatcher` is the asynchronous background engine that claims staged outbox records and delivers them to registered domain event consumers and decoupled system boundaries.

## 2. Dispatch Loop Flow
1. **Claim Pending Records**: Atomically claims up to `batchSize` records using `PostgresTransactionalOutboxStore.claimPendingRecords`.
2. **Reconstruct Event Envelopes**: Deserializes JSON payloads into strongly typed immutable `EventEnvelope` instances.
3. **Preserve Aggregate Stream Ordering**: Groups claimed events by aggregate stream (`$aggregateType:$aggregateId`). If version $N$ encounters a failure or retry, version $N+1$ for that same aggregate is deferred to avoid out-of-order execution.
4. **Deliver to Registered Consumers**: Dispatches envelopes to `DomainEventDispatcher`.
5. **Handle Consumer Results**:
   - `Success`: Marks record as `PUBLISHED` (terminal state) and records latency metrics.
   - `Failure` (`TRANSIENT`): Schedules retry using `scheduleRetry` with exponential backoff and jitter if `attempt_count < maxAttempts`.
   - `Failure` (`NON_RETRYABLE` or `attempt_count >= maxAttempts`): Quarantines record to `event_dead_letters` and sets outbox status to `DEAD_LETTER`.

## 3. Telemetry & Metrics
- Claim throughput, publication latency, retry count, and dead-letter count tracked via `OutboxMetrics`.
