package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * State machine for Quality Alert lifecycle (Module 06 Step 10).
 */
enum class QcAlertStatus(
    val defaultLabel: String,
    val isTerminal: Boolean
) {
    DETECTED("Detected", false),
    ACKNOWLEDGED("Acknowledged", false),
    INVESTIGATING("Investigating", false),
    ACTION_REQUIRED("Action Required", false),
    RESOLVED("Resolved", true),
    DISMISSED("Dismissed", true);

    val canTransitionToNext: Boolean get() = !isTerminal

    companion object {
        fun fromString(value: String?): QcAlertStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
