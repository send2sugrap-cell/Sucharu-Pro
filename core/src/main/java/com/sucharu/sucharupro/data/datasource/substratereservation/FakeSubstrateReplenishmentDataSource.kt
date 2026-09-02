package com.sucharu.sucharupro.data.datasource.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*
import java.util.concurrent.ConcurrentHashMap

class FakeSubstrateReplenishmentDataSource : SubstrateReplenishmentDataSource {

    private val evaluations = ConcurrentHashMap<String, SubstrateReplenishmentEvaluation>()
    private val alerts = ConcurrentHashMap<String, SupplierReorderAlert>()
    private val auditEvents = ConcurrentHashMap<String, MutableList<SubstrateReplenishmentAuditEvent>>()

    override suspend fun saveEvaluation(evaluation: SubstrateReplenishmentEvaluation): SubstrateReplenishmentEvaluation {
        evaluations[evaluation.evaluationId] = evaluation
        return evaluation
    }

    override suspend fun findEvaluationById(tenantId: String, evaluationId: String): SubstrateReplenishmentEvaluation? {
        val eval = evaluations[evaluationId]
        return if (eval?.tenantId == tenantId) eval else null
    }

    override suspend fun findLatestEvaluationByFingerprint(tenantId: String, fingerprint: String): SubstrateReplenishmentEvaluation? {
        return evaluations.values
            .filter { it.tenantId == tenantId && it.deduplicationFingerprint == fingerprint }
            .maxByOrNull { it.evaluatedAt }
    }

    override suspend fun listEvaluationsBySku(tenantId: String, sku: String): List<SubstrateReplenishmentEvaluation> {
        return evaluations.values.filter { it.tenantId == tenantId && it.sku.equals(sku, ignoreCase = true) }
    }

    override suspend fun listEvaluationsByState(tenantId: String, state: ReplenishmentTriggerState): List<SubstrateReplenishmentEvaluation> {
        return evaluations.values.filter { it.tenantId == tenantId && it.triggerState == state }
    }

    override suspend fun listAllEvaluations(tenantId: String, limit: Int): List<SubstrateReplenishmentEvaluation> {
        return evaluations.values.filter { it.tenantId == tenantId }.sortedByDescending { it.evaluatedAt }.take(limit)
    }

    override suspend fun updateEvaluationStatus(
        tenantId: String,
        evaluationId: String,
        newState: ReplenishmentTriggerState,
        actor: String
    ): Boolean {
        val existing = evaluations[evaluationId] ?: return false
        if (existing.tenantId != tenantId) return false
        evaluations[evaluationId] = existing.copy(triggerState = newState)
        return true
    }

    override suspend fun saveSupplierAlert(alert: SupplierReorderAlert): SupplierReorderAlert {
        alerts[alert.alertId] = alert
        return alert
    }

    override suspend fun findAlertById(tenantId: String, alertId: String): SupplierReorderAlert? {
        val alert = alerts[alertId]
        return if (alert?.tenantId == tenantId) alert else null
    }

    override suspend fun listAlertsByEvaluation(tenantId: String, evaluationId: String): List<SupplierReorderAlert> {
        return alerts.values.filter { it.tenantId == tenantId && it.evaluationId == evaluationId }
    }

    override suspend fun listAllAlerts(tenantId: String, limit: Int): List<SupplierReorderAlert> {
        return alerts.values.filter { it.tenantId == tenantId }.sortedByDescending { it.dispatchedAt }.take(limit)
    }

    override suspend fun saveAuditEvent(event: SubstrateReplenishmentAuditEvent) {
        val list = auditEvents.computeIfAbsent(event.evaluationId) { mutableListOf() }
        list.add(event)
    }

    override suspend fun listAuditEvents(tenantId: String, evaluationId: String): List<SubstrateReplenishmentAuditEvent> {
        return auditEvents[evaluationId]?.filter { it.tenantId == tenantId } ?: emptyList()
    }
}
