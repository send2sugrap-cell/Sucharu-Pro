package com.sucharu.sucharupro.domain.event.idempotency

/**
 * Execution status for event consumption.
 */
enum class EventProcessingStatus {
    IN_FLIGHT,
    PROCESSED,
    FAILED,
    SKIPPED
}

/**
 * Immutable audit record of an event's processing lifecycle by a specific consumer.
 */
data class EventProcessingRecord(
    val eventId: String,
    val consumerId: String,
    val projectId: String,
    val processedAt: Long = System.currentTimeMillis(),
    val status: EventProcessingStatus = EventProcessingStatus.PROCESSED,
    val failureReason: String? = null,
    val executionDurationMs: Long = 0L
) {
    init {
        require(eventId.isNotBlank()) { "eventId cannot be blank" }
        require(consumerId.isNotBlank()) { "consumerId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
    }
}

/**
 * Idempotency tracking store for domain event consumers.
 *
 * Guarantees that at-least-once delivery does not lead to duplicate side effects.
 */
interface EventIdempotencyStore {
    /**
     * Checks if the event has already been successfully processed by the given consumer within the tenant project.
     */
    suspend fun isProcessed(eventId: String, consumerId: String, projectId: String): Boolean

    /**
     * Records that an event has been processed (or failed) by a consumer.
     */
    suspend fun recordProcessing(record: EventProcessingRecord)

    /**
     * Retrieves existing processing record if available.
     */
    suspend fun getRecord(eventId: String, consumerId: String, projectId: String): EventProcessingRecord?
}
