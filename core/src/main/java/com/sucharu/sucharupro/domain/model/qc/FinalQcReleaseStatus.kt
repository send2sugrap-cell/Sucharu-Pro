package com.sucharu.sucharupro.domain.model.qc

/**
 * Status of the production release authorization for a Final QC record (Module 06 Step 07).
 */
enum class FinalQcReleaseStatus(
    val defaultLabel: String
) {
    /** Release has not yet been authorized. */
    PENDING_AUTHORIZATION("Pending Authorization"),

    /** Release has been formally authorized by management. */
    AUTHORIZED("Authorized"),

    /** Release was formally rejected. */
    REJECTED("Rejected"),

    /** Release authorization not applicable (e.g. cancelled). */
    NOT_APPLICABLE("Not Applicable");

    companion object {
        fun fromString(value: String?): FinalQcReleaseStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
