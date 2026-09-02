package com.sucharu.sucharupro.domain.model.qc

/**
 * Immutable audit log record for Quality Control activities in Sucharu Pro ERP.
 */
data class QcActivityEvent(
    val eventId: String,
    val qcId: String,
    val productionJobId: String,
    val actorId: String? = null,
    val actorName: String? = null,
    val activityType: QcActivityType,
    val timestamp: String,
    val notes: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(qcId.isNotBlank()) { "QC ID cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
