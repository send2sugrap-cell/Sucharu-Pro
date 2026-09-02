package com.sucharu.sucharupro.domain.model.qc

/**
 * Inspection decision for Final QC in Sucharu Pro ERP (Module 06 Step 07).
 */
enum class FinalQcDecision(
    val defaultLabel: String
) {
    /** Inspection outcome not yet determined. */
    PENDING("Pending"),

    /** All final quality checks verified and approved. */
    PASS("Pass"),

    /** One or more critical/major quality checks failed. */
    FAIL("Fail");

    companion object {
        fun fromString(value: String?): FinalQcDecision? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
