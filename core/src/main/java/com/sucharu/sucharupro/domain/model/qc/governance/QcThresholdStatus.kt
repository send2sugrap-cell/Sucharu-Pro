package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Status of a calculated KPI against its configured target thresholds (Module 06 Step 10).
 */
enum class QcThresholdStatus(
    val defaultLabel: String,
    val isBreached: Boolean
) {
    WITHIN_TARGET("Within Target", false),
    WARNING("Warning Threshold Exceeded", true),
    BREACHED("Target Breached", true),
    CRITICAL_BREACH("Critical Target Breached", true);

    companion object {
        fun fromString(value: String?): QcThresholdStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}
