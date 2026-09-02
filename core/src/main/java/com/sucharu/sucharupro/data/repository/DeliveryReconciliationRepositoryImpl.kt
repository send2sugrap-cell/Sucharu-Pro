package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryPartialSettlementDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryProofDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryReconciliationDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryReturnDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryShipmentDataSource
import com.sucharu.sucharupro.data.datasource.DispatchExecutionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationActivityType
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItem
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationSummary
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryReconciliationRepository
import com.sucharu.sucharupro.domain.service.delivery.DeliveryReconciliationCalculator
import com.sucharu.sucharupro.domain.validation.DeliveryReconciliationAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.DeliveryReconciliationLifecycleValidator
import com.sucharu.sucharupro.domain.validation.DeliveryReconciliationOperation
import com.sucharu.sucharupro.domain.validation.DeliveryReconciliationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-grade, thread-safe implementation of DeliveryReconciliationRepository (Module 08 Step 09).
 */
class DeliveryReconciliationRepositoryImpl(
    private val reconciliationDataSource: DeliveryReconciliationDataSource,
    private val orderDataSource: DeliveryOrderDataSource,
    private val challanDataSource: DeliveryChallanDataSource? = null,
    private val dispatchDataSource: DispatchExecutionDataSource? = null,
    private val shipmentDataSource: DeliveryShipmentDataSource? = null,
    private val verificationDataSource: DeliveryItemVerificationDataSource? = null,
    private val returnDataSource: DeliveryReturnDataSource? = null,
    private val proofDataSource: DeliveryProofDataSource? = null,
    private val settlementDataSource: DeliveryPartialSettlementDataSource? = null
) : DeliveryReconciliationRepository {

    private val mutex = Mutex()

    override fun observeReconciliations(projectId: String): Flow<List<DeliveryReconciliation>> {
        return reconciliationDataSource.observeReconciliations(projectId)
    }

    override fun observeReconciliation(reconciliationId: String): Flow<DeliveryReconciliation?> {
        return reconciliationDataSource.observeReconciliation(reconciliationId)
    }

    override fun observeItems(reconciliationId: String): Flow<List<DeliveryReconciliationItem>> {
        return reconciliationDataSource.observeItems(reconciliationId)
    }

    override fun observeDiscrepancies(reconciliationId: String): Flow<List<DeliveryReconciliationDiscrepancy>> {
        return reconciliationDataSource.observeDiscrepancies(reconciliationId)
    }

    override fun observeActivityEvents(reconciliationId: String): Flow<List<DeliveryReconciliationActivityEvent>> {
        return reconciliationDataSource.observeActivityEvents(reconciliationId)
    }

    override fun observeSummary(projectId: String): Flow<DeliveryReconciliationSummary> {
        return combine(reconciliationDataSource.observeReconciliations(projectId)) { array ->
            val list = array[0]
            DeliveryReconciliationSummary(
                projectId = projectId,
                totalReconciliations = list.size,
                openCount = list.count { it.reconciliationStatus == DeliveryReconciliationStatus.OPEN },
                inProgressCount = list.count { it.reconciliationStatus == DeliveryReconciliationStatus.IN_PROGRESS },
                partiallyReconciledCount = list.count { it.reconciliationStatus == DeliveryReconciliationStatus.PARTIALLY_RECONCILED },
                requiresReviewCount = list.count { it.reconciliationStatus == DeliveryReconciliationStatus.REQUIRES_REVIEW },
                reconciledCount = list.count { it.reconciliationStatus == DeliveryReconciliationStatus.RECONCILED },
                disputedCount = list.count { it.reconciliationStatus == DeliveryReconciliationStatus.DISPUTED },
                resolvedCount = list.count { it.reconciliationStatus == DeliveryReconciliationStatus.RESOLVED },
                closedCount = list.count { it.reconciliationStatus == DeliveryReconciliationStatus.CLOSED },
                totalDiscrepancyCount = list.count { it.discrepancyQuantity > 0.0 }
            )
        }
    }

    override suspend fun getReconciliation(
        reconciliationId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReconciliation> = mutex.withLock {
        val reconciliation = reconciliationDataSource.getReconciliation(reconciliationId)
            ?: return DomainResult.Error(message = "Delivery Reconciliation '$reconciliationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReconciliationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReconciliationOperation.VIEW,
                targetProjectId = reconciliation.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        DomainResult.Success(reconciliation)
    }

    override suspend fun getReconciliationByDeliveryOrder(
        deliveryOrderId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReconciliation> = mutex.withLock {
        val reconciliation = reconciliationDataSource.getReconciliationByDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "No Reconciliation found for Delivery Order '$deliveryOrderId'.")

        if (callerRole != null) {
            val authCheck = DeliveryReconciliationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReconciliationOperation.VIEW,
                targetProjectId = reconciliation.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        DomainResult.Success(reconciliation)
    }

    override suspend fun getItems(
        reconciliationId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryReconciliationItem>> = mutex.withLock {
        val reconciliation = reconciliationDataSource.getReconciliation(reconciliationId)
            ?: return DomainResult.Error(message = "Delivery Reconciliation '$reconciliationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReconciliationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReconciliationOperation.VIEW,
                targetProjectId = reconciliation.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val items = reconciliationDataSource.getItems(reconciliationId)
        DomainResult.Success(items)
    }

    override suspend fun getDiscrepancies(
        reconciliationId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryReconciliationDiscrepancy>> = mutex.withLock {
        val reconciliation = reconciliationDataSource.getReconciliation(reconciliationId)
            ?: return DomainResult.Error(message = "Delivery Reconciliation '$reconciliationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReconciliationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReconciliationOperation.VIEW,
                targetProjectId = reconciliation.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val list = reconciliationDataSource.getDiscrepancies(reconciliationId)
        DomainResult.Success(list)
    }

    override suspend fun getActivityEvents(
        reconciliationId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryReconciliationActivityEvent>> = mutex.withLock {
        val reconciliation = reconciliationDataSource.getReconciliation(reconciliationId)
            ?: return DomainResult.Error(message = "Delivery Reconciliation '$reconciliationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReconciliationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReconciliationOperation.VIEW,
                targetProjectId = reconciliation.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val events = reconciliationDataSource.observeActivityEvents(reconciliationId).first()
        DomainResult.Success(events)
    }

    override suspend fun createReconciliation(
        deliveryOrderId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReconciliation> = mutex.withLock {
        val order = orderDataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Referenced Delivery Order '$deliveryOrderId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReconciliationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReconciliationOperation.CREATE,
                targetProjectId = order.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val existing = reconciliationDataSource.getReconciliationByDeliveryOrder(deliveryOrderId)
        if (existing != null && existing.reconciliationStatus != DeliveryReconciliationStatus.CLOSED) {
            return DomainResult.Error(
                message = "An active Delivery Reconciliation ('${existing.reconciliationId}') already exists for Delivery Order '$deliveryOrderId'."
            )
        }

        val orderLines = orderDataSource.getDeliveryOrderLines(deliveryOrderId)
        val challans = challanDataSource?.getChallansForDeliveryOrder(deliveryOrderId) ?: emptyList()
        val challanLines = challans.flatMap { challanDataSource?.getChallanLines(it.challanId) ?: emptyList() }
        val dispatches = challans.flatMap { dispatchDataSource?.getDispatchesForChallan(it.challanId) ?: emptyList() }
        val dispatchLines = dispatches.flatMap { dispatchDataSource?.getDispatchLines(it.dispatchExecutionId) ?: emptyList() }
        val shipments = dispatches.flatMap { dispatchDataSource?.let { _ -> shipmentDataSource?.getShipmentsForDispatch(it.dispatchExecutionId) } ?: emptyList() }
        val verifications = dispatches.flatMap { verificationDataSource?.getVerificationsForDispatch(it.dispatchExecutionId) ?: emptyList() }
        val verificationLines = verifications.flatMap { verificationDataSource?.getVerificationLines(it.verificationId) ?: emptyList() }
        val returns = returnDataSource?.getReturnsByDeliveryOrder(deliveryOrderId) ?: emptyList()
        val returnLines = returns.flatMap { returnDataSource?.getReturnLines(it.returnId) ?: emptyList() }
        val proofs = proofDataSource?.getProofsByDeliveryOrder(deliveryOrderId) ?: emptyList()

        val reconciliationId = UUID.randomUUID().toString()
        val calcResult = DeliveryReconciliationCalculator.calculateReconciliation(
            reconciliationId = reconciliationId,
            deliveryOrder = order,
            orderLines = orderLines,
            challanLines = challanLines,
            dispatchLines = dispatchLines,
            shipments = shipments,
            verificationLines = verificationLines,
            returnLines = returnLines,
            proofs = proofs,
            existingReconciliation = null,
            actorId = actorId
        )

        val validation = DeliveryReconciliationValidator.validateReconciliation(
            reconciliation = calcResult.aggregate,
            items = calcResult.items,
            targetProjectId = order.projectId
        )
        if (validation is DomainResult.Error) return validation

        reconciliationDataSource.insertReconciliation(
            reconciliation = calcResult.aggregate,
            items = calcResult.items,
            discrepancies = calcResult.discrepancies
        )

        val event = DeliveryReconciliationActivityEvent(
            eventId = UUID.randomUUID().toString(),
            reconciliationId = reconciliationId,
            projectId = order.projectId,
            activityType = DeliveryReconciliationActivityType.CREATED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = null,
            newStatus = calcResult.aggregate.reconciliationStatus,
            metadata = mapOf("deliveryOrderId" to deliveryOrderId),
            notes = "Delivery Reconciliation created with status '${calcResult.aggregate.reconciliationStatus.defaultLabel}'."
        )
        reconciliationDataSource.insertActivityEvent(event)

        if (calcResult.discrepancies.isNotEmpty()) {
            val discEvent = DeliveryReconciliationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                reconciliationId = reconciliationId,
                projectId = order.projectId,
                activityType = DeliveryReconciliationActivityType.DISCREPANCY_DETECTED,
                actorId = actorId,
                actorRole = callerRole,
                previousStatus = calcResult.aggregate.reconciliationStatus,
                newStatus = calcResult.aggregate.reconciliationStatus,
                notes = "${calcResult.discrepancies.size} discrepancy item(s) detected during initial calculation."
            )
            reconciliationDataSource.insertActivityEvent(discEvent)
        }

        DomainResult.Success(calcResult.aggregate)
    }

    override suspend fun refreshCalculation(
        reconciliationId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReconciliation> = mutex.withLock {
        val existing = reconciliationDataSource.getReconciliation(reconciliationId)
            ?: return DomainResult.Error(message = "Delivery Reconciliation '$reconciliationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReconciliationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReconciliationOperation.REFRESH,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        if (existing.reconciliationStatus == DeliveryReconciliationStatus.CLOSED) {
            return DomainResult.Error(message = "Cannot recalculate or refresh a CLOSED reconciliation.")
        }

        val deliveryOrderId = existing.deliveryOrderId
        val order = orderDataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Referenced Delivery Order '$deliveryOrderId' not found.")

        val orderLines = orderDataSource.getDeliveryOrderLines(deliveryOrderId)
        val challans = challanDataSource?.getChallansForDeliveryOrder(deliveryOrderId) ?: emptyList()
        val challanLines = challans.flatMap { challanDataSource?.getChallanLines(it.challanId) ?: emptyList() }
        val dispatches = challans.flatMap { dispatchDataSource?.getDispatchesForChallan(it.challanId) ?: emptyList() }
        val dispatchLines = dispatches.flatMap { dispatchDataSource?.getDispatchLines(it.dispatchExecutionId) ?: emptyList() }
        val shipments = dispatches.flatMap { dispatchDataSource?.let { _ -> shipmentDataSource?.getShipmentsForDispatch(it.dispatchExecutionId) } ?: emptyList() }
        val verifications = dispatches.flatMap { verificationDataSource?.getVerificationsForDispatch(it.dispatchExecutionId) ?: emptyList() }
        val verificationLines = verifications.flatMap { verificationDataSource?.getVerificationLines(it.verificationId) ?: emptyList() }
        val returns = returnDataSource?.getReturnsByDeliveryOrder(deliveryOrderId) ?: emptyList()
        val returnLines = returns.flatMap { returnDataSource?.getReturnLines(it.returnId) ?: emptyList() }
        val proofs = proofDataSource?.getProofsByDeliveryOrder(deliveryOrderId) ?: emptyList()

        val calcResult = DeliveryReconciliationCalculator.calculateReconciliation(
            reconciliationId = reconciliationId,
            deliveryOrder = order,
            orderLines = orderLines,
            challanLines = challanLines,
            dispatchLines = dispatchLines,
            shipments = shipments,
            verificationLines = verificationLines,
            returnLines = returnLines,
            proofs = proofs,
            existingReconciliation = existing,
            actorId = actorId
        )

        val validation = DeliveryReconciliationValidator.validateReconciliation(
            reconciliation = calcResult.aggregate,
            items = calcResult.items,
            targetProjectId = existing.projectId
        )
        if (validation is DomainResult.Error) return validation

        reconciliationDataSource.updateReconciliation(
            reconciliation = calcResult.aggregate,
            items = calcResult.items,
            discrepancies = calcResult.discrepancies
        )

        val event = DeliveryReconciliationActivityEvent(
            eventId = UUID.randomUUID().toString(),
            reconciliationId = reconciliationId,
            projectId = existing.projectId,
            activityType = DeliveryReconciliationActivityType.CALCULATION_REFRESHED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.reconciliationStatus,
            newStatus = calcResult.aggregate.reconciliationStatus,
            notes = "Quantities refreshed across orders, dispatches, shipments, returns and proofs."
        )
        reconciliationDataSource.insertActivityEvent(event)

        DomainResult.Success(calcResult.aggregate)
    }

    override suspend fun startReconciliation(
        reconciliationId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReconciliation> = mutex.withLock {
        val existing = reconciliationDataSource.getReconciliation(reconciliationId)
            ?: return DomainResult.Error(message = "Delivery Reconciliation '$reconciliationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReconciliationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReconciliationOperation.START_RECONCILIATION,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lifecycleCheck = DeliveryReconciliationLifecycleValidator.validateTransition(
            currentStatus = existing.reconciliationStatus,
            targetStatus = DeliveryReconciliationStatus.IN_PROGRESS
        )
        if (lifecycleCheck is DomainResult.Error) return lifecycleCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            reconciliationStatus = DeliveryReconciliationStatus.IN_PROGRESS,
            updatedBy = actorId,
            updatedAt = now
        )
        reconciliationDataSource.updateReconciliation(updated)

        val event = DeliveryReconciliationActivityEvent(
            eventId = UUID.randomUUID().toString(),
            reconciliationId = reconciliationId,
            projectId = updated.projectId,
            activityType = DeliveryReconciliationActivityType.RECONCILIATION_STARTED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.reconciliationStatus,
            newStatus = DeliveryReconciliationStatus.IN_PROGRESS,
            notes = "Reconciliation process officially started."
        )
        reconciliationDataSource.insertActivityEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun resolveDiscrepancy(
        reconciliationId: String,
        discrepancyId: String,
        resolutionNotes: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReconciliationDiscrepancy> = mutex.withLock {
        if (resolutionNotes.isBlank()) {
            return DomainResult.Error(message = "Resolution notes cannot be blank.")
        }

        val existing = reconciliationDataSource.getReconciliation(reconciliationId)
            ?: return DomainResult.Error(message = "Delivery Reconciliation '$reconciliationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReconciliationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReconciliationOperation.RESOLVE_DISCREPANCY,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        if (existing.reconciliationStatus == DeliveryReconciliationStatus.CLOSED) {
            return DomainResult.Error(message = "Cannot modify discrepancies of a CLOSED reconciliation.")
        }

        val discrepancy = reconciliationDataSource.getDiscrepancy(discrepancyId)
            ?: return DomainResult.Error(message = "Discrepancy '$discrepancyId' not found.")

        val now = System.currentTimeMillis()
        val resolvedDiscrepancy = discrepancy.copy(
            isResolved = true,
            resolutionNotes = resolutionNotes,
            resolvedBy = actorId,
            resolvedAt = now
        )
        reconciliationDataSource.updateDiscrepancy(resolvedDiscrepancy)

        // Check if all discrepancies are resolved
        val allDiscrepancies = reconciliationDataSource.getDiscrepancies(reconciliationId)
        val allResolved = allDiscrepancies.all { if (it.discrepancyId == discrepancyId) true else it.isResolved }

        val newReconciliationStatus = if (allResolved) {
            DeliveryReconciliationStatus.RESOLVED
        } else {
            existing.reconciliationStatus
        }

        val updatedReconciliation = existing.copy(
            reconciliationStatus = newReconciliationStatus,
            resolutionNotes = resolutionNotes,
            resolvedBy = if (allResolved) actorId else existing.resolvedBy,
            resolvedAt = if (allResolved) now else existing.resolvedAt,
            updatedBy = actorId,
            updatedAt = now
        )
        reconciliationDataSource.updateReconciliation(updatedReconciliation)

        val event = DeliveryReconciliationActivityEvent(
            eventId = UUID.randomUUID().toString(),
            reconciliationId = reconciliationId,
            projectId = existing.projectId,
            activityType = if (allResolved) DeliveryReconciliationActivityType.RESOLVED else DeliveryReconciliationActivityType.RESOLUTION_STARTED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.reconciliationStatus,
            newStatus = newReconciliationStatus,
            notes = "Discrepancy '${discrepancy.discrepancyType.defaultLabel}' resolved: $resolutionNotes"
        )
        reconciliationDataSource.insertActivityEvent(event)

        DomainResult.Success(resolvedDiscrepancy)
    }

    override suspend fun markReconciled(
        reconciliationId: String,
        actorId: String,
        notes: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReconciliation> = mutex.withLock {
        val existing = reconciliationDataSource.getReconciliation(reconciliationId)
            ?: return DomainResult.Error(message = "Delivery Reconciliation '$reconciliationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReconciliationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReconciliationOperation.MARK_RECONCILED,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val discrepancies = reconciliationDataSource.getDiscrepancies(reconciliationId)
        val hasUnresolved = discrepancies.any { !it.isResolved }
        if (hasUnresolved) {
            return DomainResult.Error(
                message = "Cannot mark reconciled: Unresolved discrepancy items exist."
            )
        }

        val lifecycleCheck = DeliveryReconciliationLifecycleValidator.validateTransition(
            currentStatus = existing.reconciliationStatus,
            targetStatus = DeliveryReconciliationStatus.RECONCILED
        )
        if (lifecycleCheck is DomainResult.Error) return lifecycleCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            reconciliationStatus = DeliveryReconciliationStatus.RECONCILED,
            resolutionNotes = notes ?: existing.resolutionNotes,
            updatedBy = actorId,
            updatedAt = now
        )
        reconciliationDataSource.updateReconciliation(updated)

        val event = DeliveryReconciliationActivityEvent(
            eventId = UUID.randomUUID().toString(),
            reconciliationId = reconciliationId,
            projectId = existing.projectId,
            activityType = DeliveryReconciliationActivityType.RECONCILED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.reconciliationStatus,
            newStatus = DeliveryReconciliationStatus.RECONCILED,
            notes = notes ?: "Delivery Reconciliation marked as RECONCILED."
        )
        reconciliationDataSource.insertActivityEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun closeReconciliation(
        reconciliationId: String,
        actorId: String,
        notes: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReconciliation> = mutex.withLock {
        val existing = reconciliationDataSource.getReconciliation(reconciliationId)
            ?: return DomainResult.Error(message = "Delivery Reconciliation '$reconciliationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReconciliationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReconciliationOperation.CLOSE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId,
                actorId = actorId,
                creatorId = existing.createdBy
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lifecycleCheck = DeliveryReconciliationLifecycleValidator.validateTransition(
            currentStatus = existing.reconciliationStatus,
            targetStatus = DeliveryReconciliationStatus.CLOSED
        )
        if (lifecycleCheck is DomainResult.Error) return lifecycleCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            reconciliationStatus = DeliveryReconciliationStatus.CLOSED,
            closedBy = actorId,
            closedAt = now,
            resolutionNotes = notes ?: existing.resolutionNotes,
            updatedBy = actorId,
            updatedAt = now
        )
        reconciliationDataSource.updateReconciliation(updated)

        val event = DeliveryReconciliationActivityEvent(
            eventId = UUID.randomUUID().toString(),
            reconciliationId = reconciliationId,
            projectId = existing.projectId,
            activityType = DeliveryReconciliationActivityType.CLOSED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.reconciliationStatus,
            newStatus = DeliveryReconciliationStatus.CLOSED,
            notes = notes ?: "Delivery Reconciliation closed by '$actorId'."
        )
        reconciliationDataSource.insertActivityEvent(event)

        DomainResult.Success(updated)
    }
}
