package com.sucharu.sucharupro.domain.model.delivery.governance

/**
 * Aggregate root representing an individual delivery governance exception or risk alert.
 */
data class DeliveryGovernanceAlert(
    val alertId: String,
    val projectId: String,
    val category: DeliveryGovernanceAlertCategory,
    val severity: DeliveryGovernanceAlertSeverity,
    val referenceType: String,
    val referenceId: String,
    val title: String,
    val description: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val status: DeliveryGovernanceAlertStatus = DeliveryGovernanceAlertStatus.OPEN,
    val acknowledgedBy: String? = null,
    val acknowledgedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val resolutionNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(alertId.isNotBlank()) { "Alert ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(referenceType.isNotBlank()) { "Reference Type cannot be blank." }
        require(referenceId.isNotBlank()) { "Reference ID cannot be blank." }
        require(title.isNotBlank()) { "Title cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(detectedAt > 0) { "Detected timestamp must be positive." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp ($updatedAt) cannot precede creation ($createdAt)." }

        if (status == DeliveryGovernanceAlertStatus.ACKNOWLEDGED) {
            require(!acknowledgedBy.isNullRespBlank()) { "Acknowledged By cannot be blank for ACKNOWLEDGED status." }
            require(acknowledgedAt != null && acknowledgedAt > 0) { "Acknowledged timestamp must be positive for ACKNOWLEDGED status." }
        }

        if (status == DeliveryGovernanceAlertStatus.RESOLVED) {
            require(!resolvedBy.isNullRespBlank()) { "Resolved By cannot be blank for RESOLVED status." }
            require(resolvedAt != null && resolvedAt > 0) { "Resolved timestamp must be positive for RESOLVED status." }
            require(!resolutionNotes.isNullRespBlank()) { "Resolution notes cannot be blank for RESOLVED status." }
        }

        if (status == DeliveryGovernanceAlertStatus.DISMISSED) {
            require(!resolvedBy.isNullRespBlank()) { "Dismissed By (resolvedBy) cannot be blank for DISMISSED status." }
            require(resolvedAt != null && resolvedAt > 0) { "Dismissed timestamp must be positive for DISMISSED status." }
            require(!resolutionNotes.isNullRespBlank()) { "Dismissal reason (resolutionNotes) cannot be blank for DISMISSED status." }
        }
    }
}

private fun String?.isNullRespBlank(): Boolean = this == null || this.trim().isEmpty()
