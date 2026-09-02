package com.sucharu.sucharupro.domain.model.qc

/**
 * Immutable audit trail event for QC Defect & Failure lifecycle mutations (Module 06 Step 04).
 */
data class QcDefectActivityEvent(
    val eventId: String,
    val defectId: String,
    val productionJobId: String,
    val actorId: String? = null,
    val actorName: String? = null,
    val activityType: QcDefectActivityType,
    val timestamp: String,
    val notes: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(defectId.isNotBlank()) { "Defect ID cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
