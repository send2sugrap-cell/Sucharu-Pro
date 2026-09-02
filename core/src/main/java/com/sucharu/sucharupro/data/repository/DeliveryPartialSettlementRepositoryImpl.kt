package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryPartialSettlementDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryShipmentDataSource
import com.sucharu.sucharupro.data.datasource.DispatchExecutionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlement
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliveryPartialSettlementSummary
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementEvent
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementEventType
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatch
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchLine
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySplitDispatchStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryPartialSettlementRepository
import com.sucharu.sucharupro.domain.service.delivery.DeliveryPartialSettlementCalculator
import com.sucharu.sucharupro.domain.validation.DeliveryPartialSettlementValidator
import com.sucharu.sucharupro.domain.validation.DeliverySettlementAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.DeliverySettlementLifecycleValidator
import com.sucharu.sucharupro.domain.validation.DeliverySettlementOperation
import com.sucharu.sucharupro.domain.validation.DeliverySplitDispatchValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production implementation of [DeliveryPartialSettlementRepository] (Module 08 Step 06).
 */
class DeliveryPartialSettlementRepositoryImpl(
    private val settlementDataSource: DeliveryPartialSettlementDataSource,
    private val doDataSource: DeliveryOrderDataSource,
    private val challanDataSource: DeliveryChallanDataSource? = null,
    private val dispatchDataSource: DispatchExecutionDataSource? = null,
    private val verificationDataSource: DeliveryItemVerificationDataSource? = null,
    private val shipmentDataSource: DeliveryShipmentDataSource? = null
) : DeliveryPartialSettlementRepository {

    private val mutex = Mutex()

    // ──────────────────────────────────────────────────────────────
    // Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeSettlements(projectId: String): Flow<List<DeliveryPartialSettlement>> {
        return settlementDataSource.observeSettlements(projectId)
    }

    override fun observeSettlement(settlementId: String): Flow<DeliveryPartialSettlement?> {
        return settlementDataSource.observeSettlement(settlementId)
    }

    override fun observeSettlementLines(settlementId: String): Flow<List<DeliveryPartialSettlementLine>> {
        return settlementDataSource.observeSettlementLines(settlementId)
    }

    override suspend fun getSettlement(
        settlementId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryPartialSettlement> {
        val settlement = settlementDataSource.getSettlement(settlementId)
            ?: return DomainResult.Error(message = "Settlement '$settlementId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.VIEW,
                targetProjectId = settlement.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        return DomainResult.Success(settlement)
    }

    override suspend fun getSettlementByDeliveryOrder(
        deliveryOrderId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryPartialSettlement> {
        val settlement = settlementDataSource.getSettlementByDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Settlement for Delivery Order '$deliveryOrderId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.VIEW,
                targetProjectId = settlement.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        return DomainResult.Success(settlement)
    }

    override suspend fun getSettlementLines(
        settlementId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryPartialSettlementLine>> {
        val settlement = settlementDataSource.getSettlement(settlementId)
            ?: return DomainResult.Error(message = "Settlement '$settlementId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.VIEW,
                targetProjectId = settlement.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lines = settlementDataSource.getSettlementLines(settlementId)
        return DomainResult.Success(lines)
    }

    override fun observeSplitDispatches(deliveryOrderId: String): Flow<List<DeliverySplitDispatch>> {
        return settlementDataSource.observeSplitDispatches(deliveryOrderId)
    }

    override suspend fun getSplitDispatches(
        deliveryOrderId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliverySplitDispatch>> {
        val doOrder = doDataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery Order '$deliveryOrderId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.VIEW,
                targetProjectId = doOrder.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val splits = settlementDataSource.getSplitDispatches(deliveryOrderId)
        return DomainResult.Success(splits)
    }

    override suspend fun getSplitDispatch(
        splitDispatchId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliverySplitDispatch> {
        val split = settlementDataSource.getSplitDispatch(splitDispatchId)
            ?: return DomainResult.Error(message = "Split Dispatch '$splitDispatchId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.VIEW,
                targetProjectId = split.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        return DomainResult.Success(split)
    }

    override suspend fun getSplitDispatchLines(
        splitDispatchId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliverySplitDispatchLine>> {
        val split = settlementDataSource.getSplitDispatch(splitDispatchId)
            ?: return DomainResult.Error(message = "Split Dispatch '$splitDispatchId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.VIEW,
                targetProjectId = split.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lines = settlementDataSource.getSplitDispatchLines(splitDispatchId)
        return DomainResult.Success(lines)
    }

    override fun observeEvents(settlementId: String): Flow<List<DeliverySettlementEvent>> {
        return settlementDataSource.observeEvents(settlementId)
    }

    override suspend fun getEvents(
        settlementId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliverySettlementEvent>> {
        val settlement = settlementDataSource.getSettlement(settlementId)
            ?: return DomainResult.Error(message = "Settlement '$settlementId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.VIEW,
                targetProjectId = settlement.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val events = settlementDataSource.getEvents(settlementId)
        return DomainResult.Success(events)
    }

    override suspend fun getSettlementSummary(
        projectId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryPartialSettlementSummary> {
        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.VIEW,
                targetProjectId = projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val settlements = settlementDataSource.observeSettlements(projectId).first()
        var totalSplits = 0
        for (s in settlements) {
            totalSplits += settlementDataSource.getSplitDispatches(s.deliveryOrderId).size
        }

        val summary = DeliveryPartialSettlementSummary(
            totalSettlements = settlements.size,
            openCount = settlements.count { it.status == DeliverySettlementStatus.OPEN },
            partiallyDeliveredCount = settlements.count { it.status == DeliverySettlementStatus.PARTIALLY_DELIVERED },
            fullyDeliveredCount = settlements.count { it.status == DeliverySettlementStatus.FULLY_DELIVERED },
            partiallyReturnedCount = settlements.count { it.status == DeliverySettlementStatus.PARTIALLY_RETURNED },
            settlementPendingCount = settlements.count { it.status == DeliverySettlementStatus.SETTLEMENT_PENDING },
            settledCount = settlements.count { it.status == DeliverySettlementStatus.SETTLED },
            disputedCount = settlements.count { it.status == DeliverySettlementStatus.DISPUTED },
            cancelledCount = settlements.count { it.status == DeliverySettlementStatus.CANCELLED },
            totalOrderedQuantity = settlements.sumOf { it.totalOrderedQuantity },
            totalDeliveredQuantity = settlements.sumOf { it.totalDeliveredQuantity },
            totalPendingQuantity = settlements.sumOf { it.totalPendingQuantity },
            totalSplitDispatches = totalSplits
        )

        return DomainResult.Success(summary)
    }

    // ──────────────────────────────────────────────────────────────
    // Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun initializeSettlementForDeliveryOrder(
        deliveryOrderId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryPartialSettlement> = mutex.withLock {
        internalInitializeSettlement(
            deliveryOrderId = deliveryOrderId,
            actorId = actorId,
            callerRole = callerRole,
            callerProjectId = callerProjectId
        )
    }

    /**
     * IMPORTANT:
     * Caller must already hold repository mutex.
     * This helper intentionally does not acquire the mutex
     * to avoid recursive Mutex deadlock.
     */
    private suspend fun internalInitializeSettlement(
        deliveryOrderId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryPartialSettlement> {
        val doOrder = doDataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery Order '$deliveryOrderId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.CREATE,
                targetProjectId = doOrder.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val existing = settlementDataSource.getSettlementByDeliveryOrder(deliveryOrderId)
        if (existing != null) {
            return DomainResult.Error(
                message = "Settlement already exists for Delivery Order '$deliveryOrderId' (Settlement ID: '${existing.settlementId}')."
            )
        }

        val eligibility = DeliveryPartialSettlementValidator.validateDeliveryOrderEligibility(doOrder, doOrder.projectId)
        if (eligibility is DomainResult.Error) return eligibility

        val doLines = doDataSource.getDeliveryOrderLines(deliveryOrderId)
        if (doLines.isEmpty()) {
            return DomainResult.Error(message = "Cannot initialize settlement: Delivery Order '$deliveryOrderId' has no lines.")
        }

        val settlementId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // Fetch any existing Challan/Dispatch/Verification records if available
        val challans = challanDataSource?.observeChallans(doOrder.projectId)?.first()?.filter { it.deliveryOrderId == deliveryOrderId } ?: emptyList()
        val challanLines = challans.flatMap { challanDataSource?.getChallanLines(it.challanId) ?: emptyList() }

        val dispatches = dispatchDataSource?.observeDispatches(doOrder.projectId)?.first()?.filter { it.deliveryOrderId == deliveryOrderId } ?: emptyList()
        val dispatchLines = dispatches.flatMap { dispatchDataSource?.getDispatchLines(it.dispatchExecutionId) ?: emptyList() }

        val verifications = verificationDataSource?.observeVerifications(doOrder.projectId)?.first()?.filter { it.deliveryOrderId == deliveryOrderId } ?: emptyList()
        val verificationLines = verifications.flatMap { verificationDataSource?.getVerificationLines(it.verificationId) ?: emptyList() }

        val settlementLines = doLines.map { doLine ->
            DeliveryPartialSettlementCalculator.calculateLineSettlement(
                settlementId = settlementId,
                orderLine = doLine,
                challanLines = challanLines,
                dispatchLines = dispatchLines,
                verificationLines = verificationLines,
                timestamp = now
            )
        }

        val aggregate = DeliveryPartialSettlementCalculator.calculateAggregateSettlement(
            settlementId = settlementId,
            projectId = doOrder.projectId,
            deliveryOrderId = deliveryOrderId,
            customerId = doOrder.customerId,
            lines = settlementLines,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val validation = DeliveryPartialSettlementValidator.validateSettlement(aggregate, settlementLines)
        if (validation is DomainResult.Error) return validation

        settlementDataSource.insertSettlement(aggregate, settlementLines)

        val event = DeliverySettlementEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = doOrder.projectId,
            settlementId = settlementId,
            eventType = DeliverySettlementEventType.CREATED,
            referenceId = deliveryOrderId,
            actorId = actorId,
            timestamp = now,
            details = "Settlement initialized for Delivery Order '${doOrder.deliveryOrderNo}'."
        )
        settlementDataSource.insertEvent(event)

        return DomainResult.Success(aggregate)
    }

    override suspend fun createSplitDispatch(
        deliveryOrderId: String,
        lines: List<DeliverySplitDispatchLine>,
        deliveryChallanId: String?,
        dispatchExecutionId: String?,
        shipmentId: String?,
        notes: String?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliverySplitDispatch> = mutex.withLock {
        val doOrder = doDataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery Order '$deliveryOrderId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.CREATE_SPLIT,
                targetProjectId = doOrder.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // Get or initialize settlement
        var settlement = settlementDataSource.getSettlementByDeliveryOrder(deliveryOrderId)
        if (settlement == null) {
            val initRes = internalInitializeSettlement(
                deliveryOrderId = deliveryOrderId,
                actorId = actorId,
                callerRole = null, // Caller already authorized for CREATE_SPLIT
                callerProjectId = callerProjectId
            )
            if (initRes is DomainResult.Error) return DomainResult.Error(message = initRes.message)
            settlement = (initRes as DomainResult.Success).data
        }

        if (settlement.status.isTerminal) {
            return DomainResult.Error(
                message = "Cannot create split dispatch against terminal settlement '${settlement.settlementId}' (${settlement.status.defaultLabel})."
            )
        }

        val existingSplits = settlementDataSource.getSplitDispatches(deliveryOrderId)
        val nextSeq = (existingSplits.maxOfOrNull { it.splitSequence } ?: 0) + 1
        val now = System.currentTimeMillis()
        val splitId = UUID.randomUUID().toString()

        val split = DeliverySplitDispatch(
            splitDispatchId = splitId,
            projectId = doOrder.projectId,
            deliveryOrderId = deliveryOrderId,
            deliveryChallanId = deliveryChallanId,
            dispatchExecutionId = dispatchExecutionId,
            shipmentId = shipmentId,
            splitSequence = nextSeq,
            status = DeliverySplitDispatchStatus.APPROVED,
            notes = notes,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val settlementLines = settlementDataSource.getSettlementLines(settlement.settlementId)
        val assignedLines = lines.map { it.copy(splitDispatchId = splitId, projectId = doOrder.projectId) }

        val validation = DeliverySplitDispatchValidator.validateSplitDispatch(
            split = split,
            lines = assignedLines,
            existingSplits = existingSplits,
            settlementLines = settlementLines
        )
        if (validation is DomainResult.Error) return validation

        settlementDataSource.insertSplitDispatch(split, assignedLines)

        // Event for split
        val event = DeliverySettlementEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = doOrder.projectId,
            settlementId = settlement.settlementId,
            eventType = DeliverySettlementEventType.SPLIT_CREATED,
            referenceId = splitId,
            actorId = actorId,
            timestamp = now,
            details = "Created split dispatch #$nextSeq for Delivery Order '${doOrder.deliveryOrderNo}'."
        )
        settlementDataSource.insertEvent(event)

        DomainResult.Success(split)
    }

    override suspend fun recordPartialDelivery(
        settlementId: String,
        deliveryOrderLineId: String,
        deliveredQuantity: Double,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryPartialSettlement> = mutex.withLock {
        val existing = settlementDataSource.getSettlement(settlementId)
            ?: return DomainResult.Error(message = "Settlement '$settlementId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.RECORD_PARTIAL,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        if (existing.status.isTerminal) {
            return DomainResult.Error(
                message = "Cannot record partial delivery against terminal settlement '${existing.settlementId}' (${existing.status.defaultLabel})."
            )
        }

        if (deliveredQuantity <= 0) {
            return DomainResult.Error(message = "Delivered quantity must be strictly positive (> 0).")
        }

        val lines = settlementDataSource.getSettlementLines(settlementId)
        val targetIndex = lines.indexOfFirst { it.deliveryOrderLineId == deliveryOrderLineId }
        if (targetIndex < 0) {
            return DomainResult.Error(
                message = "Delivery Order Line '$deliveryOrderLineId' not found in settlement '$settlementId'."
            )
        }

        val targetLine = lines[targetIndex]
        val newDelivered = targetLine.deliveredQuantity + deliveredQuantity
        if (newDelivered > targetLine.orderedQuantity) {
            return DomainResult.Error(
                message = "Over-delivery rejected: Requested +$deliveredQuantity brings delivered quantity ($newDelivered) over ordered quantity (${targetLine.orderedQuantity}) for line '$deliveryOrderLineId'."
            )
        }

        val now = System.currentTimeMillis()
        val newPending = (targetLine.orderedQuantity - newDelivered).coerceAtLeast(0.0)
        val lineStatus = when {
            newDelivered >= targetLine.orderedQuantity && newPending <= 0.0 -> DeliverySettlementStatus.FULLY_DELIVERED
            else -> DeliverySettlementStatus.PARTIALLY_DELIVERED
        }

        val updatedLine = targetLine.copy(
            deliveredQuantity = newDelivered,
            pendingQuantity = newPending,
            status = lineStatus,
            updatedAt = now
        )

        val updatedLines = lines.toMutableList()
        updatedLines[targetIndex] = updatedLine

        val aggregate = DeliveryPartialSettlementCalculator.calculateAggregateSettlement(
            settlementId = existing.settlementId,
            projectId = existing.projectId,
            deliveryOrderId = existing.deliveryOrderId,
            customerId = existing.customerId,
            lines = updatedLines,
            currentStatus = existing.status,
            version = existing.settlementVersion + 1,
            createdBy = existing.createdBy,
            createdAt = existing.createdAt,
            updatedBy = actorId,
            updatedAt = now
        )

        val immutabilityCheck = DeliveryPartialSettlementValidator.validateImmutableIdentity(existing, aggregate)
        if (immutabilityCheck is DomainResult.Error) return immutabilityCheck

        settlementDataSource.updateSettlement(aggregate, updatedLines)

        val eventType = if (updatedLine.isFullyDelivered) DeliverySettlementEventType.DELIVERY_COMPLETED else DeliverySettlementEventType.PARTIAL_DELIVERY_RECORDED
        val event = DeliverySettlementEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            settlementId = existing.settlementId,
            eventType = eventType,
            referenceId = deliveryOrderLineId,
            actorId = actorId,
            timestamp = now,
            details = "Recorded delivery +$deliveredQuantity (Total delivered: $newDelivered / ${targetLine.orderedQuantity})."
        )
        settlementDataSource.insertEvent(event)

        DomainResult.Success(aggregate)
    }

    override suspend fun recalculateSettlement(
        settlementId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryPartialSettlement> = mutex.withLock {
        val existing = settlementDataSource.getSettlement(settlementId)
            ?: return DomainResult.Error(message = "Settlement '$settlementId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.RECALCULATE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val doLines = doDataSource.getDeliveryOrderLines(existing.deliveryOrderId)
        val now = System.currentTimeMillis()

        // Fetch Challans, Dispatches, Verifications, Splits for DO
        val challans = challanDataSource?.observeChallans(existing.projectId)?.first()?.filter { it.deliveryOrderId == existing.deliveryOrderId } ?: emptyList()
        val challanLines = challans.flatMap { challanDataSource?.getChallanLines(it.challanId) ?: emptyList() }

        val dispatches = dispatchDataSource?.observeDispatches(existing.projectId)?.first()?.filter { it.deliveryOrderId == existing.deliveryOrderId } ?: emptyList()
        val dispatchLines = dispatches.flatMap { dispatchDataSource?.getDispatchLines(it.dispatchExecutionId) ?: emptyList() }

        val verifications = verificationDataSource?.observeVerifications(existing.projectId)?.first()?.filter { it.deliveryOrderId == existing.deliveryOrderId } ?: emptyList()
        val verificationLines = verifications.flatMap { verificationDataSource?.getVerificationLines(it.verificationId) ?: emptyList() }

        val splits = settlementDataSource.getSplitDispatches(existing.deliveryOrderId)
        val splitLines = splits.flatMap { settlementDataSource.getSplitDispatchLines(it.splitDispatchId) }

        val existingLines = settlementDataSource.getSettlementLines(settlementId)

        val recalculatedLines = doLines.map { doLine ->
            val prevLine = existingLines.find { it.deliveryOrderLineId == doLine.lineId }
            DeliveryPartialSettlementCalculator.calculateLineSettlement(
                settlementId = settlementId,
                orderLine = doLine,
                challanLines = challanLines,
                dispatchLines = dispatchLines,
                verificationLines = verificationLines,
                splitLines = splitLines,
                recordedDeliveredQuantity = prevLine?.deliveredQuantity ?: 0.0,
                returnedQuantity = prevLine?.returnedQuantity ?: 0.0,
                replacementQuantity = prevLine?.replacementQuantity ?: 0.0,
                timestamp = now
            )
        }

        val aggregate = DeliveryPartialSettlementCalculator.calculateAggregateSettlement(
            settlementId = existing.settlementId,
            projectId = existing.projectId,
            deliveryOrderId = existing.deliveryOrderId,
            customerId = existing.customerId,
            lines = recalculatedLines,
            currentStatus = existing.status,
            version = existing.settlementVersion + 1,
            createdBy = existing.createdBy,
            createdAt = existing.createdAt,
            updatedBy = actorId,
            updatedAt = now
        )

        settlementDataSource.updateSettlement(aggregate, recalculatedLines)

        val event = DeliverySettlementEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            settlementId = existing.settlementId,
            eventType = DeliverySettlementEventType.SETTLEMENT_RECALCULATED,
            actorId = actorId,
            timestamp = now,
            details = "Settlement recalculated against latest supply chain records."
        )
        settlementDataSource.insertEvent(event)

        DomainResult.Success(aggregate)
    }

    override suspend fun finalizeSettlement(
        settlementId: String,
        notes: String?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryPartialSettlement> = mutex.withLock {
        val existing = settlementDataSource.getSettlement(settlementId)
            ?: return DomainResult.Error(message = "Settlement '$settlementId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.FINALIZE_SETTLEMENT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliverySettlementLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DeliverySettlementStatus.SETTLED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val lines = settlementDataSource.getSettlementLines(settlementId)

        val updated = existing.copy(
            status = DeliverySettlementStatus.SETTLED,
            settlementVersion = existing.settlementVersion + 1,
            updatedBy = actorId,
            updatedAt = now
        )

        settlementDataSource.updateSettlement(updated, lines)

        val event = DeliverySettlementEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            settlementId = existing.settlementId,
            eventType = DeliverySettlementEventType.SETTLED,
            actorId = actorId,
            timestamp = now,
            details = notes ?: "Settlement successfully finalized and closed."
        )
        settlementDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun disputeSettlement(
        settlementId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryPartialSettlement> = mutex.withLock {
        val existing = settlementDataSource.getSettlement(settlementId)
            ?: return DomainResult.Error(message = "Settlement '$settlementId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.DISPUTE_SETTLEMENT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliverySettlementLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DeliverySettlementStatus.DISPUTED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val lines = settlementDataSource.getSettlementLines(settlementId)

        val updated = existing.copy(
            status = DeliverySettlementStatus.DISPUTED,
            settlementVersion = existing.settlementVersion + 1,
            updatedBy = actorId,
            updatedAt = now
        )

        settlementDataSource.updateSettlement(updated, lines)

        val event = DeliverySettlementEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            settlementId = existing.settlementId,
            eventType = DeliverySettlementEventType.DISPUTED,
            actorId = actorId,
            timestamp = now,
            details = "Settlement disputed: $reason"
        )
        settlementDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun cancelSettlement(
        settlementId: String,
        reason: String?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryPartialSettlement> = mutex.withLock {
        val existing = settlementDataSource.getSettlement(settlementId)
            ?: return DomainResult.Error(message = "Settlement '$settlementId' not found.")

        if (callerRole != null) {
            val authCheck = DeliverySettlementAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliverySettlementOperation.CANCEL,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliverySettlementLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DeliverySettlementStatus.CANCELLED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val lines = settlementDataSource.getSettlementLines(settlementId)

        val updated = existing.copy(
            status = DeliverySettlementStatus.CANCELLED,
            settlementVersion = existing.settlementVersion + 1,
            updatedBy = actorId,
            updatedAt = now
        )

        settlementDataSource.updateSettlement(updated, lines)

        val event = DeliverySettlementEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            settlementId = existing.settlementId,
            eventType = DeliverySettlementEventType.CANCELLED,
            actorId = actorId,
            timestamp = now,
            details = reason ?: "Settlement cancelled."
        )
        settlementDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }
}
