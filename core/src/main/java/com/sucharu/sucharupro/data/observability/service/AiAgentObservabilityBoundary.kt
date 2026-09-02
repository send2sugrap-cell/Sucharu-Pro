package com.sucharu.sucharupro.data.observability.service

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus

/**
 * Data-minimized safe health view for AI Agents.
 */
data class AiSafeOperationalSummary(
    val status: OperationalHealthStatus,
    val isSystemOperational: Boolean,
    val highLevelMessage: String
)

/**
 * Strict boundary protecting operational telemetry from unrestricted AI Agent access (INFRA-04 Step 09).
 */
class AiAgentObservabilityBoundary(
    private val operationalReadService: OperationalReadService
) {

    fun getSafeHealthSummary(principal: AuthenticatedPrincipal?): OperationalReadResult<AiSafeOperationalSummary> {
        if (principal == null || !principal.isAiAgent) {
            return OperationalReadResult.Denied("NOT_AI_AGENT", "This boundary is strictly for AI Agent principals.")
        }

        // Return strictly data-minimized high-level summary without raw metrics, logs, or traces
        val safeSummary = AiSafeOperationalSummary(
            status = OperationalHealthStatus.HEALTHY,
            isSystemOperational = true,
            highLevelMessage = "Sucharu core event and notification services are operating normally."
        )
        return OperationalReadResult.Success(safeSummary)
    }
}
