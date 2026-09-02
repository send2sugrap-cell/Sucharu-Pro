package com.sucharu.sucharupro.domain.model.design

/**
 * Immutable audit activity event recording chronological actions on a [DesignProject].
 */
data class DesignActivityEvent(
    val eventId: String,
    val projectId: String,
    val productionJobId: String,
    val designerId: String? = null,
    val designerName: String? = null,
    val eventType: DesignActivityType,
    val message: String? = null,
    val timestamp: String,
    val createdBy: String? = null
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}
