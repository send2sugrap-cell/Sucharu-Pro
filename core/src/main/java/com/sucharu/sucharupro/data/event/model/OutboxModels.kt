package com.sucharu.sucharupro.data.event.model

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import java.util.UUID

/**
 * State machine for Transactional Outbox records (INFRA-04 Step 02).
 */
enum class OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    RETRY_SCHEDULED,
    DEAD_LETTER,
    CANCELLED;

    /**
     * Validates whether a state transition from `this` to [next] is legally permitted.
     */
    fun canTransitionTo(next: OutboxStatus): Boolean {
        if (this == next) return true
        return when (this) {
            PENDING -> next == PROCESSING || next == CANCELLED
            PROCESSING -> next == PUBLISHED || next == RETRY_SCHEDULED || next == DEAD_LETTER || next == CANCELLED
            RETRY_SCHEDULED -> next == PROCESSING || next == CANCELLED
            PUBLISHED -> false // Terminal state
            DEAD_LETTER -> next == RETRY_SCHEDULED || next == CANCELLED // Can be replayed or cancelled
            CANCELLED -> false // Terminal state
        }
    }
}

/**
 * Complete persistent record model matching the `event_outbox` PostgreSQL table.
 */
data class PersistentOutboxRecord(
    val outboxId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val eventId: String,
    val eventType: DomainEventType,
    val eventVersion: String = "v1",
    val aggregateType: String,
    val aggregateId: String,
    val aggregateVersion: Long = 1L,
    val status: OutboxStatus = OutboxStatus.PENDING,
    val attemptCount: Int = 0,
    val claimedByWorker: String? = null,
    val claimedAt: Long? = null,
    val leaseExpiresAt: Long? = null,
    val availableAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null,
    val nextAttemptAt: Long? = null,
    val publishedAt: Long? = null,
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val payloadJson: String,
    val metadata: Map<String, String> = emptyMap(),
    val correlationId: String,
    val causationId: String? = null,
    val requestId: String? = null,
    val actorType: PrincipalType,
    val actorId: String,
    val principalType: PrincipalType,
    val source: String = "sucharu-pro-backend"
) {
    init {
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(eventId.isNotBlank()) { "eventId cannot be blank" }
        require(aggregateType.isNotBlank()) { "aggregateType cannot be blank" }
        require(aggregateId.isNotBlank()) { "aggregateId cannot be blank" }
        require(aggregateVersion >= 1L) { "aggregateVersion must be >= 1" }
        require(attemptCount >= 0) { "attemptCount cannot be negative" }
        require(correlationId.isNotBlank()) { "correlationId cannot be blank" }
        require(actorId.isNotBlank()) { "actorId cannot be blank" }
    }
}

/**
 * Persistent dead-letter quarantine record matching the `event_dead_letters` PostgreSQL table.
 */
data class DeadLetterRecord(
    val deadLetterId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val outboxId: String,
    val eventId: String,
    val eventType: DomainEventType,
    val eventVersion: String = "v1",
    val aggregateType: String,
    val aggregateId: String,
    val payloadJson: String,
    val failureClassification: EventFailureClassification,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val attemptCount: Int,
    val firstFailureAt: Long,
    val finalFailureAt: Long = System.currentTimeMillis(),
    val correlationId: String,
    val causationId: String? = null,
    val requestId: String? = null,
    val replayedAt: Long? = null,
    val replayedBy: String? = null,
    val isResolved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(outboxId.isNotBlank()) { "outboxId cannot be blank" }
        require(eventId.isNotBlank()) { "eventId cannot be blank" }
        require(attemptCount >= 0) { "attemptCount cannot be negative" }
    }
}

/**
 * Configurable exponential backoff retry settings for outbox dispatch.
 */
data class RetryConfig(
    val maxAttempts: Int = 5,
    val initialBackoffMs: Long = 1000L,
    val maxBackoffMs: Long = 60000L,
    val multiplier: Double = 2.0,
    val jitterFactor: Double = 0.1
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
        require(initialBackoffMs > 0) { "initialBackoffMs must be > 0" }
        require(maxBackoffMs >= initialBackoffMs) { "maxBackoffMs must be >= initialBackoffMs" }
        require(multiplier >= 1.0) { "multiplier must be >= 1.0" }
        require(jitterFactor in 0.0..1.0) { "jitterFactor must be between 0.0 and 1.0" }
    }

    /**
     * Calculates the next backoff delay in milliseconds given the current attempt count.
     */
    fun calculateDelayMs(attempt: Int, randomFactor: Double = 0.0): Long {
        if (attempt <= 0) return 0L
        val exponential = (initialBackoffMs * Math.pow(multiplier, (attempt - 1).toDouble())).toLong()
        val capped = Math.min(exponential, maxBackoffMs)
        val jitter = (capped * jitterFactor * (if (randomFactor in 0.0..1.0) randomFactor else 0.5)).toLong()
        return capped + jitter
    }
}

/**
 * Worker claim representing locked outbox records under an active processing lease.
 */
data class OutboxWorkerClaim(
    val workerId: String,
    val claimedRecords: List<PersistentOutboxRecord>,
    val leaseExpiresAt: Long
)
