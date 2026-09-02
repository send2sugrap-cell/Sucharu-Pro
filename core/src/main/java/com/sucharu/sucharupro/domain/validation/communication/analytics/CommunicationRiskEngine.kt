package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationKpiSummary
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationRiskIndicator
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationRiskType
import com.sucharu.sucharupro.domain.model.communication.analytics.RiskSeverity

object CommunicationRiskEngine {

    private const val FAILURE_RATE_THRESHOLD_HIGH = 0.10 // 10%
    private const val FAILURE_RATE_THRESHOLD_CRITICAL = 0.25 // 25%
    
    private const val DELIVERY_RATE_THRESHOLD_LOW = 0.85 // 85%
    private const val READ_RATE_THRESHOLD_LOW = 0.30 // 30%

    fun detectRisks(kpiSummary: CommunicationKpiSummary): List<CommunicationRiskIndicator> {
        val risks = mutableListOf<CommunicationRiskIndicator>()

        if (kpiSummary.totalCommunications < 50) {
            // Not enough data for statistical significance
            return risks
        }

        // 1. High Failure Rate
        if (kpiSummary.failureRate > FAILURE_RATE_THRESHOLD_CRITICAL) {
            risks.add(
                CommunicationRiskIndicator(
                    riskType = CommunicationRiskType.HIGH_FAILURE_RATE,
                    severity = RiskSeverity.CRITICAL,
                    observedValue = kpiSummary.failureRate,
                    threshold = FAILURE_RATE_THRESHOLD_CRITICAL,
                    explanation = "Failure rate of ${kpiSummary.failureRate * 100}% exceeds critical threshold of ${FAILURE_RATE_THRESHOLD_CRITICAL * 100}%"
                )
            )
        } else if (kpiSummary.failureRate > FAILURE_RATE_THRESHOLD_HIGH) {
            risks.add(
                CommunicationRiskIndicator(
                    riskType = CommunicationRiskType.HIGH_FAILURE_RATE,
                    severity = RiskSeverity.HIGH,
                    observedValue = kpiSummary.failureRate,
                    threshold = FAILURE_RATE_THRESHOLD_HIGH,
                    explanation = "Failure rate of ${kpiSummary.failureRate * 100}% is abnormally high"
                )
            )
        }

        // 2. Low Delivery Rate
        if (kpiSummary.deliveryRate < DELIVERY_RATE_THRESHOLD_LOW) {
            risks.add(
                CommunicationRiskIndicator(
                    riskType = CommunicationRiskType.LOW_DELIVERY_RATE,
                    severity = RiskSeverity.MEDIUM,
                    observedValue = kpiSummary.deliveryRate,
                    threshold = DELIVERY_RATE_THRESHOLD_LOW,
                    explanation = "Delivery rate of ${kpiSummary.deliveryRate * 100}% is below expected baseline"
                )
            )
        }

        // 3. Low Read Rate
        if (kpiSummary.readRate < READ_RATE_THRESHOLD_LOW) {
            risks.add(
                CommunicationRiskIndicator(
                    riskType = CommunicationRiskType.LOW_READ_RATE,
                    severity = RiskSeverity.MEDIUM,
                    observedValue = kpiSummary.readRate,
                    threshold = READ_RATE_THRESHOLD_LOW,
                    explanation = "Read rate of ${kpiSummary.readRate * 100}% indicates poor engagement"
                )
            )
        }
        
        // 4. Unread Backlog (If queued + unread > 20% of total volume for high volume)
        val backlogCount = kpiSummary.queuedCount + (kpiSummary.deliveredCount - kpiSummary.readCount)
        val backlogRatio = backlogCount.toDouble() / kpiSummary.totalCommunications
        if (backlogRatio > 0.40) {
            risks.add(
                CommunicationRiskIndicator(
                    riskType = CommunicationRiskType.UNREAD_BACKLOG,
                    severity = RiskSeverity.HIGH,
                    observedValue = backlogRatio,
                    threshold = 0.40,
                    explanation = "Unread/Queued backlog accounts for ${backlogRatio * 100}% of communication volume"
                )
            )
        }

        return risks
    }
}
