package com.sucharu.sucharupro.domain.service.productionplanning

import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuote
import com.sucharu.sucharupro.domain.model.printingquote.PrintingQuoteVersion
import com.sucharu.sucharupro.domain.model.productionplanning.*
import com.sucharu.sucharupro.domain.repository.OrderRepository
import com.sucharu.sucharupro.domain.repository.commercialcommitment.CommercialCommitmentRepository
import com.sucharu.sucharupro.domain.repository.printingquote.PrintingQuoteRepository
import com.sucharu.sucharupro.domain.repository.productionplanning.ProductionPlanningRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.UUID

class ProductionPlanningServiceImpl(
    private val planningRepository: ProductionPlanningRepository,
    private val orderRepository: OrderRepository,
    private val commitmentRepository: CommercialCommitmentRepository,
    private val quoteRepository: PrintingQuoteRepository
) : ProductionPlanningService {

    private val mutex = Mutex()

    private suspend fun resolveSources(
        tenantId: String,
        orderId: String,
        orderItemId: String?
    ): Pair<Order, OrderItem> {
        val order = when (val res = orderRepository.findOrderById(orderId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> throw NoSuchElementException(res.message)
            DomainResult.Loading -> throw IllegalStateException("Unexpected loading state")
        }
        val item = if (orderItemId != null) {
            order.items.find { it.itemId == orderItemId }
                ?: throw NoSuchElementException("Order item '$orderItemId' not found in order '$orderId'.")
        } else {
            order.items.firstOrNull()
                ?: throw IllegalStateException("Order '$orderId' has no items to plan.")
        }
        return order to item
    }

    private suspend fun fetchQuoteAndVersion(
        tenantId: String,
        quotationId: String?
    ): Pair<PrintingQuote?, PrintingQuoteVersion?> {
        if (quotationId == null) return null to null
        val quote = when (val res = quoteRepository.findQuoteById(tenantId, quotationId)) {
            is DomainResult.Success -> res.data
            else -> null
        }
        val version = if (quote != null) {
            when (val vRes = quoteRepository.listVersionsByQuoteId(tenantId, quote.quoteId)) {
                is DomainResult.Success -> vRes.data.find { it.versionNumber == quote.currentVersion } ?: vRes.data.maxByOrNull { it.versionNumber }
                else -> null
            }
        } else null
        return quote to version
    }

    private suspend fun fetchCommitment(
        tenantId: String,
        quotationId: String?,
        commitmentId: String?
    ): CommercialCommitment? {
        if (commitmentId != null) {
            return when (val res = commitmentRepository.findCommitmentById(tenantId, commitmentId)) {
                is DomainResult.Success -> res.data
                else -> null
            }
        }
        if (quotationId != null) {
            return when (val res = commitmentRepository.findCommitmentByQuotation(tenantId, quotationId)) {
                is DomainResult.Success -> res.data
                else -> null
            }
        }
        return null
    }

    override suspend fun evaluateReadiness(
        tenantId: String,
        orderId: String,
        orderItemId: String?
    ): DomainResult<ManufacturingReadinessEvaluation> {
        return try {
            val (order, item) = resolveSources(tenantId, orderId, orderItemId)
            val (quote, version) = fetchQuoteAndVersion(tenantId, order.quotationId)
            val commitment = fetchCommitment(tenantId, order.quotationId, null)

            val spec = ProductionPlanningEngine.normalizeSpecification(order, item, quote, version, null)
            val machines = ProductionPlanningEngine.evaluateMachineCompatibility(spec)
            val ops = ProductionPlanningEngine.deriveRouting("PLAN-PREVIEW", spec)
            val feasibility = ProductionPlanningEngine.evaluateDueDateFeasibility(null, ops.sumOf { it.estimatedRunMinutes + it.estimatedSetupMinutes })

            val evaluation = ProductionPlanningEngine.evaluateManufacturingReadiness(
                order = order,
                item = item,
                commitment = commitment,
                spec = spec,
                machines = machines,
                operations = ops,
                feasibility = feasibility
            )
            DomainResult.Success(evaluation)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to evaluate readiness")
        }
    }

    override suspend fun createPlanningSnapshot(
        tenantId: String,
        orderId: String,
        orderItemId: String?,
        requestedBy: String,
        idempotencyKey: String?
    ): DomainResult<ProductionPlanningSnapshot> = mutex.withLock {
        try {
            // Idempotency check
            if (idempotencyKey != null) {
                val existing = planningRepository.getPlanningSnapshotByIdempotencyKey(tenantId, idempotencyKey)
                if (existing != null) {
                    return@withLock DomainResult.Success(existing)
                }
            }

            val (order, item) = resolveSources(tenantId, orderId, orderItemId)
            val (quote, version) = fetchQuoteAndVersion(tenantId, order.quotationId)
            val commitment = fetchCommitment(tenantId, order.quotationId, null)

            val latestPlan = planningRepository.getLatestPlanningSnapshotByOrder(tenantId, orderId)
            val nextVersion = (latestPlan?.version ?: 0) + 1

            val planningId = "PLAN-${order.orderId}-${item.itemId}-V$nextVersion"
            val spec = ProductionPlanningEngine.normalizeSpecification(order, item, quote, version, null)
            val reqs = ProductionPlanningEngine.deriveRequirements(planningId, spec)
            val machines = ProductionPlanningEngine.evaluateMachineCompatibility(spec)
            val ops = ProductionPlanningEngine.deriveRouting(planningId, spec)
            val feasibility = ProductionPlanningEngine.evaluateDueDateFeasibility(null, ops.sumOf { it.estimatedRunMinutes + it.estimatedSetupMinutes })

            val evaluation = ProductionPlanningEngine.evaluateManufacturingReadiness(
                order = order,
                item = item,
                commitment = commitment,
                spec = spec,
                machines = machines,
                operations = ops,
                feasibility = feasibility
            )

            val status = if (evaluation.isManufacturingReady) {
                PlanningStatus.READY
            } else if (evaluation.blockingIssuesCount > 0) {
                PlanningStatus.BLOCKED
            } else {
                PlanningStatus.REQUIRES_REVIEW
            }

            val planningFingerprint = ProductionPlanningMathUtils.generateFingerprint(
                tenantId = tenantId,
                orderId = orderId,
                orderItemId = item.itemId,
                specFingerprint = spec.specFingerprint,
                orderedQuantity = spec.orderedQuantity,
                plannedQuantity = spec.plannedQuantity,
                status = status.name
            )

            val now = System.currentTimeMillis()
            val integrityHash = ProductionPlanningMathUtils.sha256(
                "$planningId|$tenantId|$orderId|${spec.orderedQuantity}|${spec.plannedQuantity}|$planningFingerprint|$now"
            )

            val snapshot = ProductionPlanningSnapshot(
                planningId = planningId,
                tenantId = tenantId,
                projectId = tenantId,
                orderId = order.orderId,
                orderNumber = order.orderNumber,
                orderItemId = item.itemId,
                commercialCommitmentId = commitment?.commitmentId,
                quotationId = quote?.quoteId,
                quotationVersionNumber = version?.versionNumber,
                customerId = order.customerId,
                status = status,
                version = nextVersion,
                isCurrent = true,
                readinessScore = evaluation.overallScore,
                feasibilityStatus = feasibility,
                specification = spec,
                requirements = reqs,
                operations = ops,
                diagnostics = evaluation.diagnostics,
                machineCompatibility = machines,
                orderRequestedDate = null,
                estimatedCompletionDate = now + (ops.sumOf { it.estimatedRunMinutes + it.estimatedSetupMinutes } * 60 * 1000L),
                planningFingerprint = planningFingerprint,
                integrityHash = integrityHash,
                createdAt = now,
                updatedAt = now,
                createdBy = requestedBy
            )

            val saved = planningRepository.savePlanningSnapshot(snapshot, idempotencyKey)

            // Audit event
            planningRepository.savePlanningEvent(
                ProductionPlanningEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    planningId = saved.planningId,
                    tenantId = tenantId,
                    eventType = ProductionPlanningEventType.PLANNING_CREATED,
                    fromStatus = null,
                    toStatus = saved.status,
                    eventPayload = "Created production planning snapshot v$nextVersion with score ${saved.readinessScore}",
                    performedBy = requestedBy,
                    performedAt = now
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to create planning snapshot")
        }
    }

    override suspend fun getPlanningSnapshot(
        tenantId: String,
        planningId: String
    ): DomainResult<ProductionPlanningSnapshot?> {
        return try {
            val snapshot = planningRepository.getPlanningSnapshotById(tenantId, planningId)
            DomainResult.Success(snapshot)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get planning snapshot")
        }
    }

    override suspend fun getLatestPlanningSnapshotByOrder(
        tenantId: String,
        orderId: String
    ): DomainResult<ProductionPlanningSnapshot?> {
        return try {
            val snapshot = planningRepository.getLatestPlanningSnapshotByOrder(tenantId, orderId)
            DomainResult.Success(snapshot)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get latest planning snapshot")
        }
    }

    override suspend fun listPlanningSnapshots(
        tenantId: String,
        orderId: String
    ): DomainResult<List<ProductionPlanningSnapshot>> {
        return try {
            val list = planningRepository.listPlanningSnapshotsByOrder(tenantId, orderId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list planning snapshots")
        }
    }

    override suspend fun reconcilePlanning(
        tenantId: String,
        planningId: String
    ): DomainResult<ProductionPlanningReconciliationResult> {
        return try {
            val plan = planningRepository.getPlanningSnapshotById(tenantId, planningId)
                ?: throw NoSuchElementException("Planning snapshot '$planningId' not found.")
            val order = when (val res = orderRepository.findOrderById(plan.orderId)) {
                is DomainResult.Success -> res.data
                is DomainResult.Error -> throw NoSuchElementException(res.message)
                DomainResult.Loading -> throw IllegalStateException("Unexpected loading state")
            }
            val commitment = fetchCommitment(tenantId, plan.quotationId, plan.commercialCommitmentId)
            val (quote, version) = fetchQuoteAndVersion(tenantId, plan.quotationId)

            val result = ProductionPlanningReconciliationService.reconcile(
                tenantId = tenantId,
                plan = plan,
                order = order,
                commitment = commitment,
                quote = quote,
                version = version
            )

            // Audit
            planningRepository.savePlanningEvent(
                ProductionPlanningEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    planningId = plan.planningId,
                    tenantId = tenantId,
                    eventType = ProductionPlanningEventType.RECONCILIATION_PERFORMED,
                    fromStatus = plan.status,
                    toStatus = plan.status,
                    eventPayload = "Reconciliation result: isFullyReconciled=${result.isFullyReconciled}, discrepancies=${result.discrepancies.size}",
                    performedBy = "system",
                    performedAt = System.currentTimeMillis()
                )
            )

            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to reconcile planning snapshot")
        }
    }

    override suspend fun supersedePlanning(
        tenantId: String,
        planningId: String,
        reason: String,
        supersededBy: String
    ): DomainResult<ProductionPlanningSnapshot> = mutex.withLock {
        try {
            val existing = planningRepository.getPlanningSnapshotById(tenantId, planningId)
                ?: throw NoSuchElementException("Planning snapshot '$planningId' not found.")

            val updated = existing.copy(
                status = PlanningStatus.SUPERSEDED,
                isCurrent = false,
                updatedAt = System.currentTimeMillis()
            )
            val saved = planningRepository.savePlanningSnapshot(updated)

            planningRepository.savePlanningEvent(
                ProductionPlanningEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    planningId = saved.planningId,
                    tenantId = tenantId,
                    eventType = ProductionPlanningEventType.PLANNING_SUPERSEDED,
                    fromStatus = existing.status,
                    toStatus = PlanningStatus.SUPERSEDED,
                    eventPayload = "Superseded: $reason",
                    performedBy = supersededBy,
                    performedAt = System.currentTimeMillis()
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to supersede planning snapshot")
        }
    }

    override suspend fun handoffPlanning(
        tenantId: String,
        planningId: String,
        handedOffBy: String
    ): DomainResult<ProductionPlanningSnapshot> = mutex.withLock {
        try {
            val existing = planningRepository.getPlanningSnapshotById(tenantId, planningId)
                ?: throw NoSuchElementException("Planning snapshot '$planningId' not found.")

            require(existing.status == PlanningStatus.READY) {
                "Cannot handoff planning snapshot in status '${existing.status}'. Snapshot must be in 'READY' state."
            }

            val updated = existing.copy(
                status = PlanningStatus.HANDED_OFF,
                updatedAt = System.currentTimeMillis()
            )
            val saved = planningRepository.savePlanningSnapshot(updated)

            planningRepository.savePlanningEvent(
                ProductionPlanningEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    planningId = saved.planningId,
                    tenantId = tenantId,
                    eventType = ProductionPlanningEventType.PLANNING_HANDED_OFF,
                    fromStatus = existing.status,
                    toStatus = PlanningStatus.HANDED_OFF,
                    eventPayload = "Planning snapshot handed off to downstream Production Job Engine.",
                    performedBy = handedOffBy,
                    performedAt = System.currentTimeMillis()
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to handoff planning snapshot")
        }
    }

    override suspend fun exportHandoffContract(
        tenantId: String,
        planningId: String
    ): DomainResult<Module17Step04ProductionPlanningHandoffContract> {
        return try {
            val plan = planningRepository.getPlanningSnapshotById(tenantId, planningId)
                ?: throw NoSuchElementException("Planning snapshot '$planningId' not found.")

            val reconResult = reconcilePlanning(tenantId, planningId)
            val reconStatus = if (reconResult is DomainResult.Success && reconResult.data.isFullyReconciled) "RECONCILED" else "UNRECONCILED"

            val contract = Module17Step04ProductionPlanningHandoffContract(
                contractVersion = "1.0.0",
                planningId = plan.planningId,
                tenantId = plan.tenantId,
                projectId = plan.projectId,
                orderId = plan.orderId,
                orderNumber = plan.orderNumber,
                customerId = plan.customerId,
                planningStatus = plan.status.name,
                readinessScore = plan.readinessScore,
                isManufacturingReady = plan.status == PlanningStatus.READY || plan.status == PlanningStatus.HANDED_OFF,
                feasibilityStatus = plan.feasibilityStatus.name,
                jobTitle = plan.specification.jobTitle,
                orderedQuantity = plan.specification.orderedQuantity,
                plannedQuantity = plan.specification.plannedQuantity,
                primaryWorkCenter = plan.operations.find { it.stageType == com.sucharu.sucharupro.domain.model.production.ProductionStageType.PRINTING }?.targetWorkCenter ?: "OFFSET_BAY",
                totalEstimatedRunMinutes = plan.operations.sumOf { it.estimatedRunMinutes + it.estimatedSetupMinutes },
                operationsCount = plan.operations.size,
                blockingIssues = plan.diagnostics.filter { it.isBlocking }.map { it.message },
                warnings = plan.diagnostics.filter { !it.isBlocking }.map { it.message },
                reconciliationStatus = reconStatus,
                integrityHash = plan.integrityHash,
                generatedAt = System.currentTimeMillis()
            )

            DomainResult.Success(contract)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to export handoff contract")
        }
    }

    override suspend fun listPlanningEvents(
        tenantId: String,
        planningId: String
    ): DomainResult<List<ProductionPlanningEvent>> {
        return try {
            val events = planningRepository.listPlanningEvents(tenantId, planningId)
            DomainResult.Success(events)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list planning events")
        }
    }
}
