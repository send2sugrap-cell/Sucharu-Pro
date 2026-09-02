package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Data Source Contract for Profitability Alert persistence.
 * Module 16 Step 09.
 */
interface ProfitabilityAlertDataSource {

    suspend fun saveAlert(alert: ProfitabilityAlert)

    suspend fun updateAlert(alert: ProfitabilityAlert)

    suspend fun getAlertById(tenantId: String, alertId: String): ProfitabilityAlert?

    suspend fun findAlertByFingerprint(tenantId: String, fingerprint: String): ProfitabilityAlert?

    suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        dimension: ProfitabilityAlertDimension?,
        severity: ProfitabilityAlertSeverity?,
        status: ProfitabilityAlertStatus?,
        isRecurring: Boolean?
    ): List<ProfitabilityAlert>

    suspend fun saveOccurrence(occurrence: ProfitabilityAlertOccurrence)

    suspend fun listOccurrences(tenantId: String, alertId: String): List<ProfitabilityAlertOccurrence>

    suspend fun saveRule(rule: ProfitabilityAlertRule)

    suspend fun updateRule(rule: ProfitabilityAlertRule)

    suspend fun getRuleById(tenantId: String, ruleId: String): ProfitabilityAlertRule?

    suspend fun listRules(
        tenantId: String,
        projectId: String,
        dimensionType: ProfitabilityAlertDimension?
    ): List<ProfitabilityAlertRule>

    suspend fun saveAction(action: ProfitabilityManagementAction)

    suspend fun updateAction(action: ProfitabilityManagementAction)

    suspend fun getActionById(tenantId: String, actionId: String): ProfitabilityManagementAction?

    suspend fun listActions(
        tenantId: String,
        projectId: String,
        alertId: String?,
        status: ManagementActionStatus?
    ): List<ProfitabilityManagementAction>

    suspend fun saveActionOutcome(outcome: ProfitabilityActionOutcome)

    suspend fun saveCorrelation(correlation: ProfitabilityAlertCorrelation)

    suspend fun listCorrelations(tenantId: String, projectId: String): List<ProfitabilityAlertCorrelation>

    suspend fun saveEscalation(escalation: ProfitabilityAlertEscalation)

    suspend fun listEscalations(tenantId: String, projectId: String): List<ProfitabilityAlertEscalation>

    suspend fun saveMonitoringSnapshot(snapshot: ProfitabilityMonitoringSnapshot)

    suspend fun getLatestMonitoringSnapshot(
        tenantId: String,
        projectId: String,
        periodId: String?
    ): ProfitabilityMonitoringSnapshot?

    suspend fun saveAuditEvent(event: ProfitabilityAlertAuditEvent)

    suspend fun listAuditEvents(tenantId: String, alertId: String): List<ProfitabilityAlertAuditEvent>

    suspend fun saveProvenance(provenance: ProfitabilityAlertProvenance)

    suspend fun listProvenance(tenantId: String, alertId: String): List<ProfitabilityAlertProvenance>
}
