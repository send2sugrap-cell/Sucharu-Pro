package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Priority levels for Quality Improvement Actions (Module 06 Step 10).
 */
enum class QcImprovementPriority(
    val defaultLabel: String,
    val level: Int
) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    CRITICAL("Critical", 4);

    companion object {
        fun fromString(value: String?): QcImprovementPriority? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
