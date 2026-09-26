package com.sucharu.sucharupro.data.api.model.communication

import kotlinx.serialization.Serializable

@Serializable
data class ProcessEventDispatchRequestDto(
    val eventId: String,
    val recipientId: String,
    val title: String,
    val message: String,
    val channel: String = "IN_APP",
    val idempotencyKey: String
)

@Serializable
data class CommunicationDispatchAttemptDto(
    val dispatchId: String,
    val eventId: String,
    val recipientId: String,
    val channel: String,
    val title: String,
    val renderedMessage: String,
    val deliveryStatus: String = "QUEUED",
    val attemptCount: Int = 1,
    val idempotencyKey: String,
    val dispatchedAt: String
)

@Serializable
data class CommunicationAutomationDispatchSummaryDto(
    val totalEventsProcessedCount: Int,
    val queuedCount: Int,
    val sentCount: Int,
    val deliveredCount: Int,
    val failedCount: Int,
    val retriedCount: Int,
    val isOutboxDecoupledDeliveryEnforced: Boolean = true,
    val activeDispatches: List<CommunicationDispatchAttemptDto> = emptyList(),
    val generatedAt: String
)
