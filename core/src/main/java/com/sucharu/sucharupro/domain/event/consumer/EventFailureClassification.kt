package com.sucharu.sucharupro.domain.event.consumer

/**
 * Categorical failure classification for domain event processing (INFRA-04 Step 01).
 *
 * Distinguishes retryable failures from permanent rejections to guarantee at-least-once safety
 * without poisoned queues or infinite loops.
 */
enum class EventFailureClassification(
    val isRetryable: Boolean
) {
    /**
     * Downstream transient failure (e.g. database connection timeout, network glitch, temporary 503).
     * Retry safe.
     */
    TRANSIENT(isRetryable = true),

    /**
     * Permanent failure due to unrecoverable business logic error or unsupported data format.
     * Not retryable.
     */
    NON_RETRYABLE(isRetryable = false),

    /**
     * Authorization / tenant security violation (e.g. cross-tenant delivery attempt, unauthorized AI agent subscription).
     * Hard blocked, never retryable.
     */
    SECURITY(isRetryable = false),

    /**
     * Event payload schema or invariant validation error.
     * Hard blocked, never retryable.
     */
    VALIDATION(isRetryable = false),

    /**
     * Event has already been processed by this consumer (idempotency hit).
     * Handled cleanly without side-effects, not retryable.
     */
    DUPLICATE(isRetryable = false),

    /**
     * Out-of-order or stale aggregate version received where an older event arrives after a newer state.
     * Discarded or routed to conflict resolution, not retryable.
     */
    STALE_VERSION(isRetryable = false)
}

/**
 * Sealed result returned by a [DomainEventConsumer] upon processing an event.
 */
sealed class EventConsumerResult {
    /**
     * Successfully processed event.
     */
    data class Success(
        val message: String = "SUCCESS",
        val processedAt: Long = System.currentTimeMillis()
    ) : EventConsumerResult()

    /**
     * Failed event processing with structured classification.
     */
    data class Failure(
        val reason: String,
        val classification: EventFailureClassification,
        val cause: Throwable? = null,
        val failedAt: Long = System.currentTimeMillis()
    ) : EventConsumerResult() {
        val isRetryable: Boolean get() = classification.isRetryable
    }

    /**
     * Skipped execution because event is already processed (idempotency) or not relevant.
     */
    data class Skipped(
        val reason: String,
        val classification: EventFailureClassification = EventFailureClassification.DUPLICATE
    ) : EventConsumerResult()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
    val isSkipped: Boolean get() = this is Skipped
}
