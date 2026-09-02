package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.data.datasource.profitability.ProfitabilityAlertDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Production Implementation of ProfitabilityAlertRepository delegating to ProfitabilityAlertDataSource.
 * Module 16 Step 09.
 */
class ProfitabilityAlertRepositoryImpl(
    private val dataSource: ProfitabilityAlertDataSource
) : ProfitabilityAlertRepository {

    override suspend fun saveAlert(alert: ProfitabilityAlert): DomainResult<Unit> {
        return try {
            dataSource.saveAlert(alert)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save profitability alert.")
        }
    }

    override suspend fun updateAlert(alert: ProfitabilityAlert): DomainResult<Unit> {
        return try {
            dataSource.updateAlert(alert)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to update profitability alert.")
        }
    }

    override suspend fun getAlertById(tenantId: String, alertId: String): DomainResult<ProfitabilityAlert?> {
        return try {
            val alert = dataSource.getAlertById(tenantId, alertId)
            DomainResult.Success(alert)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to get alert by ID $alertId.")
        }
    }

    override suspend fun findAlertByFingerprint(tenantId: String, fingerprint: String): DomainResult<ProfitabilityAlert?> {
        return try {
            val alert = dataSource.findAlertByFingerprint(tenantId, fingerprint)
            DomainResult.Success(alert)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to find alert by fingerprint.")
        }
    }

    override suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        dimension: ProfitabilityAlertDimension?,
        severity: ProfitabilityAlertSeverity?,
        status: ProfitabilityAlertStatus?,
        isRecurring: Boolean?
    ): DomainResult<List<ProfitabilityAlert>> {
        return try {
            val list = dataSource.listAlerts(tenantId, projectId, dimension, severity, status, isRecurring)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list profitability alerts.")
        }
    }

    override suspend fun saveOccurrence(occurrence: ProfitabilityAlertOccurrence): DomainResult<Unit> {
        return try {
            dataSource.saveOccurrence(occurrence)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save alert occurrence.")
        }
    }

    override suspend fun listOccurrences(tenantId: String, alertId: String): DomainResult<List<ProfitabilityAlertOccurrence>> {
        return try {
            val list = dataSource.listOccurrences(tenantId, alertId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list occurrences for alert $alertId.")
        }
    }

    override suspend fun saveRule(rule: ProfitabilityAlertRule): DomainResult<Unit> {
        return try {
            dataSource.saveRule(rule)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save alert rule.")
        }
    }

    override suspend fun updateRule(rule: ProfitabilityAlertRule): DomainResult<Unit> {
        return try {
            dataSource.updateRule(rule)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to update alert rule.")
        }
    }

    override suspend fun getRuleById(tenantId: String, ruleId: String): DomainResult<ProfitabilityAlertRule?> {
        return try {
            val rule = dataSource.getRuleById(tenantId, ruleId)
            DomainResult.Success(rule)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to get rule by ID $ruleId.")
        }
    }

    override suspend fun listRules(
        tenantId: String,
        projectId: String,
        dimensionType: ProfitabilityAlertDimension?
    ): DomainResult<List<ProfitabilityAlertRule>> {
        return try {
            val list = dataSource.listRules(tenantId, projectId, dimensionType)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list alert rules.")
        }
    }

    override suspend fun saveAction(action: ProfitabilityManagementAction): DomainResult<Unit> {
        return try {
            dataSource.saveAction(action)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save management action.")
        }
    }

    override suspend fun updateAction(action: ProfitabilityManagementAction): DomainResult<Unit> {
        return try {
            dataSource.updateAction(action)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to update management action.")
        }
    }

    override suspend fun getActionById(tenantId: String, actionId: String): DomainResult<ProfitabilityManagementAction?> {
        return try {
            val action = dataSource.getActionById(tenantId, actionId)
            DomainResult.Success(action)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to get action by ID $actionId.")
        }
    }

    override suspend fun listActions(
        tenantId: String,
        projectId: String,
        alertId: String?,
        status: ManagementActionStatus?
    ): DomainResult<List<ProfitabilityManagementAction>> {
        return try {
            val list = dataSource.listActions(tenantId, projectId, alertId, status)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list management actions.")
        }
    }

    override suspend fun saveActionOutcome(outcome: ProfitabilityActionOutcome): DomainResult<Unit> {
        return try {
            dataSource.saveActionOutcome(outcome)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save action outcome.")
        }
    }

    override suspend fun saveCorrelation(correlation: ProfitabilityAlertCorrelation): DomainResult<Unit> {
        return try {
            dataSource.saveCorrelation(correlation)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save alert correlation.")
        }
    }

    override suspend fun listCorrelations(tenantId: String, projectId: String): DomainResult<List<ProfitabilityAlertCorrelation>> {
        return try {
            val list = dataSource.listCorrelations(tenantId, projectId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list alert correlations.")
        }
    }

    override suspend fun saveEscalation(escalation: ProfitabilityAlertEscalation): DomainResult<Unit> {
        return try {
            dataSource.saveEscalation(escalation)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save alert escalation.")
        }
    }

    override suspend fun listEscalations(tenantId: String, projectId: String): DomainResult<List<ProfitabilityAlertEscalation>> {
        return try {
            val list = dataSource.listEscalations(tenantId, projectId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list alert escalations.")
        }
    }

    override suspend fun saveMonitoringSnapshot(snapshot: ProfitabilityMonitoringSnapshot): DomainResult<Unit> {
        return try {
            dataSource.saveMonitoringSnapshot(snapshot)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save monitoring snapshot.")
        }
    }

    override suspend fun getLatestMonitoringSnapshot(
        tenantId: String,
        projectId: String,
        periodId: String?
    ): DomainResult<ProfitabilityMonitoringSnapshot?> {
        return try {
            val snap = dataSource.getLatestMonitoringSnapshot(tenantId, projectId, periodId)
            DomainResult.Success(snap)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to get monitoring snapshot.")
        }
    }

    override suspend fun saveAuditEvent(event: ProfitabilityAlertAuditEvent): DomainResult<Unit> {
        return try {
            dataSource.saveAuditEvent(event)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save audit event.")
        }
    }

    override suspend fun listAuditEvents(tenantId: String, alertId: String): DomainResult<List<ProfitabilityAlertAuditEvent>> {
        return try {
            val list = dataSource.listAuditEvents(tenantId, alertId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list audit events.")
        }
    }

    override suspend fun saveProvenance(provenance: ProfitabilityAlertProvenance): DomainResult<Unit> {
        return try {
            dataSource.saveProvenance(provenance)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save alert provenance.")
        }
    }

    override suspend fun listProvenance(tenantId: String, alertId: String): DomainResult<List<ProfitabilityAlertProvenance>> {
        return try {
            val list = dataSource.listProvenance(tenantId, alertId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list provenance records.")
        }
    }
}
