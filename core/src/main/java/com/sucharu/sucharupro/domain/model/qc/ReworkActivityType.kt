package com.sucharu.sucharupro.domain.model.qc

/**
 * Event types for QC Rework mutation audit tracking (Module 06 Step 05).
 */
enum class ReworkActivityType(
    val defaultLabel: String,
    val description: String
) {
    REWORK_CREATED(
        defaultLabel = "Rework Created",
        description = "New rework draft initialized."
    ),
    REWORK_REQUESTED(
        defaultLabel = "Rework Requested",
        description = "Rework formally submitted for review."
    ),
    REWORK_REVIEW_STARTED(
        defaultLabel = "Review Started",
        description = "Management review and feasibility assessment initiated."
    ),
    REWORK_APPROVED(
        defaultLabel = "Rework Approved",
        description = "Rework authorized by management."
    ),
    REWORK_REJECTED(
        defaultLabel = "Rework Rejected",
        description = "Rework request rejected by management."
    ),
    REWORK_ASSIGNED(
        defaultLabel = "Rework Assigned",
        description = "Rework assigned to a designated operator/technician."
    ),
    REWORK_REASSIGNED(
        defaultLabel = "Rework Reassigned",
        description = "Rework reassigned to a new operator/technician."
    ),
    REWORK_UNASSIGNED(
        defaultLabel = "Rework Unassigned",
        description = "Rework assignment cleared."
    ),
    REWORK_STARTED(
        defaultLabel = "Rework Started",
        description = "Execution of corrective action started."
    ),
    REWORK_UPDATED(
        defaultLabel = "Rework Updated",
        description = "Rework parameters or operational details updated."
    ),
    REWORK_COMPLETED(
        defaultLabel = "Rework Completed",
        description = "Corrective rework action finished."
    ),
    REWORK_RETURNED_TO_QC(
        defaultLabel = "Returned to QC",
        description = "Rework handed off to QC for subsequent Re-QC inspection."
    ),
    REWORK_CANCELLED(
        defaultLabel = "Rework Cancelled",
        description = "Rework cancelled."
    ),
    REWORK_EVIDENCE_ATTACHED(
        defaultLabel = "Evidence Attached",
        description = "Supporting evidence file or photo attached to rework."
    );

    companion object {
        fun fromString(value: String?): ReworkActivityType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
