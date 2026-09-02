package com.sucharu.sucharupro.domain.model.qc

/**
 * Root domain entity representing a Final Quality Control inspection record in Sucharu Pro ERP (Module 06 Step 07).
 *
 * Serves as the ultimate quality gate before production release authorization.
 */
data class FinalQcInspection(
    val finalQcId: String,
    val projectId: String,
    val productionJobId: String,
    val productionJobItemId: String? = null,
    val status: FinalQcStatus = FinalQcStatus.PENDING,
    val decision: FinalQcDecision = FinalQcDecision.PENDING,
    val releaseStatus: FinalQcReleaseStatus = FinalQcReleaseStatus.PENDING_AUTHORIZATION,
    val assignedInspectorId: String? = null,
    val assignedInspectorName: String? = null,
    val inspectedBy: String? = null,
    val inspectedByName: String? = null,
    val inspectedAt: String? = null,
    val totalQuantity: Int,
    val inspectedQuantity: Int = 0,
    val acceptedQuantity: Int = 0,
    val rejectedQuantity: Int = 0,
    val quantityUnit: String = "units",
    val notes: String? = null,
    val failureReason: String? = null,
    val blockingReasons: List<String> = emptyList(),
    val preProductionQcId: String? = null,
    val checklistId: String? = null,
    val sourceDefectIds: List<String> = emptyList(),
    val sourceReworkIds: List<String> = emptyList(),
    val sourceReQcIds: List<String> = emptyList(),
    val releaseAuthorizationId: String? = null,
    val createdBy: String? = null,
    val createdByName: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    init {
        require(finalQcId.isNotBlank()) { "Final QC ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(totalQuantity > 0) { "Total quantity must be greater than zero." }
        require(inspectedQuantity >= 0) { "Inspected quantity cannot be negative." }
        require(acceptedQuantity >= 0) { "Accepted quantity cannot be negative." }
        require(rejectedQuantity >= 0) { "Rejected quantity cannot be negative." }
        require(createdAt.isNotBlank()) { "Created timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "Updated timestamp cannot be blank." }
    }

    /** Convenience flag indicating whether the inspection is terminal. */
    val isTerminal: Boolean get() = status.isTerminal

    /** Convenience flag indicating whether an inspector is assigned. */
    val isAssigned: Boolean get() = !assignedInspectorId.isNullOrBlank()

    /** Convenience flag indicating whether the job has been released. */
    val isReleased: Boolean get() = status == FinalQcStatus.RELEASED && releaseStatus == FinalQcReleaseStatus.AUTHORIZED

    /** Convenience flag indicating whether the inspection passed. */
    val isPassed: Boolean get() = decision == FinalQcDecision.PASS && status in setOf(FinalQcStatus.PASSED, FinalQcStatus.RELEASED)
}
