package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * State machine for formal Management Quality Reviews (Module 06 Step 10).
 */
enum class QcQualityReviewStatus(
    val defaultLabel: String,
    val isTerminal: Boolean
) {
    DRAFT("Draft", false),
    SCHEDULED("Scheduled", false),
    IN_REVIEW("In Review", false),
    COMPLETED("Completed", true),
    CANCELLED("Cancelled", true);

    val canTransition: Boolean get() = !isTerminal

    companion object {
        fun fromString(value: String?): QcQualityReviewStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
