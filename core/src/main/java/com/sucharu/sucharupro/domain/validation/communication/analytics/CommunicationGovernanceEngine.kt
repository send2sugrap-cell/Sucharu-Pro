package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnomaly
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceResult
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationRiskIndicator
import com.sucharu.sucharupro.domain.model.communication.analytics.GovernanceStatus
import com.sucharu.sucharupro.domain.model.communication.analytics.RiskSeverity

object CommunicationGovernanceEngine {

    fun evaluateGovernance(
        projectId: String,
        risks: List<CommunicationRiskIndicator>,
        anomalies: List<CommunicationAnomaly>
    ): CommunicationGovernanceResult {
        val passedControls = mutableListOf<String>()
        val warningControls = mutableListOf<String>()
        val failedControls = mutableListOf<String>()

        // Rule 1: Zero Critical Risks
        val criticalRisks = risks.filter { it.severity == RiskSeverity.CRITICAL }
        if (criticalRisks.isEmpty()) {
            passedControls.add("ZERO_CRITICAL_RISKS")
        } else {
            failedControls.add("ZERO_CRITICAL_RISKS")
        }

        // Rule 2: Zero High Anomalies
        val highAnomalies = anomalies.filter { it.severity == RiskSeverity.HIGH || it.severity == RiskSeverity.CRITICAL }
        if (highAnomalies.isEmpty()) {
            passedControls.add("STABLE_COMMUNICATION_VOLUME_AND_ENGAGEMENT")
        } else {
            failedControls.add("STABLE_COMMUNICATION_VOLUME_AND_ENGAGEMENT")
        }

        // Rule 3: Acceptable Warning Levels
        val warnings = risks.filter { it.severity == RiskSeverity.MEDIUM } + anomalies.filter { it.severity == RiskSeverity.MEDIUM }
        if (warnings.size > 3) {
            warningControls.add("MULTIPLE_WARNING_INDICATORS_ACTIVE")
        } else {
            passedControls.add("WARNING_INDICATORS_WITHIN_LIMIT")
        }

        val status = when {
            failedControls.isNotEmpty() -> GovernanceStatus.CRITICAL
            warningControls.isNotEmpty() -> GovernanceStatus.WARNING
            else -> GovernanceStatus.HEALTHY
        }

        return CommunicationGovernanceResult(
            projectId = projectId,
            passedControls = passedControls,
            warningControls = warningControls,
            failedControls = failedControls,
            riskCount = risks.size,
            anomalyCount = anomalies.size,
            governanceStatus = status
        )
    }
}
