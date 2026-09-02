package com.sucharu.sucharupro.domain.repository.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*

/**
 * Domain repository contract for Substrate Auto-Replenishment Evaluations and Supplier Alerts.
 * Module 19 Step 04.
 */
interface SubstrateReplenishmentRepository {
    suspend fun saveEvaluation(evaluation: SubstrateReplenishmentEvaluation): SubstrateReplenishmentEvaluation
    suspend fun getEvaluationById(tenantId: String, evaluationId: String): SubstrateReplenishmentEvaluation?
    suspend fun getLatestEvaluationByFingerprint(tenantId: String, fingerprint: String): SubstrateReplenishmentEvaluation?
    suspend fun listEvaluationsBySku(tenantId: String, sku: String): List<SubstrateReplenishmentEvaluation>
    suspend fun listEvaluationsByState(tenantId: String, state: ReplenishmentTriggerState): List<SubstrateReplenishmentEvaluation>
    suspend fun listAllEvaluations(tenantId: String, limit: Int = 50): List<SubstrateReplenishmentEvaluation>
    suspend fun updateEvaluationStatus(
        tenantId: String,
        evaluationId: String,
        newState: ReplenishmentTriggerState,
        actor: String
    ): Boolean

    suspend fun saveSupplierAlert(alert: SupplierReorderAlert): SupplierReorderAlert
    suspend fun getAlertById(tenantId: String, alertId: String): SupplierReorderAlert?
    suspend fun listAlertsByEvaluation(tenantId: String, evaluationId: String): List<SupplierReorderAlert>
    suspend fun listAllAlerts(tenantId: String, limit: Int = 50): List<SupplierReorderAlert>

    suspend fun recordAuditEvent(event: SubstrateReplenishmentAuditEvent)
    suspend fun listAuditEvents(tenantId: String, evaluationId: String): List<SubstrateReplenishmentAuditEvent>
}
