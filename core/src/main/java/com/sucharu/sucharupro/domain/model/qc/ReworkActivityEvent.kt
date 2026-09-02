package com.sucharu.sucharupro.domain.model.qc

/**
 * Immutable audit trail event for QC Rework lifecycle mutations (Module 06 Step 05).
 */
data class ReworkActivityEvent(
    val eventId: String,
    val reworkId: String,
    val productionJobId: String,
    val projectId: String,
    val defectId: String? = null,
    val actorId: String? = null,
    val actorName: String? = null,
    val activityType: ReworkActivityType,
    val timestamp: String,
    val notes: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(reworkId.isNotBlank()) { "Rework ID cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
