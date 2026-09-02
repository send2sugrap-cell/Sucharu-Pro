package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.repository.substratereservation.SubstrateReplenishmentRepository
import java.util.UUID

class SubstrateReplenishmentServiceImpl(
    private val repository: SubstrateReplenishmentRepository
) : SubstrateReplenishmentService {

    override suspend fun evaluateReplenishment(
        tenantId: String,
        input: SubstrateReplenishmentEngine.EvaluationInput
    ): SubstrateReplenishmentEvaluation {
        // Compute condition fingerprint to verify idempotency
        val onHand = maxOf(0L, input.onHandPhysicalSheets)
        val reserved = maxOf(0L, input.activeReservedSheets)
        val inbound = maxOf(0L, input.pendingInboundSheets)
        val demand = maxOf(0L, input.plannedDemandSheets)

        // Evaluate deterministically
        val evaluation = SubstrateReplenishmentEngine.evaluate(input)

        // Check if an identical active evaluation already exists
        val existing = repository.getLatestEvaluationByFingerprint(tenantId, evaluation.deduplicationFingerprint)
        if (existing != null && existing.triggerState != ReplenishmentTriggerState.CANCELLED && existing.triggerState != ReplenishmentTriggerState.COVERED) {
            return existing
        }

        // Persist new evaluation
        val saved = repository.saveEvaluation(evaluation)

        // Record initial audit event
        repository.recordAuditEvent(
            SubstrateReplenishmentAuditEvent(
                auditId = "AUD-${UUID.randomUUID().toString().take(12).uppercase()}",
                evaluationId = saved.evaluationId,
                tenantId = tenantId,
                previousState = ReplenishmentTriggerState.NORMAL,
                newState = saved.triggerState,
                triggerAction = "EVALUATE_REPLENISHMENT",
                actor = input.evaluator,
                timestamp = System.currentTimeMillis(),
                details = "Evaluated substrate replenishment: state=${saved.triggerState}, priority=${saved.priority}, reason=${saved.primaryReason}, recommended=${saved.recommendedReorderSheets} sheets"
            )
        )

        return saved
    }

    override suspend fun triggerSupplierAlert(
        tenantId: String,
        evaluationId: String,
        vendorId: String?,
        actor: String
    ): SupplierReorderAlert {
        val evaluation = repository.getEvaluationById(tenantId, evaluationId)
            ?: throw IllegalArgumentException("Replenishment evaluation $evaluationId not found in tenant $tenantId")

        val targetVendor = if (!vendorId.isNullOrBlank()) {
            evaluation.recommendedSuppliers.firstOrNull { it.vendorId == vendorId }
                ?: throw IllegalArgumentException("Vendor $vendorId is not an approved candidate for evaluation $evaluationId")
        } else {
            evaluation.recommendedSuppliers.firstOrNull()
                ?: throw IllegalStateException("No eligible supplier available to trigger reorder alert for ${evaluation.sku}")
        }

        val alert = SupplierReorderAlert(
            alertId = "ALRT-${UUID.randomUUID().toString().take(12).uppercase()}",
            evaluationId = evaluation.evaluationId,
            tenantId = tenantId,
            vendorId = targetVendor.vendorId,
            vendorCode = targetVendor.vendorCode,
            vendorName = targetVendor.vendorName,
            sku = evaluation.sku,
            materialName = evaluation.materialName,
            requestedSheets = evaluation.recommendedReorderSheets,
            requestedReams = evaluation.recommendedReorderReams,
            targetDeliveryTimestamp = System.currentTimeMillis() + (targetVendor.estimatedLeadTimeDays * 86400000L),
            priority = evaluation.priority,
            status = ReplenishmentTriggerState.SUPPLIER_ALERT_SENT,
            alertPayloadJson = "{\"sku\":\"${evaluation.sku}\",\"sheets\":${evaluation.recommendedReorderSheets},\"reams\":${evaluation.recommendedReorderReams},\"urgency\":\"${evaluation.priority.name}\"}",
            dispatchedBy = actor,
            dispatchedAt = System.currentTimeMillis(),
            acknowledgedAt = null,
            purchaseRequisitionId = "REQ-PO-${UUID.randomUUID().toString().take(8).uppercase()}"
        )

        val savedAlert = repository.saveSupplierAlert(alert)

        // Update evaluation status
        repository.updateEvaluationStatus(
            tenantId = tenantId,
            evaluationId = evaluationId,
            newState = ReplenishmentTriggerState.SUPPLIER_ALERT_SENT,
            actor = actor
        )

        // Record audit
        repository.recordAuditEvent(
            SubstrateReplenishmentAuditEvent(
                auditId = "AUD-${UUID.randomUUID().toString().take(12).uppercase()}",
                evaluationId = evaluationId,
                tenantId = tenantId,
                previousState = evaluation.triggerState,
                newState = ReplenishmentTriggerState.SUPPLIER_ALERT_SENT,
                triggerAction = "DISPATCH_SUPPLIER_ALERT",
                actor = actor,
                timestamp = System.currentTimeMillis(),
                details = "Dispatched reorder alert ${savedAlert.alertId} to vendor ${targetVendor.vendorName} (${targetVendor.vendorCode}) for ${alert.requestedSheets} sheets"
            )
        )

        return savedAlert
    }

    override suspend fun updateReplenishmentStatus(
        tenantId: String,
        evaluationId: String,
        newState: ReplenishmentTriggerState,
        reason: String,
        actor: String
    ): SubstrateReplenishmentEvaluation {
        val existing = repository.getEvaluationById(tenantId, evaluationId)
            ?: throw IllegalArgumentException("Replenishment evaluation $evaluationId not found in tenant $tenantId")

        val previousState = existing.triggerState
        repository.updateEvaluationStatus(tenantId, evaluationId, newState, actor)

        repository.recordAuditEvent(
            SubstrateReplenishmentAuditEvent(
                auditId = "AUD-${UUID.randomUUID().toString().take(12).uppercase()}",
                evaluationId = evaluationId,
                tenantId = tenantId,
                previousState = previousState,
                newState = newState,
                triggerAction = "STATUS_UPDATE",
                actor = actor,
                timestamp = System.currentTimeMillis(),
                details = "Updated state from $previousState to $newState: $reason"
            )
        )

        return repository.getEvaluationById(tenantId, evaluationId)!!
    }

    override suspend fun getEvaluationById(
        tenantId: String,
        evaluationId: String
    ): SubstrateReplenishmentEvaluation? {
        return repository.getEvaluationById(tenantId, evaluationId)
    }

    override suspend fun listEvaluations(
        tenantId: String,
        sku: String?,
        state: ReplenishmentTriggerState?,
        limit: Int
    ): List<SubstrateReplenishmentEvaluation> {
        return when {
            !sku.isNullOrBlank() -> repository.listEvaluationsBySku(tenantId, sku)
            state != null -> repository.listEvaluationsByState(tenantId, state)
            else -> repository.listAllEvaluations(tenantId, limit)
        }
    }

    override suspend fun listAlerts(
        tenantId: String,
        evaluationId: String?,
        limit: Int
    ): List<SupplierReorderAlert> {
        return if (!evaluationId.isNullOrBlank()) {
            repository.listAlertsByEvaluation(tenantId, evaluationId)
        } else {
            repository.listAllAlerts(tenantId, limit)
        }
    }

    override suspend fun exportHandoffContract(
        tenantId: String,
        evaluationId: String
    ): Module19Step04ReplenishmentHandoffContract {
        val evaluation = repository.getEvaluationById(tenantId, evaluationId)
            ?: throw IllegalArgumentException("Evaluation $evaluationId not found in tenant $tenantId")

        val primaryVendor = evaluation.recommendedSuppliers.firstOrNull()

        return Module19Step04ReplenishmentHandoffContract(
            contractVersion = "4.0.0",
            evaluationId = evaluation.evaluationId,
            tenantId = evaluation.tenantId,
            sku = evaluation.sku,
            materialName = evaluation.materialName,
            warehouseId = evaluation.warehouseId,
            onHandPhysicalSheets = evaluation.onHandPhysicalSheets,
            activeReservedSheets = evaluation.activeReservedSheets,
            availableSheets = evaluation.availableSheets,
            netProjectedAvailabilitySheets = evaluation.netProjectedAvailabilitySheets,
            safetyStockSheets = evaluation.safetyStockSheets,
            reorderPointSheets = evaluation.reorderPointSheets,
            isReorderRequired = evaluation.isReorderRequired,
            projectedShortfallSheets = evaluation.projectedShortfallSheets,
            recommendedReorderSheets = evaluation.recommendedReorderSheets,
            recommendedReorderReams = evaluation.recommendedReorderReams,
            triggerState = evaluation.triggerState,
            priority = evaluation.priority,
            primaryReason = evaluation.primaryReason,
            preferredVendorId = primaryVendor?.vendorId,
            preferredVendorName = primaryVendor?.vendorName,
            estimatedLeadTimeDays = primaryVendor?.estimatedLeadTimeDays,
            deduplicationFingerprint = evaluation.deduplicationFingerprint,
            masterIntegrityHash = evaluation.masterIntegrityHash,
            generatedAt = System.currentTimeMillis(),
            auditSummary = "Certified auto-replenishment evaluation for ${evaluation.sku}: state=${evaluation.triggerState}, priority=${evaluation.priority}, reason=${evaluation.primaryReason}, reorder=${evaluation.recommendedReorderSheets} sheets"
        )
    }
}
