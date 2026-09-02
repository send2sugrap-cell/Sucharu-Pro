# Domain Event Idempotency & Stream Ordering

## 1. At-Least-Once Delivery & Idempotency
Because distributed messaging systems provide at-least-once delivery guarantees, duplicate delivery is expected and handled gracefully.

### Processing Record
For each consumer execution, `EventIdempotencyStore` persists an `EventProcessingRecord`:
```kotlin
data class EventProcessingRecord(
    val eventId: String,
    val consumerId: String,
    val projectId: String,
    val processedAt: Long,
    val status: EventProcessingStatus,
    val failureReason: String? = null,
    val executionDurationMs: Long = 0L
)
```

Before consumer invocation, `isProcessed(eventId, consumerId, projectId)` is evaluated. If already processed, the duplicate event is skipped without triggering side effects.

## 2. Stream Ordering
Each aggregate stream enforces monotonic `aggregateVersion` ordering. If an older event arrives after a newer version is already recorded, it is classified as `EventFailureClassification.STALE_VERSION` and rejected.
