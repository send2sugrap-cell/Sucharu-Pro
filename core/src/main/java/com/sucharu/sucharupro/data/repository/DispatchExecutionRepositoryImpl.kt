package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.DispatchExecutionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanActivityType
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionActivityType
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryIssueType
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutLine
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryMovementType
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DispatchExecutionRepository
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import com.sucharu.sucharupro.domain.repository.InventoryTraceabilityRepository
import com.sucharu.sucharupro.domain.validation.DispatchExecutionAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.DispatchExecutionLifecycleValidator
import com.sucharu.sucharupro.domain.validation.DispatchExecutionOperation
import com.sucharu.sucharupro.domain.validation.DispatchExecutionValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Production implementation of [DispatchExecutionRepository] (Module 08 Step 03).
 *
 * Integrates operational dispatch with Module 07 Step 04 [InventoryStockOutRepository]
 * and Module 07 Step 07 [InventoryTraceabilityRepository].
 */
class DispatchExecutionRepositoryImpl(
    private val dispatchDataSource: DispatchExecutionDataSource,
    private val challanDataSource: DeliveryChallanDataSource,
    private val stockOutRepository: InventoryStockOutRepository,
    private val traceabilityRepository: InventoryTraceabilityRepository? = null
) : DispatchExecutionRepository {

    private val mutex = Mutex()

    // ──────────────────────────────────────────────────────────────
    // Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeDispatches(projectId: String): Flow<List<DispatchExecution>> {
        return dispatchDataSource.observeDispatches(projectId)
    }

    override fun observeDispatchesForChallan(deliveryChallanId: String): Flow<List<DispatchExecution>> {
        return dispatchDataSource.observeDispatchesForChallan(deliveryChallanId)
    }

    override fun observeDispatch(dispatchExecutionId: String): Flow<DispatchExecution?> {
        return dispatchDataSource.observeDispatch(dispatchExecutionId)
    }

    override suspend fun getDispatch(
        dispatchExecutionId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DispatchExecution> {
        val dispatch = dispatchDataSource.getDispatch(dispatchExecutionId)
            ?: return DomainResult.Error(message = "Dispatch execution '$dispatchExecutionId' not found.")

        if (callerRole != null) {
            val authCheck = DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DispatchExecutionOperation.VIEW,
                targetProjectId = dispatch.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        return DomainResult.Success(dispatch)
    }

    override suspend fun getDispatchesForChallan(
        deliveryChallanId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DispatchExecution>> {
        val challan = challanDataSource.getChallan(deliveryChallanId)
            ?: return DomainResult.Error(message = "Delivery challan '$deliveryChallanId' not found.")

        if (callerRole != null) {
            val authCheck = DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DispatchExecutionOperation.VIEW,
                targetProjectId = challan.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val dispatches = dispatchDataSource.getDispatchesForChallan(deliveryChallanId)
        return DomainResult.Success(dispatches)
    }

    override fun observeDispatchLines(dispatchExecutionId: String): Flow<List<DispatchExecutionLine>> {
        return dispatchDataSource.observeDispatchLines(dispatchExecutionId)
    }

    override suspend fun getDispatchLines(
        dispatchExecutionId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DispatchExecutionLine>> {
        val dispatch = dispatchDataSource.getDispatch(dispatchExecutionId)
            ?: return DomainResult.Error(message = "Dispatch execution '$dispatchExecutionId' not found.")

        if (callerRole != null) {
            val authCheck = DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DispatchExecutionOperation.VIEW,
                targetProjectId = dispatch.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lines = dispatchDataSource.getDispatchLines(dispatchExecutionId)
        return DomainResult.Success(lines)
    }

    override fun observeActivityEvents(dispatchExecutionId: String): Flow<List<DispatchExecutionActivityEvent>> {
        return dispatchDataSource.observeActivityEvents(dispatchExecutionId)
    }

    override suspend fun getActivityEvents(
        dispatchExecutionId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DispatchExecutionActivityEvent>> {
        val dispatch = dispatchDataSource.getDispatch(dispatchExecutionId)
            ?: return DomainResult.Error(message = "Dispatch execution '$dispatchExecutionId' not found.")

        if (callerRole != null) {
            val authCheck = DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DispatchExecutionOperation.VIEW,
                targetProjectId = dispatch.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val events = dispatchDataSource.getActivityEvents(dispatchExecutionId)
        return DomainResult.Success(events)
    }

    // ──────────────────────────────────────────────────────────────
    // Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun createDispatch(
        dispatch: DispatchExecution,
        lines: List<DispatchExecutionLine>,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DispatchExecution> = mutex.withLock {
        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DispatchExecutionOperation.CREATE,
                targetProjectId = dispatch.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Structural validation
        val validationResult = DispatchExecutionValidator.validateDispatchExecution(dispatch, lines)
        if (validationResult is DomainResult.Error) return validationResult

        // 3. Unique dispatch number check
        val existingDispatch = dispatchDataSource.getDispatchByNo(dispatch.projectId, dispatch.dispatchNo)
        if (existingDispatch != null) {
            return DomainResult.Error(
                message = "Dispatch number '${dispatch.dispatchNo}' already exists in project '${dispatch.projectId}'."
            )
        }

        // 4. Challan Eligibility Check
        val challan = challanDataSource.getChallan(dispatch.deliveryChallanId)
            ?: return DomainResult.Error(message = "Referenced Delivery Challan '${dispatch.deliveryChallanId}' not found.")

        val eligibilityCheck = DispatchExecutionValidator.validateChallanEligibility(challan, dispatch.projectId)
        if (eligibilityCheck is DomainResult.Error) return eligibilityCheck

        // 5. Lines vs Challan Validation
        val challanLines = challanDataSource.getChallanLines(dispatch.deliveryChallanId)
        val linesCheck = DispatchExecutionValidator.validateLinesAgainstChallan(challanLines, lines)
        if (linesCheck is DomainResult.Error) return linesCheck

        // 6. Persistence
        dispatchDataSource.insertDispatch(dispatch, lines)

        // 7. Audit
        val activity = DispatchExecutionActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = dispatch.projectId,
            dispatchExecutionId = dispatch.dispatchExecutionId,
            activityType = DispatchExecutionActivityType.CREATED,
            performedBy = dispatch.createdBy,
            performedAt = dispatch.createdAt,
            details = "Dispatch execution created for Challan '${challan.challanNo}' with ${lines.size} line(s).",
            newStatus = dispatch.status.name
        )
        dispatchDataSource.insertActivityEvent(activity)

        DomainResult.Success(dispatch)
    }

    override suspend fun updateDraftDispatch(
        dispatchExecutionId: String,
        dispatchType: DispatchExecutionType,
        sourceWarehouseId: String,
        sourceLocationId: String,
        dispatchDate: Long,
        notes: String?,
        lines: List<DispatchExecutionLine>,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DispatchExecution> = mutex.withLock {
        val existing = dispatchDataSource.getDispatch(dispatchExecutionId)
            ?: return DomainResult.Error(message = "Dispatch execution '$dispatchExecutionId' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DispatchExecutionOperation.EDIT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        if (existing.status != DispatchExecutionStatus.DRAFT) {
            return DomainResult.Error(message = "Only DRAFT dispatch executions can be updated. Current status: '${existing.status}'.")
        }

        val updated = existing.copy(
            dispatchType = dispatchType,
            sourceWarehouseId = sourceWarehouseId,
            sourceLocationId = sourceLocationId,
            dispatchDate = dispatchDate,
            notes = notes,
            updatedAt = System.currentTimeMillis()
        )

        // Validation
        val validationResult = DispatchExecutionValidator.validateDispatchExecution(updated, lines)
        if (validationResult is DomainResult.Error) return validationResult

        val immutabilityCheck = DispatchExecutionValidator.validateImmutableIdentity(existing, updated)
        if (immutabilityCheck is DomainResult.Error) return immutabilityCheck

        val challanLines = challanDataSource.getChallanLines(existing.deliveryChallanId)
        val linesCheck = DispatchExecutionValidator.validateLinesAgainstChallan(challanLines, lines)
        if (linesCheck is DomainResult.Error) return linesCheck

        dispatchDataSource.updateDispatchWithLines(updated, lines)

        val activity = DispatchExecutionActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            dispatchExecutionId = existing.dispatchExecutionId,
            activityType = DispatchExecutionActivityType.UPDATED,
            performedBy = actorId,
            performedAt = updated.updatedAt,
            details = "Draft dispatch execution updated with ${lines.size} line(s)."
        )
        dispatchDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun submitDispatch(
        dispatchExecutionId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DispatchExecution> = mutex.withLock {
        val existing = dispatchDataSource.getDispatch(dispatchExecutionId)
            ?: return DomainResult.Error(message = "Dispatch execution '$dispatchExecutionId' not found.")

        if (callerRole != null) {
            val authCheck = DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DispatchExecutionOperation.SUBMIT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DispatchExecutionLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DispatchExecutionStatus.PENDING
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(status = DispatchExecutionStatus.PENDING, updatedAt = now)
        dispatchDataSource.updateDispatch(updated)

        val activity = DispatchExecutionActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            dispatchExecutionId = existing.dispatchExecutionId,
            activityType = DispatchExecutionActivityType.SUBMITTED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DispatchExecutionStatus.PENDING.name,
            details = "Dispatch submitted for manager approval."
        )
        dispatchDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun approveDispatch(
        dispatchExecutionId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DispatchExecution> = mutex.withLock {
        val existing = dispatchDataSource.getDispatch(dispatchExecutionId)
            ?: return DomainResult.Error(message = "Dispatch execution '$dispatchExecutionId' not found.")

        if (callerRole != null) {
            val authCheck = DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DispatchExecutionOperation.APPROVE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DispatchExecutionLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DispatchExecutionStatus.APPROVED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(status = DispatchExecutionStatus.APPROVED, updatedAt = now)
        dispatchDataSource.updateDispatch(updated)

        val activity = DispatchExecutionActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            dispatchExecutionId = existing.dispatchExecutionId,
            activityType = DispatchExecutionActivityType.APPROVED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DispatchExecutionStatus.APPROVED.name,
            details = "Dispatch execution approved."
        )
        dispatchDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun markReadyForExecution(
        dispatchExecutionId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DispatchExecution> = mutex.withLock {
        val existing = dispatchDataSource.getDispatch(dispatchExecutionId)
            ?: return DomainResult.Error(message = "Dispatch execution '$dispatchExecutionId' not found.")

        if (callerRole != null) {
            val authCheck = DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DispatchExecutionOperation.PREPARE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DispatchExecutionLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DispatchExecutionStatus.READY_FOR_EXECUTION
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(status = DispatchExecutionStatus.READY_FOR_EXECUTION, updatedAt = now)
        dispatchDataSource.updateDispatch(updated)

        val activity = DispatchExecutionActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            dispatchExecutionId = existing.dispatchExecutionId,
            activityType = DispatchExecutionActivityType.READY_FOR_EXECUTION,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DispatchExecutionStatus.READY_FOR_EXECUTION.name,
            details = "Dispatch prepared and marked ready for warehouse execution."
        )
        dispatchDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun executeDispatch(
        dispatchExecutionId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DispatchExecution> = mutex.withLock {
        val existing = dispatchDataSource.getDispatch(dispatchExecutionId)
            ?: return DomainResult.Error(message = "Dispatch execution '$dispatchExecutionId' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DispatchExecutionOperation.EXECUTE_DISPATCH,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Lifecycle
        val transitionCheck = DispatchExecutionLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DispatchExecutionStatus.DISPATCHED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        // 3. Re-verify Challan
        val challan = challanDataSource.getChallan(existing.deliveryChallanId)
            ?: return DomainResult.Error(message = "Referenced Delivery Challan '${existing.deliveryChallanId}' not found.")

        val eligibilityCheck = DispatchExecutionValidator.validateChallanEligibility(challan, existing.projectId)
        if (eligibilityCheck is DomainResult.Error) return eligibilityCheck

        // 4. Retrieve lines
        val lines = dispatchDataSource.getDispatchLines(dispatchExecutionId)
        if (lines.isEmpty()) {
            return DomainResult.Error(message = "No lines found to dispatch.")
        }

        // 5. Stock Availability Validation
        for (line in lines) {
            val available = stockOutRepository.getAvailableQuantity(
                projectId = existing.projectId,
                warehouseId = existing.sourceWarehouseId,
                locationId = line.sourceLocationId,
                productId = line.productId
            )
            val required = line.dispatchQuantity.toInt()
            if (available < required) {
                // Record failed activity event
                val failEvent = DispatchExecutionActivityEvent(
                    activityId = UUID.randomUUID().toString(),
                    projectId = existing.projectId,
                    dispatchExecutionId = existing.dispatchExecutionId,
                    activityType = DispatchExecutionActivityType.FAILED,
                    performedBy = actorId,
                    performedAt = System.currentTimeMillis(),
                    details = "Insufficient stock for product '${line.productId}' at location '${line.sourceLocationId}'. Available: $available, Required: $required."
                )
                dispatchDataSource.insertActivityEvent(failEvent)

                return DomainResult.Error(
                    message = "Insufficient stock for product '${line.productId}' at location '${line.sourceLocationId}'. Available: $available, Required: $required."
                )
            }
        }

        // 6. Execute Module 07 Stock Out operation atomically
        val now = System.currentTimeMillis()
        val nowIso = now.toString()
        val stockOutId = UUID.randomUUID().toString()
        val stockOutReference = "SOUT-DISP-${existing.dispatchNo}"

        val totalQty = lines.sumOf { it.dispatchQuantity.toInt() }

        val stockOut = InventoryStockOut(
            stockOutId = stockOutId,
            projectId = existing.projectId,
            stockOutReference = stockOutReference,
            warehouseId = existing.sourceWarehouseId,
            stockOutDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now)),
            status = com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutStatus.DRAFT,
            issueType = InventoryIssueType.DELIVERY,
            sourceReference = existing.dispatchExecutionId,
            expectedTotalQuantity = totalQty,
            issuedTotalQuantity = 0,
            notes = "Dispatched via ${existing.dispatchNo} for Challan ${challan.challanNo}",
            createdBy = actorId,
            createdAt = nowIso,
            updatedAt = nowIso
        )

        val internalRole = if (callerRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) callerRole else UserRole.MANAGER

        val createStockOutRes = stockOutRepository.createStockOut(stockOut, internalRole)
        if (createStockOutRes is DomainResult.Error) return createStockOutRes

        for (line in lines) {
            val stockOutLine = InventoryStockOutLine(
                stockOutLineId = UUID.randomUUID().toString(),
                stockOutId = stockOutId,
                projectId = existing.projectId,
                inventoryProductId = line.productId,
                warehouseId = existing.sourceWarehouseId,
                locationId = line.sourceLocationId,
                expectedQuantity = line.dispatchQuantity.toInt(),
                issuedQuantity = line.dispatchQuantity.toInt(),
                unit = InventoryUnit.PCS,
                notes = "Line for DO Line ${line.deliveryOrderLineId}",
                createdAt = nowIso,
                updatedAt = nowIso
            )
            val addLineRes = stockOutRepository.addStockOutLine(stockOutLine, internalRole)
            if (addLineRes is DomainResult.Error) return addLineRes
        }

        val submitStockOutRes = stockOutRepository.submitStockOut(stockOutId, actorId, nowIso, internalRole)
        if (submitStockOutRes is DomainResult.Error) return submitStockOutRes

        val approveStockOutRes = stockOutRepository.approveStockOut(stockOutId, actorId, nowIso, internalRole)
        if (approveStockOutRes is DomainResult.Error) return approveStockOutRes

        val completeStockOutRes = stockOutRepository.completeStockOut(stockOutId, actorId, nowIso, internalRole)
        if (completeStockOutRes is DomainResult.Error) return completeStockOutRes

        // 7. Batch/Lot Traceability Integration
        if (traceabilityRepository != null) {
            for (line in lines) {
                if (line.batchId != null || line.lotId != null) {
                    val traceRecord = InventoryTraceabilityRecord(
                        traceRecordId = UUID.randomUUID().toString(),
                        batchId = line.batchId,
                        lotId = line.lotId,
                        projectId = existing.projectId,
                        productId = line.productId,
                        locationId = line.sourceLocationId,
                        movementRecordId = stockOutId,
                        movementType = InventoryMovementType.STOCK_OUT,
                        quantity = line.dispatchQuantity,
                        unit = InventoryUnit.PCS,
                        actorId = actorId,
                        timestamp = nowIso
                    )
                    traceabilityRepository.linkMovementToTraceability(traceRecord, callerRole)
                }
            }
        }

        // 8. Update Delivery Challan state to DISPATCHED
        val updatedChallan = challan.copy(
            status = DeliveryChallanStatus.DISPATCHED,
            updatedAt = now
        )
        challanDataSource.updateChallan(updatedChallan)

        val challanAudit = DeliveryChallanActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = challan.projectId,
            challanId = challan.challanId,
            activityType = DeliveryChallanActivityType.READY_FOR_DISPATCH,
            performedBy = actorId,
            performedAt = now,
            previousStatus = challan.status.name,
            newStatus = DeliveryChallanStatus.DISPATCHED.name,
            details = "Challan physically dispatched under execution '${existing.dispatchNo}'."
        )
        challanDataSource.insertActivityEvent(challanAudit)

        // 9. Update DispatchExecution to DISPATCHED
        val updatedDispatch = existing.copy(
            status = DispatchExecutionStatus.DISPATCHED,
            stockOutId = stockOutId,
            dispatchedAt = now,
            dispatchedBy = actorId,
            updatedAt = now
        )
        dispatchDataSource.updateDispatch(updatedDispatch)

        // 10. Audit events
        val stockOutEvent = DispatchExecutionActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            dispatchExecutionId = existing.dispatchExecutionId,
            activityType = DispatchExecutionActivityType.STOCK_OUT_CREATED,
            performedBy = actorId,
            performedAt = now,
            referenceId = stockOutId,
            details = "Stock Out '$stockOutReference' generated and completed."
        )
        dispatchDataSource.insertActivityEvent(stockOutEvent)

        val dispatchEvent = DispatchExecutionActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            dispatchExecutionId = existing.dispatchExecutionId,
            activityType = DispatchExecutionActivityType.DISPATCHED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DispatchExecutionStatus.DISPATCHED.name,
            details = "Dispatch execution completed successfully."
        )
        dispatchDataSource.insertActivityEvent(dispatchEvent)

        DomainResult.Success(updatedDispatch)
    }

    override suspend fun cancelDispatch(
        dispatchExecutionId: String,
        actorId: String,
        reason: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DispatchExecution> = mutex.withLock {
        val existing = dispatchDataSource.getDispatch(dispatchExecutionId)
            ?: return DomainResult.Error(message = "Dispatch execution '$dispatchExecutionId' not found.")

        if (callerRole != null) {
            val authCheck = DispatchExecutionAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DispatchExecutionOperation.CANCEL,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DispatchExecutionLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DispatchExecutionStatus.CANCELLED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DispatchExecutionStatus.CANCELLED,
            cancelledAt = now,
            cancelledBy = actorId,
            updatedAt = now
        )
        dispatchDataSource.updateDispatch(updated)

        val activity = DispatchExecutionActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            dispatchExecutionId = existing.dispatchExecutionId,
            activityType = DispatchExecutionActivityType.CANCELLED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DispatchExecutionStatus.CANCELLED.name,
            details = reason ?: "Dispatch execution cancelled."
        )
        dispatchDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }
}
