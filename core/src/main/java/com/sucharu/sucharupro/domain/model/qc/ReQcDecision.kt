package com.sucharu.sucharupro.domain.model.qc

/**
 * Inspection decision outcome for a Re-QC cycle (Module 06 Step 06).
 */
enum class ReQcDecision(
    val defaultLabel: String
) {
    PENDING("Pending"),
    PASS("Pass"),
    FAIL("Fail");

    companion object {
        fun fromString(value: String?): ReQcDecision? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
