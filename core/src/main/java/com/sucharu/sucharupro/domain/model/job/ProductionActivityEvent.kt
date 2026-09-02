package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Immutable audit activity event recording chronological production events on a Production Job.
 */
data class ProductionActivityEvent(
    val eventId: String,
    val jobId: String,
    val stageId: String? = null,
    val stageType: ProductionStageType? = null,
    val operatorId: String? = null,
    val operatorName: String? = null,
    val eventType: ProductionActivityType,
    val message: String? = null,
    val timestamp: String,
    val createdBy: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(jobId.isNotBlank()) { "Job ID cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
