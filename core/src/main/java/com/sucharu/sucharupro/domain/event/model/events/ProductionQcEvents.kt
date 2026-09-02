package com.sucharu.sucharupro.domain.event.model.events

import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType

/**
 * Emitted when production begins for a job or work order.
 */
data class ProductionStartedEvent(
    val jobId: String,
    val orderId: String,
    val workstationId: String,
    val operatorId: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.PRODUCTION_STARTED
    override val aggregateId: String get() = jobId
    override val aggregateType: String get() = "PRODUCTION"

    init {
        require(jobId.isNotBlank()) { "jobId cannot be blank" }
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
    }
}

/**
 * Emitted when a production job completes.
 */
data class ProductionCompletedEvent(
    val jobId: String,
    val orderId: String,
    val completedQuantity: Int,
    val wastageQuantity: Int = 0,
    override val aggregateVersion: Long
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.PRODUCTION_COMPLETED
    override val aggregateId: String get() = jobId
    override val aggregateType: String get() = "PRODUCTION"

    init {
        require(jobId.isNotBlank()) { "jobId cannot be blank" }
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
        require(completedQuantity >= 0) { "completedQuantity cannot be negative" }
        require(wastageQuantity >= 0) { "wastageQuantity cannot be negative" }
    }
}

/**
 * Emitted when quality inspection passes for a batch.
 */
data class QcPassedEvent(
    val inspectionId: String,
    val jobId: String,
    val inspectedBy: String,
    val sampleSize: Int,
    val passRatePercentage: Double,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.QC_PASSED
    override val aggregateId: String get() = inspectionId
    override val aggregateType: String get() = "QC"

    init {
        require(inspectionId.isNotBlank()) { "inspectionId cannot be blank" }
        require(jobId.isNotBlank()) { "jobId cannot be blank" }
        require(sampleSize > 0) { "sampleSize must be positive" }
        require(passRatePercentage in 0.0..100.0) { "passRatePercentage must be between 0.0 and 100.0" }
    }
}

/**
 * Emitted when quality inspection fails for a batch.
 */
data class QcFailedEvent(
    val inspectionId: String,
    val jobId: String,
    val inspectedBy: String,
    val defectReason: String,
    val rejectedQuantity: Int,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.QC_FAILED
    override val aggregateId: String get() = inspectionId
    override val aggregateType: String get() = "QC"

    init {
        require(inspectionId.isNotBlank()) { "inspectionId cannot be blank" }
        require(jobId.isNotBlank()) { "jobId cannot be blank" }
        require(defectReason.isNotBlank()) { "defectReason cannot be blank" }
        require(rejectedQuantity >= 0) { "rejectedQuantity cannot be negative" }
    }
}
