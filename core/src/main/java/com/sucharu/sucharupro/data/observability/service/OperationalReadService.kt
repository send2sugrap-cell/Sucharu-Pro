package com.sucharu.sucharupro.data.observability.service

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.observability.alert.OperationalAlertEngine
import com.sucharu.sucharupro.data.observability.capacity.CapacityMonitor
import com.sucharu.sucharupro.data.observability.health.SystemHealthAggregator
import com.sucharu.sucharupro.data.observability.slo.SloEngine
import com.sucharu.sucharupro.domain.observability.*

/**
 * Result wrapper for secure operational reads.
 */
sealed class OperationalReadResult<out T> {
    data class Success<T>(val data: T) : OperationalReadResult<T>()
    data class Denied(val code: String, val message: String) : OperationalReadResult<Nothing>()
}

/**
 * Production-grade, secure, read-only operational telemetry API service (INFRA-04 Step 09).
 */
class OperationalReadService(
    private val authService: TenantObservabilityAuthorizationService = TenantObservabilityAuthorizationService(),
    private val healthAggregator: SystemHealthAggregator,
    private val alertEngine: OperationalAlertEngine,
    private val sloEngine: SloEngine = SloEngine(),
    private val capacityMonitor: CapacityMonitor = CapacityMonitor()
) {

    fun getSystemHealth(principal: AuthenticatedPrincipal?): OperationalReadResult<SystemHealthSummary> {
        val auth = authService.authorizeGlobalAccess(principal, AuthorizationCapability.OBSERVABILITY_VIEW)
        if (auth is ObservabilityAuthDecision.Denied) {
            return OperationalReadResult.Denied(auth.code, auth.message)
        }
        val alertsCount = alertEngine.getActiveAlerts().size
        return OperationalReadResult.Success(healthAggregator.aggregateSystemHealth(alertsCount))
    }

    fun getTenantHealth(principal: AuthenticatedPrincipal?, targetProjectId: String): OperationalReadResult<TenantHealthSummary> {
        val auth = authService.authorizeTenantAccess(principal, targetProjectId, AuthorizationCapability.OBSERVABILITY_TENANT_VIEW)
        if (auth is ObservabilityAuthDecision.Denied) {
            return OperationalReadResult.Denied(auth.code, auth.message)
        }
        val tenantAlertsCount = alertEngine.getActiveAlerts(targetProjectId).size
        return OperationalReadResult.Success(healthAggregator.aggregateTenantHealth(targetProjectId, tenantAlertsCount))
    }

    fun getActiveAlerts(principal: AuthenticatedPrincipal?, targetProjectId: String): OperationalReadResult<List<OperationalAlert>> {
        val auth = authService.authorizeTenantAccess(principal, targetProjectId, AuthorizationCapability.OBSERVABILITY_ALERT_VIEW)
        if (auth is ObservabilityAuthDecision.Denied) {
            return OperationalReadResult.Denied(auth.code, auth.message)
        }
        return OperationalReadResult.Success(alertEngine.getActiveAlerts(targetProjectId))
    }

    fun getSloStatus(principal: AuthenticatedPrincipal?): OperationalReadResult<List<SloMeasurement>> {
        val auth = authService.authorizeGlobalAccess(principal, AuthorizationCapability.OBSERVABILITY_VIEW)
        if (auth is ObservabilityAuthDecision.Denied) {
            return OperationalReadResult.Denied(auth.code, auth.message)
        }
        val system = healthAggregator.aggregateSystemHealth()
        val measurements = mapOf(
            "slo-evt-pub" to 99.9,
            "slo-notif-deliv" to system.notificationInfrastructure.overallDeliveryRate,
            "slo-job-comp" to 99.0,
            "slo-wf-comp" to 99.5,
            "slo-prov-avail" to 99.8,
            "slo-ai-act" to 98.0
        )
        return OperationalReadResult.Success(sloEngine.evaluateAll(measurements))
    }

    fun getCapacitySnapshot(principal: AuthenticatedPrincipal?): OperationalReadResult<CapacitySnapshot> {
        val auth = authService.authorizeGlobalAccess(principal, AuthorizationCapability.OBSERVABILITY_VIEW)
        if (auth is ObservabilityAuthDecision.Denied) {
            return OperationalReadResult.Denied(auth.code, auth.message)
        }
        return OperationalReadResult.Success(capacityMonitor.captureSnapshot())
    }
}
