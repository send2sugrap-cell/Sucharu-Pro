package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Quality Alert entity representing a detected KPI breach or operational quality hazard (Module 06 Step 10).
 */
data class QcQualityAlert(
    val alertId: String,
    val projectId: String,
    val jobId: String? = null,
    val kpiType: QcGovernanceKpi,
    val currentValue: Double,
    val targetValue: Double,
    val thresholdValue: Double? = null,
    val severity: QcAlertSeverity,
    val status: QcAlertStatus = QcAlertStatus.DETECTED,
    val title: String,
    val message: String,
    val detectedAt: String,
    val acknowledgedAt: String? = null,
    val acknowledgedBy: String? = null,
    val resolvedAt: String? = null,
    val resolvedBy: String? = null,
    val detectedBy: String = "SYSTEM",
    val assignedTo: String? = null,
    val assignedToName: String? = null,
    val escalationLevel: QcEscalationLevel = QcEscalationLevel.NONE,
    val notes: String? = null
) {
    val isTerminal: Boolean get() = status.isTerminal

    init {
        require(alertId.isNotBlank()) { "Alert ID cannot be blank" }
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(title.isNotBlank()) { "Alert title cannot be blank" }
        require(detectedAt.isNotBlank()) { "DetectedAt timestamp cannot be blank" }
    }
}
