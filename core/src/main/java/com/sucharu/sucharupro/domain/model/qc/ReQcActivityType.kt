package com.sucharu.sucharupro.domain.model.qc

/**
 * Audit activity event types for Re-QC & Failure Loops (Module 06 Step 06).
 */
enum class ReQcActivityType(
    val defaultLabel: String
) {
    RE_QC_CREATED("Re-QC Created"),
    RE_QC_ASSIGNED("Re-QC Assigned"),
    RE_QC_REASSIGNED("Re-QC Reassigned"),
    RE_QC_UNASSIGNED("Re-QC Unassigned"),
    RE_QC_STARTED("Re-QC Started"),
    RE_QC_ITEM_UPDATED("Re-QC Item Updated"),
    RE_QC_PASSED("Re-QC Passed"),
    RE_QC_FAILED("Re-QC Failed"),
    RE_QC_RETURNED_TO_REWORK("Re-QC Returned to Rework"),
    RE_QC_CYCLE_CREATED("Re-QC Cycle Created"),
    RE_QC_FAILURE_RECORDED("Re-QC Failure Recorded"),
    RE_QC_CANCELLED("Re-QC Cancelled");

    companion object {
        fun fromString(value: String?): ReQcActivityType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
