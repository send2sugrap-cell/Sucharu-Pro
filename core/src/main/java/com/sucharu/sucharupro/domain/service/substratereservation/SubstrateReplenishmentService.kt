package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*

/**
 * Service Contract for Substrate Auto-Replenishment Triggers & Supplier Reorder Alerts.
 * Module 19 Step 04.
 */
interface SubstrateReplenishmentService {

    /**
     * Evaluates substrate stock state, policy thresholds, and supplier ranking.
     * Idempotent: returns existing evaluation if condition fingerprint matches.
     */
    suspend fun evaluateReplenishment(
        tenantId: String,
        input: SubstrateReplenishmentEngine.EvaluationInput
    ): SubstrateReplenishmentEvaluation

    /**
     * Manually or automatically dispatches a Supplier Reorder Alert to Module 12/13.
     */
    suspend fun triggerSupplierAlert(
        tenantId: String,
        evaluationId: String,
        vendorId: String? = null,
        actor: String
    ): SupplierReorderAlert

    /**
     * Updates replenishment evaluation lifecycle state.
     */
    suspend fun updateReplenishmentStatus(
        tenantId: String,
        evaluationId: String,
        newState: ReplenishmentTriggerState,
        reason: String,
        actor: String
    ): SubstrateReplenishmentEvaluation

    suspend fun getEvaluationById(
        tenantId: String,
        evaluationId: String
    ): SubstrateReplenishmentEvaluation?

    suspend fun listEvaluations(
        tenantId: String,
        sku: String? = null,
        state: ReplenishmentTriggerState? = null,
        limit: Int = 50
    ): List<SubstrateReplenishmentEvaluation>

    suspend fun listAlerts(
        tenantId: String,
        evaluationId: String? = null,
        limit: Int = 50
    ): List<SupplierReorderAlert>

    suspend fun exportHandoffContract(
        tenantId: String,
        evaluationId: String
    ): Module19Step04ReplenishmentHandoffContract
}
