package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Append-only immutable audit event entity for QC Governance (Module 06 Step 10).
 */
data class QcGovernanceActivityEvent(
    val eventId: String,
    val projectId: String,
    val eventType: QcGovernanceActivityType,
    val targetId: String,
    val targetType: String,
    val actorId: String,
    val actorName: String? = null,
    val actorRole: String? = null,
    val description: String,
    val timestamp: String,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank" }
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(targetId.isNotBlank()) { "Target ID cannot be blank" }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank" }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank" }
    }
}
