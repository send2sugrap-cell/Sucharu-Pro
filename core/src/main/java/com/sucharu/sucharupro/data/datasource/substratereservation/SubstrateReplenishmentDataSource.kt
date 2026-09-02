package com.sucharu.sucharupro.data.datasource.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*

/**
 * Data source interface for persisting Substrate Auto-Replenishment Evaluations and Supplier Alerts.
 * Module 19 Step 04.
 */
interface SubstrateReplenishmentDataSource {
    suspend fun saveEvaluation(evaluation: SubstrateReplenishmentEvaluation): SubstrateReplenishmentEvaluation
    suspend fun findEvaluationById(tenantId: String, evaluationId: String): SubstrateReplenishmentEvaluation?
    suspend fun findLatestEvaluationByFingerprint(tenantId: String, fingerprint: String): SubstrateReplenishmentEvaluation?
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
    suspend fun findAlertById(tenantId: String, alertId: String): SupplierReorderAlert?
    suspend fun listAlertsByEvaluation(tenantId: String, evaluationId: String): List<SupplierReorderAlert>
    suspend fun listAllAlerts(tenantId: String, limit: Int = 50): List<SupplierReorderAlert>

    suspend fun saveAuditEvent(event: SubstrateReplenishmentAuditEvent)
    suspend fun listAuditEvents(tenantId: String, evaluationId: String): List<SubstrateReplenishmentAuditEvent>
}
