package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Severity categorization for Quality Alerts (Module 06 Step 10).
 */
enum class QcAlertSeverity(
    val defaultLabel: String,
    val level: Int
) {
    INFO("Info", 1),
    WARNING("Warning", 2),
    CRITICAL("Critical", 3);

    companion object {
        fun fromString(value: String?): QcAlertSeverity? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
