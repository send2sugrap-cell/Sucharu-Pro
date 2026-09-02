package com.sucharu.sucharupro.domain.model.qc

/**
 * Originating trigger or classification type for a Re-QC cycle (Module 06 Step 06).
 */
enum class ReQcCycleType(
    val defaultLabel: String
) {
    POST_REWORK("Post Rework"),
    POST_CORRECTIVE_ACTION("Post Corrective Action"),
    POST_DEFECT_RESOLUTION("Post Defect Resolution"),
    REPEAT_FAILURE("Repeat Failure"),
    MANAGEMENT_RECHECK("Management Recheck");

    companion object {
        fun fromString(value: String?): ReQcCycleType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
