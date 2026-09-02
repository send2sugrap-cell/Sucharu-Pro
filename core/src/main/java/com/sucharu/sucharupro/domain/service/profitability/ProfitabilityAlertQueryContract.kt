package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Profitability Alert & Management Action Service Interface.
 * Canonical Orchestration Contract for Module 16 Step 09.
 */
interface ProfitabilityAlertService {

    suspend fun evaluateAlerts(
        tenantId: String,
        projectId: String,
        periodId: String? = null,
        idempotencyKey: String? = null,
        actorId: String = "SYSTEM",
        actorRole: String = "SYSTEM"
    ): DomainResult<ProfitabilityMonitoringSnapshot>

    suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        dimension: ProfitabilityAlertDimension? = null,
        severity: ProfitabilityAlertSeverity? = null,
        status: ProfitabilityAlertStatus? = null,
        isRecurring: Boolean? = null
    ): DomainResult<List<ProfitabilityAlert>>

    suspend fun getAlertById(
        tenantId: String,
        alertId: String
    ): DomainResult<ProfitabilityAlert>

    suspend fun getAlertProvenance(
        tenantId: String,
        alertId: String
    ): DomainResult<List<ProfitabilityAlertProvenance>>

    suspend fun acknowledgeAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityAlert>

    suspend fun updateAlertStatus(
        tenantId: String,
        projectId: String,
        alertId: String,
        newStatus: ProfitabilityAlertStatus,
        resolutionNotes: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityAlert>

    suspend fun resolveAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        resolutionNotes: String,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityAlert>

    suspend fun reopenAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityAlert>

    suspend fun getMonitoringSnapshot(
        tenantId: String,
        projectId: String,
        periodId: String? = null
    ): DomainResult<ProfitabilityMonitoringSnapshot>

    suspend fun getCriticalAlerts(
        tenantId: String,
        projectId: String
    ): DomainResult<List<ProfitabilityAlert>>

    suspend fun getEscalations(
        tenantId: String,
        projectId: String
    ): DomainResult<List<ProfitabilityAlertEscalation>>

    suspend fun getCorrelations(
        tenantId: String,
        projectId: String
    ): DomainResult<List<ProfitabilityAlertCorrelation>>

    suspend fun listRules(
        tenantId: String,
        projectId: String,
        dimensionType: ProfitabilityAlertDimension? = null
    ): DomainResult<List<ProfitabilityAlertRule>>

    suspend fun createRule(
        tenantId: String,
        projectId: String,
        rule: ProfitabilityAlertRule,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityAlertRule>

    suspend fun updateRule(
        tenantId: String,
        projectId: String,
        rule: ProfitabilityAlertRule,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityAlertRule>

    suspend fun createManagementAction(
        tenantId: String,
        projectId: String,
        action: ProfitabilityManagementAction,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityManagementAction>

    suspend fun listManagementActions(
        tenantId: String,
        projectId: String,
        alertId: String? = null,
        status: ManagementActionStatus? = null
    ): DomainResult<List<ProfitabilityManagementAction>>

    suspend fun updateActionStatus(
        tenantId: String,
        projectId: String,
        actionId: String,
        newStatus: ManagementActionStatus,
        realizedImpact: BigDecimal? = null,
        outcomeNotes: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityManagementAction>

    suspend fun reconcileAlerts(
        tenantId: String,
        projectId: String
    ): DomainResult<ProfitabilityAlertReconciliationAssertion>

    suspend fun listAuditEvents(
        tenantId: String,
        alertId: String
    ): DomainResult<List<ProfitabilityAlertAuditEvent>>

    suspend fun exportHandoffContract(
        tenantId: String,
        projectId: String
    ): DomainResult<Module16Step09ProfitabilityAlertHandoffContract>
}
