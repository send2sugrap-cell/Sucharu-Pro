package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnomaly
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationKpiSummary
import com.sucharu.sucharupro.domain.model.communication.analytics.RiskSeverity
import kotlin.math.abs

object CommunicationAnomalyDetector {

    /**
     * Compares a baseline KPI against current KPI to detect statistically significant anomalies
     */
    fun detectAnomalies(baseline: CommunicationKpiSummary, current: CommunicationKpiSummary): List<CommunicationAnomaly> {
        val anomalies = mutableListOf<CommunicationAnomaly>()

        // 1. Volume Spikes
        if (baseline.totalCommunications > 10) { // Require minimum baseline volume
            val volumeDeviation = (current.totalCommunications - baseline.totalCommunications).toDouble() / baseline.totalCommunications
            if (volumeDeviation > 1.5) { // 150% increase
                anomalies.add(
                    CommunicationAnomaly(
                        anomalyType = "VOLUME_SPIKE",
                        baselineValue = baseline.totalCommunications.toDouble(),
                        observedValue = current.totalCommunications.toDouble(),
                        deviationPercentage = volumeDeviation * 100,
                        severity = RiskSeverity.HIGH,
                        explanation = "Communication volume increased by ${String.format("%.1f", volumeDeviation * 100)}% compared to baseline"
                    )
                )
            } else if (volumeDeviation < -0.8) { // 80% decrease
                anomalies.add(
                    CommunicationAnomaly(
                        anomalyType = "VOLUME_DROP",
                        baselineValue = baseline.totalCommunications.toDouble(),
                        observedValue = current.totalCommunications.toDouble(),
                        deviationPercentage = volumeDeviation * 100,
                        severity = RiskSeverity.MEDIUM,
                        explanation = "Communication volume dropped unexpectedly by ${String.format("%.1f", abs(volumeDeviation) * 100)}%"
                    )
                )
            }
        }

        // 2. Failure Spikes
        val failureDeviation = current.failureRate - baseline.failureRate
        if (failureDeviation > 0.15) { // 15% absolute jump in failure rate
            anomalies.add(
                CommunicationAnomaly(
                    anomalyType = "FAILURE_SPIKE",
                    baselineValue = baseline.failureRate,
                    observedValue = current.failureRate,
                    deviationPercentage = failureDeviation * 100,
                    severity = RiskSeverity.CRITICAL,
                    explanation = "Delivery failure rate spiked by ${String.format("%.1f", failureDeviation * 100)}%"
                )
            )
        }

        // 3. Engagement Drops
        val engagementDeviation = baseline.readRate - current.readRate
        if (engagementDeviation > 0.20 && baseline.totalCommunications > 50) { // 20% absolute drop in read rate
            anomalies.add(
                CommunicationAnomaly(
                    anomalyType = "ENGAGEMENT_DROP",
                    baselineValue = baseline.readRate,
                    observedValue = current.readRate,
                    deviationPercentage = engagementDeviation * 100, // Positive value representing drop
                    severity = RiskSeverity.HIGH,
                    explanation = "Read rate dropped significantly by ${String.format("%.1f", engagementDeviation * 100)}%"
                )
            )
        }

        return anomalies
    }
}
