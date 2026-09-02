package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Repository Contract for Profitability Alerts, Early-Warning & Management Action Persistence.
 * Module 16 Step 09.
 */
interface ProfitabilityAlertRepository {

    suspend fun saveAlert(alert: ProfitabilityAlert): DomainResult<Unit>

    suspend fun updateAlert(alert: ProfitabilityAlert): DomainResult<Unit>

    suspend fun getAlertById(tenantId: String, alertId: String): DomainResult<ProfitabilityAlert?>

    suspend fun findAlertByFingerprint(tenantId: String, fingerprint: String): DomainResult<ProfitabilityAlert?>

    suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        dimension: ProfitabilityAlertDimension?,
        severity: ProfitabilityAlertSeverity?,
        status: ProfitabilityAlertStatus?,
        isRecurring: Boolean?
    ): DomainResult<List<ProfitabilityAlert>>

    suspend fun saveOccurrence(occurrence: ProfitabilityAlertOccurrence): DomainResult<Unit>

    suspend fun listOccurrences(tenantId: String, alertId: String): DomainResult<List<ProfitabilityAlertOccurrence>>

    suspend fun saveRule(rule: ProfitabilityAlertRule): DomainResult<Unit>

    suspend fun updateRule(rule: ProfitabilityAlertRule): DomainResult<Unit>

    suspend fun getRuleById(tenantId: String, ruleId: String): DomainResult<ProfitabilityAlertRule?>

    suspend fun listRules(
        tenantId: String,
        projectId: String,
        dimensionType: ProfitabilityAlertDimension?
    ): DomainResult<List<ProfitabilityAlertRule>>

    suspend fun saveAction(action: ProfitabilityManagementAction): DomainResult<Unit>

    suspend fun updateAction(action: ProfitabilityManagementAction): DomainResult<Unit>

    suspend fun getActionById(tenantId: String, actionId: String): DomainResult<ProfitabilityManagementAction?>

    suspend fun listActions(
        tenantId: String,
        projectId: String,
        alertId: String?,
        status: ManagementActionStatus?
    ): DomainResult<List<ProfitabilityManagementAction>>

    suspend fun saveActionOutcome(outcome: ProfitabilityActionOutcome): DomainResult<Unit>

    suspend fun saveCorrelation(correlation: ProfitabilityAlertCorrelation): DomainResult<Unit>

    suspend fun listCorrelations(tenantId: String, projectId: String): DomainResult<List<ProfitabilityAlertCorrelation>>

    suspend fun saveEscalation(escalation: ProfitabilityAlertEscalation): DomainResult<Unit>

    suspend fun listEscalations(tenantId: String, projectId: String): DomainResult<List<ProfitabilityAlertEscalation>>

    suspend fun saveMonitoringSnapshot(snapshot: ProfitabilityMonitoringSnapshot): DomainResult<Unit>

    suspend fun getLatestMonitoringSnapshot(
        tenantId: String,
        projectId: String,
        periodId: String?
    ): DomainResult<ProfitabilityMonitoringSnapshot?>

    suspend fun saveAuditEvent(event: ProfitabilityAlertAuditEvent): DomainResult<Unit>

    suspend fun listAuditEvents(tenantId: String, alertId: String): DomainResult<List<ProfitabilityAlertAuditEvent>>

    suspend fun saveProvenance(provenance: ProfitabilityAlertProvenance): DomainResult<Unit>

    suspend fun listProvenance(tenantId: String, alertId: String): DomainResult<List<ProfitabilityAlertProvenance>>
}
