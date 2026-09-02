package com.sucharu.sucharupro.domain.model.qc.analytics

/**
 * Configurable thresholds for deterministic insight generation.
 */
data class QcAnalyticsThresholdConfig(
    val maxAcceptableCostVariance: Double = 500.0,
    val maxAcceptableTimeVarianceMinutes: Long = 60L,
    val maxAcceptableDefectRate: Double = 15.0, // in percent (e.g. 15%)
    val maxAcceptableReworkRate: Double = 10.0, // in percent (e.g. 10%)
    val maxAcceptableReQcRate: Double = 10.0, // in percent (e.g. 10%)
    val repeatedFailureCycleThreshold: Int = 2 // repeated cycles > 1 trigger warning/critical
) {
    companion object {
        val DEFAULT = QcAnalyticsThresholdConfig()
    }
}
