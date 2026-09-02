package com.sucharu.sucharupro.domain.model.qc

/**
 * Audit activity event types for Final QC & Production Release (Module 06 Step 07).
 */
enum class FinalQcActivityType(
    val defaultLabel: String
) {
    FINAL_QC_CREATED("Final QC Created"),
    FINAL_QC_ASSIGNED("Inspector Assigned"),
    FINAL_QC_REASSIGNED("Inspector Reassigned"),
    FINAL_QC_UNASSIGNED("Inspector Unassigned"),
    FINAL_QC_STARTED("Final QC Started"),
    FINAL_QC_PASSED("Final QC Passed"),
    FINAL_QC_FAILED("Final QC Failed"),
    FINAL_QC_BLOCKED("Final QC Blocked"),
    FINAL_QC_RELEASE_ELIGIBILITY_CHECKED("Release Eligibility Evaluated"),
    FINAL_QC_RELEASE_AUTHORIZED("Production Release Authorized"),
    FINAL_QC_RELEASE_REJECTED("Production Release Rejected"),
    FINAL_QC_CANCELLED("Final QC Cancelled");

    companion object {
        fun fromString(value: String?): FinalQcActivityType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
