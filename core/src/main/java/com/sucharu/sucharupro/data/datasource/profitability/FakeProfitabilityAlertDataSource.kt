package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-Safe In-Memory Fake for Profitability Alerts & Management Action Persistence.
 * Module 16 Step 09.
 */
class FakeProfitabilityAlertDataSource : ProfitabilityAlertDataSource {

    private val alerts = ConcurrentHashMap<String, ProfitabilityAlert>()
    private val occurrences = ConcurrentHashMap<String, MutableList<ProfitabilityAlertOccurrence>>()
    private val rules = ConcurrentHashMap<String, ProfitabilityAlertRule>()
    private val actions = ConcurrentHashMap<String, ProfitabilityManagementAction>()
    private val actionOutcomes = ConcurrentHashMap<String, ProfitabilityActionOutcome>()
    private val correlations = ConcurrentHashMap<String, MutableList<ProfitabilityAlertCorrelation>>()
    private val escalations = ConcurrentHashMap<String, MutableList<ProfitabilityAlertEscalation>>()
    private val snapshots = ConcurrentHashMap<String, MutableList<ProfitabilityMonitoringSnapshot>>()
    private val auditEvents = ConcurrentHashMap<String, MutableList<ProfitabilityAlertAuditEvent>>()
    private val provenanceRecords = ConcurrentHashMap<String, MutableList<ProfitabilityAlertProvenance>>()

    override suspend fun saveAlert(alert: ProfitabilityAlert) {
        alerts["${alert.tenantId}:${alert.alertId}"] = alert
    }

    override suspend fun updateAlert(alert: ProfitabilityAlert) {
        alerts["${alert.tenantId}:${alert.alertId}"] = alert
    }

    override suspend fun getAlertById(tenantId: String, alertId: String): ProfitabilityAlert? {
        return alerts["$tenantId:$alertId"]
    }

    override suspend fun findAlertByFingerprint(tenantId: String, fingerprint: String): ProfitabilityAlert? {
        return alerts.values.firstOrNull { it.tenantId == tenantId && it.fingerprint == fingerprint }
    }

    override suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        dimension: ProfitabilityAlertDimension?,
        severity: ProfitabilityAlertSeverity?,
        status: ProfitabilityAlertStatus?,
        isRecurring: Boolean?
    ): List<ProfitabilityAlert> {
        return alerts.values.filter {
            it.tenantId == tenantId &&
            it.projectId == projectId &&
            (dimension == null || it.dimensionType == dimension) &&
            (severity == null || it.severity == severity) &&
            (status == null || it.status == status) &&
            (isRecurring == null || it.isRecurring == isRecurring)
        }.sortedByDescending { it.lastDetectedAt }
    }

    override suspend fun saveOccurrence(occurrence: ProfitabilityAlertOccurrence) {
        val list = occurrences.computeIfAbsent("${occurrence.tenantId}:${occurrence.alertId}") { mutableListOf() }
        list.add(occurrence)
    }

    override suspend fun listOccurrences(tenantId: String, alertId: String): List<ProfitabilityAlertOccurrence> {
        return occurrences["$tenantId:$alertId"] ?: emptyList()
    }

    override suspend fun saveRule(rule: ProfitabilityAlertRule) {
        rules["${rule.tenantId}:${rule.ruleId}"] = rule
    }

    override suspend fun updateRule(rule: ProfitabilityAlertRule) {
        rules["${rule.tenantId}:${rule.ruleId}"] = rule
    }

    override suspend fun getRuleById(tenantId: String, ruleId: String): ProfitabilityAlertRule? {
        return rules["$tenantId:$ruleId"]
    }

    override suspend fun listRules(
        tenantId: String,
        projectId: String,
        dimensionType: ProfitabilityAlertDimension?
    ): List<ProfitabilityAlertRule> {
        return rules.values.filter {
            it.tenantId == tenantId &&
            it.projectId == projectId &&
            (dimensionType == null || it.dimensionType == dimensionType)
        }.sortedByDescending { it.createdAt }
    }

    override suspend fun saveAction(action: ProfitabilityManagementAction) {
        actions["${action.tenantId}:${action.actionId}"] = action
    }

    override suspend fun updateAction(action: ProfitabilityManagementAction) {
        actions["${action.tenantId}:${action.actionId}"] = action
    }

    override suspend fun getActionById(tenantId: String, actionId: String): ProfitabilityManagementAction? {
        return actions["$tenantId:$actionId"]
    }

    override suspend fun listActions(
        tenantId: String,
        projectId: String,
        alertId: String?,
        status: ManagementActionStatus?
    ): List<ProfitabilityManagementAction> {
        return actions.values.filter {
            it.tenantId == tenantId &&
            it.projectId == projectId &&
            (alertId == null || it.alertId == alertId) &&
            (status == null || it.status == status)
        }.sortedByDescending { it.priorityScore }
    }

    override suspend fun saveActionOutcome(outcome: ProfitabilityActionOutcome) {
        actionOutcomes["${outcome.tenantId}:${outcome.outcomeId}"] = outcome
    }

    override suspend fun saveCorrelation(correlation: ProfitabilityAlertCorrelation) {
        val list = correlations.computeIfAbsent("${correlation.tenantId}:${correlation.projectId}") { mutableListOf() }
        list.removeIf { it.correlationId == correlation.correlationId }
        list.add(correlation)
    }

    override suspend fun listCorrelations(tenantId: String, projectId: String): List<ProfitabilityAlertCorrelation> {
        return correlations["$tenantId:$projectId"] ?: emptyList()
    }

    override suspend fun saveEscalation(escalation: ProfitabilityAlertEscalation) {
        val list = escalations.computeIfAbsent(escalation.tenantId) { mutableListOf() }
        list.removeIf { it.escalationId == escalation.escalationId }
        list.add(escalation)
    }

    override suspend fun listEscalations(tenantId: String, projectId: String): List<ProfitabilityAlertEscalation> {
        return escalations[tenantId] ?: emptyList()
    }

    override suspend fun saveMonitoringSnapshot(snapshot: ProfitabilityMonitoringSnapshot) {
        val list = snapshots.computeIfAbsent("${snapshot.tenantId}:${snapshot.projectId}") { mutableListOf() }
        list.add(snapshot)
    }

    override suspend fun getLatestMonitoringSnapshot(
        tenantId: String,
        projectId: String,
        periodId: String?
    ): ProfitabilityMonitoringSnapshot? {
        val list = snapshots["$tenantId:$projectId"] ?: return null
        return list.filter { periodId == null || it.periodId == periodId }.maxByOrNull { it.generatedAt }
    }

    override suspend fun saveAuditEvent(event: ProfitabilityAlertAuditEvent) {
        val list = auditEvents.computeIfAbsent("${event.tenantId}:${event.alertId}") { mutableListOf() }
        list.add(event)
    }

    override suspend fun listAuditEvents(tenantId: String, alertId: String): List<ProfitabilityAlertAuditEvent> {
        return auditEvents["$tenantId:$alertId"] ?: emptyList()
    }

    override suspend fun saveProvenance(provenance: ProfitabilityAlertProvenance) {
        val list = provenanceRecords.computeIfAbsent("${provenance.tenantId}:${provenance.alertId}") { mutableListOf() }
        list.add(provenance)
    }

    override suspend fun listProvenance(tenantId: String, alertId: String): List<ProfitabilityAlertProvenance> {
        return provenanceRecords["$tenantId:$alertId"] ?: emptyList()
    }
}
