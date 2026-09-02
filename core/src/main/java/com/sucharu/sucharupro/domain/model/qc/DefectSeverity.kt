package com.sucharu.sucharupro.domain.model.qc

/**
 * Severity classification for QC defects and failures (Module 06 Step 04).
 */
enum class DefectSeverity(
    val defaultLabel: String,
    val description: String,
    val priorityWeight: Int
) {
    /** Non-critical variance that does not prevent product usage or delivery. */
    MINOR(
        defaultLabel = "Minor",
        description = "Non-critical cosmetic or minor spec variance.",
        priorityWeight = 1
    ),

    /** Significant defect that impairs quality, function, or appearance; requires containment/resolution. */
    MAJOR(
        defaultLabel = "Major",
        description = "Significant quality variance affecting delivery or performance.",
        priorityWeight = 2
    ),

    /** Critical failure that makes product unusable, violates safety/customer specs, or halts production. */
    CRITICAL(
        defaultLabel = "Critical",
        description = "Critical failure halting production or violating core specifications.",
        priorityWeight = 3
    );

    companion object {
        fun fromString(value: String?): DefectSeverity? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
