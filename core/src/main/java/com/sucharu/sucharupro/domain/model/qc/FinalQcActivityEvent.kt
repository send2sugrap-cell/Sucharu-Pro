package com.sucharu.sucharupro.domain.model.qc

/**
 * Immutable audit activity event for Final QC & Production Release actions (Module 06 Step 07).
 */
data class FinalQcActivityEvent(
    val eventId: String,
    val projectId: String,
    val productionJobId: String,
    val finalQcId: String,
    val actorId: String,
    val actorName: String? = null,
    val activityType: FinalQcActivityType,
    val notes: String? = null,
    val timestamp: String,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(finalQcId.isNotBlank()) { "Final QC ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
