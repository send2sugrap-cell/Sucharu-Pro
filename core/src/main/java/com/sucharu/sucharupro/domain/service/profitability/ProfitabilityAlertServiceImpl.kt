package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.repository.profitability.ProfitabilityAlertRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import com.sucharu.sucharupro.domain.validation.profitability.ProfitabilityAlertValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap

/**
 * Production Implementation of ProfitabilityAlertService.
 * Module 16 Step 09.
 */
class ProfitabilityAlertServiceImpl(
    private val repository: ProfitabilityAlertRepository,
    private val sourceCollector: ProfitabilityAlertSourceCollector,
    private val ruleEngine: ProfitabilityAlertRuleEngine = ProfitabilityAlertRuleEngineImpl(),
    private val correlationEngine: ProfitabilityAlertCorrelationEngine = ProfitabilityAlertCorrelationEngineImpl(),
    private val escalationEngine: ProfitabilityAlertEscalationEngine = ProfitabilityAlertEscalationEngineImpl(),
    private val managementActionEngine: ProfitabilityManagementActionEngine = ProfitabilityManagementActionEngineImpl(),
    private val reconciliationService: ProfitabilityAlertReconciliationService = ProfitabilityAlertReconciliationServiceImpl()
) : ProfitabilityAlertService {

    private val tenantLocks = ConcurrentHashMap<String, Mutex>()
    private val idempotencyCache = ConcurrentHashMap<String, ProfitabilityMonitoringSnapshot>()

    private fun getLock(tenantId: String, periodId: String?): Mutex {
        val key = "$tenantId:${periodId ?: "ALL"}"
        return tenantLocks.computeIfAbsent(key) { Mutex() }
    }

    override suspend fun evaluateAlerts(
        tenantId: String,
        projectId: String,
        periodId: String?,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityMonitoringSnapshot> {
        val valResult = ProfitabilityAlertValidator.validateTenantAndProject(tenantId, projectId)
        if (valResult is DomainResult.Error) return valResult

        if (!idempotencyKey.isNullOrBlank()) {
            val cached = idempotencyCache["$tenantId:$idempotencyKey"]
            if (cached != null) {
                return DomainResult.Success(cached)
            }
        }

        return getLock(tenantId, periodId).withLock {
            if (!idempotencyKey.isNullOrBlank()) {
                val cached = idempotencyCache["$tenantId:$idempotencyKey"]
                if (cached != null) return@withLock DomainResult.Success(cached)
            }

            val payload = when (val res = sourceCollector.collectEvaluationPayload(tenantId, projectId, periodId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> return@withLock res
                DomainResult.Loading -> return@withLock DomainResult.Error(message = "Evaluation in progress")
            }

            val rules = when (val res = repository.listRules(tenantId, projectId, null)) {
                is DomainResult.Success -> res.data
                else -> emptyList()
            }

            val detectedAlerts = ruleEngine.evaluateRules(payload, rules)
            val now = System.currentTimeMillis()

            for (detected in detectedAlerts) {
                val existingRes = repository.findAlertByFingerprint(tenantId, detected.fingerprint)
                val existing = if (existingRes is DomainResult.Success) existingRes.data else null

                if (existing != null) {
                    if (existing.status in setOf(ProfitabilityAlertStatus.RESOLVED, ProfitabilityAlertStatus.DISMISSED)) {
                        // Reopen alert if condition reappears
                        val updatedAlert = existing.copy(
                            status = ProfitabilityAlertStatus.REOPENED,
                            observedValue = detected.observedValue,
                            financialImpact = detected.financialImpact,
                            lastDetectedAt = now,
                            occurrenceCount = existing.occurrenceCount + 1,
                            isRecurring = true
                        )
                        repository.updateAlert(updatedAlert)
                        recordOccurrenceAndAudit(updatedAlert, detected, existing.status, "Alert reopened due to recurring threshold violation.", actorId, actorRole)
                    } else {
                        // Deduplicate: increment count and update lastDetectedAt
                        val updatedAlert = existing.copy(
                            observedValue = detected.observedValue,
                            financialImpact = detected.financialImpact,
                            lastDetectedAt = now,
                            occurrenceCount = existing.occurrenceCount + 1,
                            isRecurring = (existing.occurrenceCount + 1) >= 2
                        )
                        repository.updateAlert(updatedAlert)
                        recordOccurrenceAndAudit(updatedAlert, detected, existing.status, "Recurring alert detected; incremented occurrence count to ${updatedAlert.occurrenceCount}.", actorId, actorRole)
                    }
                } else {
                    // New alert
                    repository.saveAlert(detected)
                    recordOccurrenceAndAudit(detected, detected, ProfitabilityAlertStatus.DETECTED, "Initial detection of profitability condition.", actorId, actorRole)

                    // Auto-generate recommended management action if applicable
                    val actions = managementActionEngine.generateRecommendedActions(tenantId, projectId, listOf(detected))
                    for (act in actions) {
                        repository.saveAction(act)
                    }

                    // Save provenance record
                    val provHash = ProfitabilityAlertMathUtils.sha256("${detected.alertId}:${detected.sourceModule}:${detected.sourceEntityId}:${detected.triggerMetric}")
                    repository.saveProvenance(
                        ProfitabilityAlertProvenance(
                            provenanceId = "prv-${detected.alertId}".take(64),
                            alertId = detected.alertId,
                            tenantId = tenantId,
                            sourceModule = detected.sourceModule,
                            sourceStep = detected.sourceStep,
                            sourceEntityType = detected.sourceEntityType,
                            sourceEntityId = detected.sourceEntityId,
                            metricKey = detected.triggerMetric,
                            metricValue = detected.observedValue,
                            calculationTimestamp = now,
                            provenanceHash = provHash
                        )
                    )
                }
            }

            // Fetch current active alerts and compute correlations & escalations
            val allAlerts = when (val res = repository.listAlerts(tenantId, projectId, null, null, null, null)) {
                is DomainResult.Success -> res.data
                else -> emptyList()
            }

            val allActions = when (val res = repository.listActions(tenantId, projectId, null, null)) {
                is DomainResult.Success -> res.data
                else -> emptyList()
            }

            val correlations = correlationEngine.correlateAlerts(tenantId, projectId, allAlerts)
            for (corr in correlations) {
                repository.saveCorrelation(corr)
            }

            val escalations = escalationEngine.evaluateEscalations(tenantId, allAlerts, allActions)
            for (esc in escalations) {
                repository.saveEscalation(esc)
            }

            // Build Monitoring Snapshot
            val activeAlerts = allAlerts.filter {
                it.status !in setOf(
                    ProfitabilityAlertStatus.RESOLVED,
                    ProfitabilityAlertStatus.DISMISSED,
                    ProfitabilityAlertStatus.SUPPRESSED
                )
            }

            val criticalCount = activeAlerts.count { it.severity == ProfitabilityAlertSeverity.CRITICAL }
            val highCount = activeAlerts.count { it.severity == ProfitabilityAlertSeverity.HIGH }
            val mediumCount = activeAlerts.count { it.severity == ProfitabilityAlertSeverity.MEDIUM }
            val lowCount = activeAlerts.count { it.severity == ProfitabilityAlertSeverity.LOW }
            val unresolvedImpact = activeAlerts.map { it.financialImpact }.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
                .setScale(4, RoundingMode.HALF_UP)

            val openActionCount = allActions.count {
                it.status !in setOf(
                    ManagementActionStatus.COMPLETED,
                    ManagementActionStatus.VERIFIED,
                    ManagementActionStatus.CANCELLED
                )
            }
            val overdueActionCount = allActions.count {
                it.dueAt != null && now > it.dueAt && it.status !in setOf(
                    ManagementActionStatus.COMPLETED,
                    ManagementActionStatus.VERIFIED,
                    ManagementActionStatus.CANCELLED
                )
            }

            val snapshotId = "snap-mon-$tenantId-${periodId ?: "ALL"}-$now".take(64)
            val snapHash = ProfitabilityAlertMathUtils.generateMonitoringSnapshotIntegrityHash(
                snapshotId, tenantId, projectId, activeAlerts.size, criticalCount, highCount, unresolvedImpact, openActionCount
            )

            val snapshot = ProfitabilityMonitoringSnapshot(
                snapshotId = snapshotId,
                tenantId = tenantId,
                projectId = projectId,
                periodId = periodId,
                totalActiveAlerts = activeAlerts.size,
                criticalAlertCount = criticalCount,
                highAlertCount = highCount,
                mediumAlertCount = mediumCount,
                lowAlertCount = lowCount,
                totalUnresolvedFinancialImpact = unresolvedImpact,
                openActionCount = openActionCount,
                overdueActionCount = overdueActionCount,
                recurringIssueCount = activeAlerts.count { it.isRecurring },
                escalatedAlertCount = escalations.size,
                severityDistribution = activeAlerts.groupBy { it.severity }.mapValues { it.value.size },
                dimensionDistribution = activeAlerts.groupBy { it.dimensionType }.mapValues { it.value.size },
                generatedAt = now,
                integrityHash = snapHash
            )

            repository.saveMonitoringSnapshot(snapshot)

            if (!idempotencyKey.isNullOrBlank()) {
                idempotencyCache["$tenantId:$idempotencyKey"] = snapshot
            }

            DomainResult.Success(snapshot)
        }
    }

    private suspend fun recordOccurrenceAndAudit(
        alert: ProfitabilityAlert,
        detected: ProfitabilityAlert,
        prevStatus: ProfitabilityAlertStatus,
        notes: String,
        actorId: String,
        actorRole: String
    ) {
        val now = System.currentTimeMillis()
        repository.saveOccurrence(
            ProfitabilityAlertOccurrence(
                occurrenceId = "occ-${alert.alertId}-$now".take(64),
                alertId = alert.alertId,
                tenantId = alert.tenantId,
                detectedAt = now,
                observedValue = detected.observedValue,
                financialImpact = detected.financialImpact,
                previousStatus = prevStatus,
                triggerDetails = notes,
                sourceSnapshotId = null
            )
        )

        repository.saveAuditEvent(
            ProfitabilityAlertAuditEvent(
                eventId = "aud-${alert.alertId}-$now".take(64),
                tenantId = alert.tenantId,
                projectId = alert.projectId,
                alertId = alert.alertId,
                action = "EVALUATE",
                actorId = actorId,
                actorRole = actorRole,
                previousState = prevStatus.name,
                newState = alert.status.name,
                notes = notes,
                timestamp = now
            )
        )
    }

    override suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        dimension: ProfitabilityAlertDimension?,
        severity: ProfitabilityAlertSeverity?,
        status: ProfitabilityAlertStatus?,
        isRecurring: Boolean?
    ): DomainResult<List<ProfitabilityAlert>> {
        val valRes = ProfitabilityAlertValidator.validateTenantAndProject(tenantId, projectId)
        if (valRes is DomainResult.Error) return valRes
        return repository.listAlerts(tenantId, projectId, dimension, severity, status, isRecurring)
    }

    override suspend fun getAlertById(tenantId: String, alertId: String): DomainResult<ProfitabilityAlert> {
        if (tenantId.isBlank() || alertId.isBlank()) return DomainResult.Error(message = "tenantId and alertId must not be blank.")
        val res = repository.getAlertById(tenantId, alertId)
        return when (res) {
            is DomainResult.Success -> {
                val data = res.data
                if (data != null) DomainResult.Success(data) else DomainResult.Error(message = "Alert with ID $alertId not found.")
            }
            is DomainResult.Error -> res
            DomainResult.Loading -> DomainResult.Error(message = "Operation in progress")
        }
    }

    override suspend fun getAlertProvenance(tenantId: String, alertId: String): DomainResult<List<ProfitabilityAlertProvenance>> {
        if (tenantId.isBlank() || alertId.isBlank()) return DomainResult.Error(message = "tenantId and alertId must not be blank.")
        return repository.listProvenance(tenantId, alertId)
    }

    override suspend fun acknowledgeAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityAlert> {
        return updateAlertStatus(
            tenantId = tenantId,
            projectId = projectId,
            alertId = alertId,
            newStatus = ProfitabilityAlertStatus.ACKNOWLEDGED,
            resolutionNotes = "Alert acknowledged by $actorRole $actorId.",
            actorId = actorId,
            actorRole = actorRole
        )
    }

    override suspend fun updateAlertStatus(
        tenantId: String,
        projectId: String,
        alertId: String,
        newStatus: ProfitabilityAlertStatus,
        resolutionNotes: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityAlert> {
        val alertRes = getAlertById(tenantId, alertId)
        val alert = when (alertRes) {
            is DomainResult.Success -> alertRes.data
            is DomainResult.Error -> return alertRes
            DomainResult.Loading -> return DomainResult.Error(message = "Operation in progress")
        }

        val transitionCheck = ProfitabilityAlertValidator.validateAlertStatusTransition(alert.status, newStatus)
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = alert.copy(
            status = newStatus,
            acknowledgedAt = if (newStatus == ProfitabilityAlertStatus.ACKNOWLEDGED) now else alert.acknowledgedAt,
            acknowledgedBy = if (newStatus == ProfitabilityAlertStatus.ACKNOWLEDGED) actorId else alert.acknowledgedBy,
            resolvedAt = if (newStatus == ProfitabilityAlertStatus.RESOLVED) now else alert.resolvedAt,
            resolvedBy = if (newStatus == ProfitabilityAlertStatus.RESOLVED) actorId else alert.resolvedBy,
            resolutionNotes = resolutionNotes ?: alert.resolutionNotes
        )

        repository.updateAlert(updated)

        repository.saveAuditEvent(
            ProfitabilityAlertAuditEvent(
                eventId = "aud-$alertId-$now".take(64),
                tenantId = tenantId,
                projectId = projectId,
                alertId = alertId,
                action = "UPDATE_STATUS",
                actorId = actorId,
                actorRole = actorRole,
                previousState = alert.status.name,
                newState = newStatus.name,
                notes = resolutionNotes,
                timestamp = now
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun resolveAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        resolutionNotes: String,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityAlert> {
        return updateAlertStatus(
            tenantId = tenantId,
            projectId = projectId,
            alertId = alertId,
            newStatus = ProfitabilityAlertStatus.RESOLVED,
            resolutionNotes = resolutionNotes,
            actorId = actorId,
            actorRole = actorRole
        )
    }

    override suspend fun reopenAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityAlert> {
        return updateAlertStatus(
            tenantId = tenantId,
            projectId = projectId,
            alertId = alertId,
            newStatus = ProfitabilityAlertStatus.REOPENED,
            resolutionNotes = reason,
            actorId = actorId,
            actorRole = actorRole
        )
    }

    override suspend fun getMonitoringSnapshot(
        tenantId: String,
        projectId: String,
        periodId: String?
    ): DomainResult<ProfitabilityMonitoringSnapshot> {
        val valRes = ProfitabilityAlertValidator.validateTenantAndProject(tenantId, projectId)
        if (valRes is DomainResult.Error) return valRes

        val snapRes = repository.getLatestMonitoringSnapshot(tenantId, projectId, periodId)
        return when (snapRes) {
            is DomainResult.Success -> {
                val data = snapRes.data
                if (data != null) DomainResult.Success(data) else {
                    // Evaluate immediately on demand
                    evaluateAlerts(tenantId, projectId, periodId, null, "SYSTEM", "SYSTEM")
                }
            }
            is DomainResult.Error -> snapRes
            DomainResult.Loading -> DomainResult.Error(message = "Operation in progress")
        }
    }

    override suspend fun getCriticalAlerts(tenantId: String, projectId: String): DomainResult<List<ProfitabilityAlert>> {
        return listAlerts(
            tenantId = tenantId,
            projectId = projectId,
            dimension = null,
            severity = ProfitabilityAlertSeverity.CRITICAL,
            status = null,
            isRecurring = null
        )
    }

    override suspend fun getEscalations(tenantId: String, projectId: String): DomainResult<List<ProfitabilityAlertEscalation>> {
        val valRes = ProfitabilityAlertValidator.validateTenantAndProject(tenantId, projectId)
        if (valRes is DomainResult.Error) return valRes
        return repository.listEscalations(tenantId, projectId)
    }

    override suspend fun getCorrelations(tenantId: String, projectId: String): DomainResult<List<ProfitabilityAlertCorrelation>> {
        val valRes = ProfitabilityAlertValidator.validateTenantAndProject(tenantId, projectId)
        if (valRes is DomainResult.Error) return valRes
        return repository.listCorrelations(tenantId, projectId)
    }

    override suspend fun listRules(
        tenantId: String,
        projectId: String,
        dimensionType: ProfitabilityAlertDimension?
    ): DomainResult<List<ProfitabilityAlertRule>> {
        val valRes = ProfitabilityAlertValidator.validateTenantAndProject(tenantId, projectId)
        if (valRes is DomainResult.Error) return valRes
        return repository.listRules(tenantId, projectId, dimensionType)
    }

    override suspend fun createRule(
        tenantId: String,
        projectId: String,
        rule: ProfitabilityAlertRule,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityAlertRule> {
        val valRes = ProfitabilityAlertValidator.validateAlertRule(rule)
        if (valRes is DomainResult.Error) return valRes

        repository.saveRule(rule)
        return DomainResult.Success(rule)
    }

    override suspend fun updateRule(
        tenantId: String,
        projectId: String,
        rule: ProfitabilityAlertRule,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityAlertRule> {
        val valRes = ProfitabilityAlertValidator.validateAlertRule(rule)
        if (valRes is DomainResult.Error) return valRes

        repository.updateRule(rule)
        return DomainResult.Success(rule)
    }

    override suspend fun createManagementAction(
        tenantId: String,
        projectId: String,
        action: ProfitabilityManagementAction,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityManagementAction> {
        val valRes = ProfitabilityAlertValidator.validateManagementAction(action)
        if (valRes is DomainResult.Error) return valRes

        repository.saveAction(action)
        return DomainResult.Success(action)
    }

    override suspend fun listManagementActions(
        tenantId: String,
        projectId: String,
        alertId: String?,
        status: ManagementActionStatus?
    ): DomainResult<List<ProfitabilityManagementAction>> {
        val valRes = ProfitabilityAlertValidator.validateTenantAndProject(tenantId, projectId)
        if (valRes is DomainResult.Error) return valRes
        return repository.listActions(tenantId, projectId, alertId, status)
    }

    override suspend fun updateActionStatus(
        tenantId: String,
        projectId: String,
        actionId: String,
        newStatus: ManagementActionStatus,
        realizedImpact: BigDecimal?,
        outcomeNotes: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityManagementAction> {
        val actionRes = repository.getActionById(tenantId, actionId)
        val action = when (actionRes) {
            is DomainResult.Success -> {
                val act = actionRes.data
                if (act != null) act else return DomainResult.Error(message = "Action $actionId not found.")
            }
            is DomainResult.Error -> return actionRes
            DomainResult.Loading -> return DomainResult.Error(message = "Operation in progress")
        }

        val transitionCheck = ProfitabilityAlertValidator.validateManagementActionTransition(action.status, newStatus)
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = action.copy(
            status = newStatus,
            startedAt = if (newStatus == ManagementActionStatus.IN_PROGRESS) now else action.startedAt,
            completedAt = if (newStatus == ManagementActionStatus.COMPLETED) now else action.completedAt,
            verifiedAt = if (newStatus == ManagementActionStatus.VERIFIED) now else action.verifiedAt,
            verifiedBy = if (newStatus == ManagementActionStatus.VERIFIED) actorId else action.verifiedBy,
            realizedFinancialImpact = realizedImpact ?: action.realizedFinancialImpact,
            outcomeNotes = outcomeNotes ?: action.outcomeNotes,
            updatedAt = now
        )

        repository.updateAction(updated)

        if (newStatus == ManagementActionStatus.COMPLETED && realizedImpact != null) {
            val outcome = managementActionEngine.evaluateActionOutcome(
                action = updated,
                metricBefore = action.expectedFinancialImpact,
                metricAfter = action.expectedFinancialImpact.subtract(realizedImpact).max(BigDecimal.ZERO),
                realizedSavings = realizedImpact
            )
            repository.saveActionOutcome(outcome)
        }

        return DomainResult.Success(updated)
    }

    override suspend fun reconcileAlerts(
        tenantId: String,
        projectId: String
    ): DomainResult<ProfitabilityAlertReconciliationAssertion> {
        val valRes = ProfitabilityAlertValidator.validateTenantAndProject(tenantId, projectId)
        if (valRes is DomainResult.Error) return valRes

        val alerts = when (val res = repository.listAlerts(tenantId, projectId, null, null, null, null)) {
            is DomainResult.Success -> res.data
            else -> emptyList()
        }

        val actions = when (val res = repository.listActions(tenantId, projectId, null, null)) {
            is DomainResult.Success -> res.data
            else -> emptyList()
        }

        val snapshot = when (val res = repository.getLatestMonitoringSnapshot(tenantId, projectId, null)) {
            is DomainResult.Success -> res.data
            else -> null
        }

        return reconciliationService.reconcileAlerts(tenantId, projectId, alerts, actions, snapshot)
    }

    override suspend fun listAuditEvents(tenantId: String, alertId: String): DomainResult<List<ProfitabilityAlertAuditEvent>> {
        if (tenantId.isBlank() || alertId.isBlank()) return DomainResult.Error(message = "tenantId and alertId must not be blank.")
        return repository.listAuditEvents(tenantId, alertId)
    }

    override suspend fun exportHandoffContract(
        tenantId: String,
        projectId: String
    ): DomainResult<Module16Step09ProfitabilityAlertHandoffContract> {
        val valRes = ProfitabilityAlertValidator.validateTenantAndProject(tenantId, projectId)
        if (valRes is DomainResult.Error) return valRes

        val snapshot = when (val res = getMonitoringSnapshot(tenantId, projectId, null)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            DomainResult.Loading -> return DomainResult.Error(message = "Operation in progress")
        }

        val allAlerts = when (val res = repository.listAlerts(tenantId, projectId, null, null, null, null)) {
            is DomainResult.Success -> res.data
            else -> emptyList()
        }

        val criticalAlerts = allAlerts.filter { it.severity == ProfitabilityAlertSeverity.CRITICAL }

        val allActions = when (val res = repository.listActions(tenantId, projectId, null, null)) {
            is DomainResult.Success -> res.data
            else -> emptyList()
        }
        val highPriorityActions = allActions.filter { it.priorityScore >= BigDecimal("70.0000") }

        val activeCorrelations = when (val res = repository.listCorrelations(tenantId, projectId)) {
            is DomainResult.Success -> res.data
            else -> emptyList()
        }

        val topEscalations = when (val res = repository.listEscalations(tenantId, projectId)) {
            is DomainResult.Success -> res.data
            else -> emptyList()
        }

        val overallRisk = when {
            topEscalations.any { it.escalationLevel == AlertEscalationLevel.CRITICAL } -> AlertEscalationLevel.CRITICAL
            topEscalations.any { it.escalationLevel == AlertEscalationLevel.URGENT } -> AlertEscalationLevel.URGENT
            topEscalations.any { it.escalationLevel == AlertEscalationLevel.ESCALATE } -> AlertEscalationLevel.ESCALATE
            topEscalations.any { it.escalationLevel == AlertEscalationLevel.WATCH } -> AlertEscalationLevel.WATCH
            else -> AlertEscalationLevel.NONE
        }

        val handoffHash = ProfitabilityAlertMathUtils.sha256(
            "$tenantId:$projectId:${snapshot.snapshotId}:${snapshot.totalActiveAlerts}:${snapshot.totalUnresolvedFinancialImpact}:$overallRisk"
        )

        return DomainResult.Success(
            Module16Step09ProfitabilityAlertHandoffContract(
                tenantId = tenantId,
                projectId = projectId,
                snapshotId = snapshot.snapshotId,
                totalActiveAlerts = snapshot.totalActiveAlerts,
                criticalAlertCount = snapshot.criticalAlertCount,
                highAlertCount = snapshot.highAlertCount,
                totalUnresolvedFinancialImpact = snapshot.totalUnresolvedFinancialImpact,
                criticalAlerts = criticalAlerts,
                highPriorityActions = highPriorityActions,
                activeCorrelations = activeCorrelations,
                topEscalations = topEscalations,
                overallHealthRisk = overallRisk,
                handoffIntegrityHash = handoffHash,
                generatedAt = System.currentTimeMillis(),
                contractVersion = "1.0.0"
            )
        )
    }
}
