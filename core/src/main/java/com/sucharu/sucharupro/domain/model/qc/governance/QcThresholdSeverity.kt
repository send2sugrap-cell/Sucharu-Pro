package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Severity levels for KPI threshold breaches and governance alerts (Module 06 Step 10).
 */
enum class QcThresholdSeverity(
    val defaultLabel: String,
    val level: Int
) {
    INFO("Info", 1),
    WARNING("Warning", 2),
    CRITICAL("Critical", 3);

    companion object {
        fun fromString(value: String?): QcThresholdSeverity? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
