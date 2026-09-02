package com.sucharu.sucharupro.domain.model.qc

/**
 * Event types for QC defect mutation audit tracking (Module 06 Step 04).
 */
enum class QcDefectActivityType(
    val defaultLabel: String,
    val description: String
) {
    DEFECT_CREATED(
        defaultLabel = "Defect Logged",
        description = "New QC defect record logged and registered in the system."
    ),
    DEFECT_UPDATED(
        defaultLabel = "Defect Updated",
        description = "Defect specifications, notes, or details modified."
    ),
    DEFECT_ACKNOWLEDGED(
        defaultLabel = "Defect Acknowledged",
        description = "Defect reviewed and acknowledged by authorized personnel."
    ),
    DEFECT_INVESTIGATION_STARTED(
        defaultLabel = "Investigation Started",
        description = "Root cause analysis and investigation initiated."
    ),
    DEFECT_CONTAINED(
        defaultLabel = "Defect Contained",
        description = "Immediate containment measures applied to stop defective output."
    ),
    DEFECT_RESOLUTION_STARTED(
        defaultLabel = "Resolution Pending",
        description = "Corrective action determined and pending implementation."
    ),
    DEFECT_RESOLVED(
        defaultLabel = "Defect Resolved",
        description = "Corrective action completed and verified."
    ),
    DEFECT_CLOSED(
        defaultLabel = "Defect Closed",
        description = "Final sign-off and permanent terminal closure."
    ),
    DEFECT_CANCELLED(
        defaultLabel = "Defect Cancelled",
        description = "Defect voided or cancelled."
    ),
    DEFECT_EVIDENCE_ATTACHED(
        defaultLabel = "Evidence Attached",
        description = "Supporting photo or document evidence attached."
    ),
    DEFECT_SEVERITY_CHANGED(
        defaultLabel = "Severity Changed",
        description = "Defect severity reclassified."
    ),
    DEFECT_STATUS_CHANGED(
        defaultLabel = "Status Changed",
        description = "Defect lifecycle state changed."
    ),
    DEFECT_ASSIGNED(
        defaultLabel = "Defect Assigned",
        description = "Assigned ownership of defect to a technician/inspector."
    ),
    DEFECT_REASSIGNED(
        defaultLabel = "Defect Reassigned",
        description = "Reassigned defect ownership to a new technician/inspector."
    ),
    DEFECT_UNASSIGNED(
        defaultLabel = "Defect Unassigned",
        description = "Removed active defect ownership assignment."
    );

    companion object {
        fun fromString(value: String?): QcDefectActivityType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
