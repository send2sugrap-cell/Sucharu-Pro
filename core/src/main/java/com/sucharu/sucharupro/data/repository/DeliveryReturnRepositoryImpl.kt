package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryPartialSettlementDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryReturnDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryShipmentDataSource
import com.sucharu.sucharupro.data.datasource.InventoryReceivingDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnActivityType
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnDisposition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLineCondition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnShipment
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnSummary
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryReturnRepository
import com.sucharu.sucharupro.domain.service.delivery.DeliveryReturnEligibilityCalculator
import com.sucharu.sucharupro.domain.validation.DeliveryReturnAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.DeliveryReturnDispositionValidator
import com.sucharu.sucharupro.domain.validation.DeliveryReturnLifecycleValidator
import com.sucharu.sucharupro.domain.validation.DeliveryReturnLineValidator
import com.sucharu.sucharupro.domain.validation.DeliveryReturnOperation
import com.sucharu.sucharupro.domain.validation.DeliveryReturnValidator
import com.sucharu.sucharupro.domain.validation.DeliveryReverseShipmentValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-grade implementation of DeliveryReturnRepository (Module 08 Step 07).
 */
class DeliveryReturnRepositoryImpl(
    private val returnDataSource: DeliveryReturnDataSource,
    private val doDataSource: DeliveryOrderDataSource,
    private val receivingDataSource: InventoryReceivingDataSource? = null,
    private val settlementDataSource: DeliveryPartialSettlementDataSource? = null,
    private val shipmentDataSource: DeliveryShipmentDataSource? = null,
    private val verificationDataSource: DeliveryItemVerificationDataSource? = null
) : DeliveryReturnRepository {

    private val mutex = Mutex()

    override fun observeReturns(projectId: String): Flow<List<DeliveryReturn>> {
        return returnDataSource.observeReturns(projectId)
    }

    override fun observeReturn(returnId: String): Flow<DeliveryReturn?> {
        return returnDataSource.observeReturn(returnId)
    }

    override fun observeReturnSummary(returnId: String): Flow<DeliveryReturnSummary?> {
        val retFlow = returnDataSource.observeReturn(returnId)
        return retFlow.combine(flowOf(Unit)) { ret, _ ->
            if (ret == null) null
            else {
                val lines = returnDataSource.getReturnLines(ret.returnId)
                DeliveryReturnEligibilityCalculator.buildSummary(ret, lines)
            }
        }
    }

    override suspend fun getReturn(
        returnId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        val ret = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.VIEW,
                targetProjectId = ret.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        DomainResult.Success(ret)
    }

    override suspend fun getReturnLines(
        returnId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryReturnLine>> = mutex.withLock {
        val ret = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.VIEW,
                targetProjectId = ret.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lines = returnDataSource.getReturnLines(returnId)
        DomainResult.Success(lines)
    }

    override suspend fun createReturn(
        ret: DeliveryReturn,
        lines: List<DeliveryReturnLine>,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.CREATE,
                targetProjectId = ret.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // Validate Delivery Order reference
        val doOrder = doDataSource.getDeliveryOrder(ret.deliveryOrderId)
            ?: return DomainResult.Error(message = "Referenced Delivery Order '${ret.deliveryOrderId}' not found.")

        if (doOrder.projectId != ret.projectId) {
            return DomainResult.Error(
                message = "Project mismatch: Delivery Order belongs to '${doOrder.projectId}', but return target is '${ret.projectId}'."
            )
        }

        // Validate Shipment reference if present
        if (ret.shipmentId != null && shipmentDataSource != null) {
            val shipment = shipmentDataSource.getShipment(ret.shipmentId)
            if (shipment == null) {
                return DomainResult.Error(message = "Referenced Shipment '${ret.shipmentId}' not found.")
            }
            if (shipment.projectId != ret.projectId) {
                return DomainResult.Error(message = "Project mismatch: Shipment belongs to '${shipment.projectId}'.")
            }
        }

        // Validate unique Return Number per project
        val existingWithNo = returnDataSource.getReturnByNumber(ret.projectId, ret.returnNo)
        if (existingWithNo != null) {
            return DomainResult.Error(message = "Delivery Return with number '${ret.returnNo}' already exists in project '${ret.projectId}'.")
        }

        // Validate eligibility against existing returns & delivered quantity
        val doLines = doDataSource.getDeliveryOrderLines(ret.deliveryOrderId)
        val existingReturns = returnDataSource.getReturnsByDeliveryOrder(ret.deliveryOrderId)
        val existingReturnsWithLines = existingReturns.map { it to returnDataSource.getReturnLines(it.returnId) }

        for (line in lines) {
            val doLine = doLines.find { it.lineId == line.deliveryOrderLineId }
                ?: return DomainResult.Error(message = "Delivery Order Line '${line.deliveryOrderLineId}' not found in order '${ret.deliveryOrderId}'.")

            if (doLine.productId != line.productId) {
                return DomainResult.Error(message = "Product mismatch on return line '${line.returnLineId}'. Expected '${doLine.productId}', got '${line.productId}'.")
            }

            val maxDelivered = doLine.requestedQuantity
            val maxEligible = DeliveryReturnEligibilityCalculator.calculateEligibleReturnQuantity(
                deliveryOrderLineId = line.deliveryOrderLineId,
                deliveredQuantity = maxDelivered,
                existingReturns = existingReturnsWithLines
            )

            val lineValidation = DeliveryReturnLineValidator.validateLine(line, maxEligible)
            if (lineValidation is DomainResult.Error) return lineValidation
        }

        val aggregateValidation = DeliveryReturnValidator.validateReturn(ret, lines)
        if (aggregateValidation is DomainResult.Error) return aggregateValidation

        val assignedLines = lines.map { it.copy(returnId = ret.returnId, projectId = ret.projectId) }
        returnDataSource.insertReturn(ret, assignedLines)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = ret.projectId,
            returnId = ret.returnId,
            activityType = DeliveryReturnActivityType.CREATED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = null,
            newStatus = ret.status,
            notes = "Delivery Return created with ${lines.size} line(s)."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(ret)
    }

    override suspend fun updateDraftReturn(
        ret: DeliveryReturn,
        lines: List<DeliveryReturnLine>,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        val existing = returnDataSource.getReturn(ret.returnId)
            ?: return DomainResult.Error(message = "Delivery Return '${ret.returnId}' not found.")

        if (existing.status != DeliveryReturnStatus.DRAFT) {
            return DomainResult.Error(message = "Cannot update return in non-draft status '${existing.status.defaultLabel}'.")
        }

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.UPDATE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val immCheck = DeliveryReturnValidator.validateImmutableIdentity(existing, ret)
        if (immCheck is DomainResult.Error) return immCheck

        val updated = ret.copy(updatedAt = System.currentTimeMillis())
        val assignedLines = lines.map { it.copy(returnId = ret.returnId, projectId = ret.projectId, updatedAt = System.currentTimeMillis()) }

        returnDataSource.insertReturn(updated, assignedLines)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = updated.projectId,
            returnId = updated.returnId,
            activityType = DeliveryReturnActivityType.UPDATED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = updated.status,
            notes = "Draft return updated."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun submitReturn(
        returnId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.SUBMIT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transCheck = DeliveryReturnLifecycleValidator.validateTransition(existing.status, DeliveryReturnStatus.PENDING)
        if (transCheck is DomainResult.Error) return transCheck

        val lines = returnDataSource.getReturnLines(returnId)
        if (lines.isEmpty()) {
            return DomainResult.Error(message = "Cannot submit return without any item lines.")
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryReturnStatus.PENDING,
            submittedAt = now,
            updatedAt = now
        )
        returnDataSource.updateReturn(updated)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = updated.projectId,
            returnId = updated.returnId,
            activityType = DeliveryReturnActivityType.SUBMITTED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = DeliveryReturnStatus.PENDING,
            notes = "Return submitted for authorization."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun approveReturn(
        returnId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.APPROVE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transCheck = DeliveryReturnLifecycleValidator.validateTransition(existing.status, DeliveryReturnStatus.APPROVED)
        if (transCheck is DomainResult.Error) return transCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryReturnStatus.APPROVED,
            approvedBy = actorId,
            approvedAt = now,
            updatedAt = now
        )
        returnDataSource.updateReturn(updated)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = updated.projectId,
            returnId = updated.returnId,
            activityType = DeliveryReturnActivityType.APPROVED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = DeliveryReturnStatus.APPROVED,
            notes = "Return approved by '$actorId'."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun rejectReturn(
        returnId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.REJECT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transCheck = DeliveryReturnLifecycleValidator.validateTransition(existing.status, DeliveryReturnStatus.REJECTED)
        if (transCheck is DomainResult.Error) return transCheck

        if (rejectionReason.isBlank()) {
            return DomainResult.Error(message = "Rejection reason is required.")
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryReturnStatus.REJECTED,
            rejectionReason = rejectionReason,
            updatedAt = now
        )
        returnDataSource.updateReturn(updated)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = updated.projectId,
            returnId = updated.returnId,
            activityType = DeliveryReturnActivityType.REJECTED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = DeliveryReturnStatus.REJECTED,
            notes = "Return rejected: $rejectionReason"
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun startReceiving(
        returnId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.RECEIVE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transCheck = DeliveryReturnLifecycleValidator.validateTransition(existing.status, DeliveryReturnStatus.RECEIVING)
        if (transCheck is DomainResult.Error) return transCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryReturnStatus.RECEIVING,
            updatedAt = now
        )
        returnDataSource.updateReturn(updated)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = updated.projectId,
            returnId = updated.returnId,
            activityType = DeliveryReturnActivityType.RECEIVING_STARTED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = DeliveryReturnStatus.RECEIVING,
            notes = "Physical receiving started."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun receiveReturn(
        returnId: String,
        receivedQuantitiesByLine: Map<String, Double>,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.RECEIVE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transCheck = DeliveryReturnLifecycleValidator.validateTransition(existing.status, DeliveryReturnStatus.RECEIVED)
        if (transCheck is DomainResult.Error) return transCheck

        val lines = returnDataSource.getReturnLines(returnId)
        val now = System.currentTimeMillis()

        for (line in lines) {
            val received = receivedQuantitiesByLine[line.returnLineId] ?: line.returnedQuantity
            if (received < 0) {
                return DomainResult.Error(message = "Received quantity cannot be negative for line '${line.returnLineId}'.")
            }
            if (received > line.returnedQuantity + 0.001) {
                return DomainResult.Error(
                    message = "Received quantity ($received) cannot exceed requested return quantity (${line.returnedQuantity}) for line '${line.returnLineId}'."
                )
            }
            val updatedLine = line.copy(receivedQuantity = received, updatedAt = now)
            returnDataSource.updateReturnLine(updatedLine)
        }

        val updated = existing.copy(
            status = DeliveryReturnStatus.RECEIVED,
            receivedBy = actorId,
            receivedAt = now,
            updatedAt = now
        )
        returnDataSource.updateReturn(updated)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = updated.projectId,
            returnId = updated.returnId,
            activityType = DeliveryReturnActivityType.RECEIVED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = DeliveryReturnStatus.RECEIVED,
            notes = "Returned items received by '$actorId'."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun startInspection(
        returnId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.INSPECT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transCheck = DeliveryReturnLifecycleValidator.validateTransition(existing.status, DeliveryReturnStatus.INSPECTING)
        if (transCheck is DomainResult.Error) return transCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryReturnStatus.INSPECTING,
            updatedAt = now
        )
        returnDataSource.updateReturn(updated)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = updated.projectId,
            returnId = updated.returnId,
            activityType = DeliveryReturnActivityType.INSPECTION_STARTED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = DeliveryReturnStatus.INSPECTING,
            notes = "Return inspection started by '$actorId'."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun inspectReturnLine(
        returnId: String,
        returnLineId: String,
        acceptedQuantity: Double,
        rejectedQuantity: Double,
        condition: DeliveryReturnLineCondition,
        disposition: DeliveryReturnDisposition,
        inspectionNotes: String?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturnLine> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.INSPECT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val line = returnDataSource.getReturnLine(returnLineId)
            ?: return DomainResult.Error(message = "Return Line '$returnLineId' not found.")

        if (line.returnId != returnId) {
            return DomainResult.Error(message = "Line '$returnLineId' does not belong to return '$returnId'.")
        }

        val dispValidation = DeliveryReturnDispositionValidator.validateDispositionCompatibility(condition, disposition)
        if (dispValidation is DomainResult.Error) return dispValidation

        val now = System.currentTimeMillis()
        val updatedLine = line.copy(
            acceptedQuantity = acceptedQuantity,
            rejectedQuantity = rejectedQuantity,
            condition = condition,
            disposition = disposition,
            inspectionNotes = inspectionNotes,
            updatedAt = now
        )

        val lineValidation = DeliveryReturnLineValidator.validateLine(updatedLine)
        if (lineValidation is DomainResult.Error) return lineValidation

        returnDataSource.updateReturnLine(updatedLine)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            returnId = returnId,
            activityType = DeliveryReturnActivityType.INSPECTED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = existing.status,
            notes = "Line '${line.productId}' inspected: Accepted=$acceptedQuantity, Rejected=$rejectedQuantity, Condition=$condition, Disposition=$disposition"
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updatedLine)
    }

    override suspend fun completeInspection(
        returnId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.INSPECT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transCheck = DeliveryReturnLifecycleValidator.validateTransition(existing.status, DeliveryReturnStatus.INSPECTED)
        if (transCheck is DomainResult.Error) return transCheck

        val lines = returnDataSource.getReturnLines(returnId)
        val incompleteLine = lines.find { !it.isInspectionComplete }
        if (incompleteLine != null) {
            return DomainResult.Error(
                message = "Cannot complete inspection: Line '${incompleteLine.productId}' has not been inspected."
            )
        }

        val now = System.currentTimeMillis()
        val nextStatus = if (lines.all { it.isDispositionSet }) DeliveryReturnStatus.DISPOSITION_PENDING else DeliveryReturnStatus.INSPECTED
        val updated = existing.copy(
            status = nextStatus,
            inspectedBy = actorId,
            inspectedAt = now,
            updatedAt = now
        )
        returnDataSource.updateReturn(updated)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = updated.projectId,
            returnId = updated.returnId,
            activityType = DeliveryReturnActivityType.INSPECTED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = nextStatus,
            notes = "Inspection completed for all return lines by '$actorId'."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun setDisposition(
        returnId: String,
        returnLineId: String,
        disposition: DeliveryReturnDisposition,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturnLine> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.SET_DISPOSITION,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val line = returnDataSource.getReturnLine(returnLineId)
            ?: return DomainResult.Error(message = "Return Line '$returnLineId' not found.")

        val dispValidation = DeliveryReturnDispositionValidator.validateDispositionCompatibility(line.condition, disposition)
        if (dispValidation is DomainResult.Error) return dispValidation

        val now = System.currentTimeMillis()
        val updatedLine = line.copy(disposition = disposition, updatedAt = now)
        returnDataSource.updateReturnLine(updatedLine)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            returnId = returnId,
            activityType = DeliveryReturnActivityType.DISPOSITION_SET,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = existing.status,
            notes = "Disposition set to '$disposition' for line '${line.productId}'."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updatedLine)
    }

    override suspend fun processRestock(
        returnId: String,
        returnLineId: String,
        warehouseId: String,
        locationId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturnLine> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.PROCESS_RESTOCK,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val line = returnDataSource.getReturnLine(returnLineId)
            ?: return DomainResult.Error(message = "Return Line '$returnLineId' not found.")

        if (!line.disposition.allowsRestock) {
            return DomainResult.Error(
                message = "Line disposition is '${line.disposition.defaultLabel}', which does not allow restock."
            )
        }

        if (line.acceptedQuantity <= 0) {
            return DomainResult.Error(message = "Cannot restock line with 0 accepted quantity.")
        }

        // Idempotency check: if already restocked, return success safely
        if (line.isRestocked && !line.restockMovementId.isNullOrBlank()) {
            return DomainResult.Success(line)
        }

        val now = System.currentTimeMillis()
        val stockInId = UUID.randomUUID().toString()

        // Canonical Module 07 Stock-In integration
        if (receivingDataSource != null) {
            val stockInRecord = InventoryStockInRecord(
                stockInId = stockInId,
                receivingId = existing.returnId,
                receivingLineId = line.returnLineId,
                projectId = existing.projectId,
                inventoryProductId = line.productId,
                warehouseId = warehouseId,
                locationId = locationId,
                quantity = line.acceptedQuantity.toInt(),
                unit = InventoryUnit.PCS,
                createdBy = actorId,
                createdAt = now.toString(),
                sourceReference = "DELIVERY_RETURN:${existing.returnNo}"
            )
            receivingDataSource.insertStockInRecord(stockInRecord)
        }

        val updatedLine = line.copy(
            isRestocked = true,
            restockedQuantity = line.acceptedQuantity,
            restockMovementId = stockInId,
            restockedAt = now,
            warehouseId = warehouseId,
            locationId = locationId,
            updatedAt = now
        )
        returnDataSource.updateReturnLine(updatedLine)

        // Advance return status to PROCESSING if appropriate
        if (existing.status == DeliveryReturnStatus.INSPECTED || existing.status == DeliveryReturnStatus.DISPOSITION_PENDING) {
            val updatedReturn = existing.copy(status = DeliveryReturnStatus.PROCESSING, updatedAt = now)
            returnDataSource.updateReturn(updatedReturn)
        }

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            returnId = returnId,
            activityType = DeliveryReturnActivityType.INVENTORY_RESTOCKED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = DeliveryReturnStatus.PROCESSING,
            metadata = mapOf(
                "lineId" to line.returnLineId,
                "productId" to line.productId,
                "quantity" to line.acceptedQuantity.toString(),
                "stockInId" to stockInId,
                "warehouseId" to warehouseId,
                "locationId" to locationId
            ),
            notes = "Restocked ${line.acceptedQuantity} pcs of '${line.productId}' into WH '$warehouseId' Loc '$locationId' via Stock-In '$stockInId'."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updatedLine)
    }

    override suspend fun processAllRestock(
        returnId: String,
        defaultWarehouseId: String,
        defaultLocationId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        val lines = returnDataSource.getReturnLines(returnId)
        val restockableLines = lines.filter { it.disposition.allowsRestock && it.acceptedQuantity > 0 && !it.isRestocked }

        for (line in restockableLines) {
            val wId = line.warehouseId ?: defaultWarehouseId
            val locId = line.locationId ?: defaultLocationId

            val now = System.currentTimeMillis()
            val stockInId = UUID.randomUUID().toString()

            if (receivingDataSource != null) {
                val stockInRecord = InventoryStockInRecord(
                    stockInId = stockInId,
                    receivingId = existing.returnId,
                    receivingLineId = line.returnLineId,
                    projectId = existing.projectId,
                    inventoryProductId = line.productId,
                    warehouseId = wId,
                    locationId = locId,
                    quantity = line.acceptedQuantity.toInt(),
                    unit = InventoryUnit.PCS,
                    createdBy = actorId,
                    createdAt = now.toString(),
                    sourceReference = "DELIVERY_RETURN:${existing.returnNo}"
                )
                receivingDataSource.insertStockInRecord(stockInRecord)
            }

            val updatedLine = line.copy(
                isRestocked = true,
                restockedQuantity = line.acceptedQuantity,
                restockMovementId = stockInId,
                restockedAt = now,
                warehouseId = wId,
                locationId = locId,
                updatedAt = now
            )
            returnDataSource.updateReturnLine(updatedLine)
        }

        val now = System.currentTimeMillis()
        val updatedReturn = existing.copy(status = DeliveryReturnStatus.PROCESSING, updatedAt = now)
        returnDataSource.updateReturn(updatedReturn)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            returnId = returnId,
            activityType = DeliveryReturnActivityType.INVENTORY_RESTOCKED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = DeliveryReturnStatus.PROCESSING,
            notes = "Processed restock for ${restockableLines.size} line(s)."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updatedReturn)
    }

    override suspend fun completeReturn(
        returnId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.COMPLETE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transCheck = DeliveryReturnLifecycleValidator.validateTransition(existing.status, DeliveryReturnStatus.COMPLETED)
        if (transCheck is DomainResult.Error) return transCheck

        val lines = returnDataSource.getReturnLines(returnId)
        val uninspected = lines.find { !it.isInspectionComplete }
        if (uninspected != null) {
            return DomainResult.Error(message = "Cannot complete return: line '${uninspected.productId}' is not inspected.")
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryReturnStatus.COMPLETED,
            completedBy = actorId,
            completedAt = now,
            updatedAt = now
        )
        returnDataSource.updateReturn(updated)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = updated.projectId,
            returnId = updated.returnId,
            activityType = DeliveryReturnActivityType.COMPLETED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = DeliveryReturnStatus.COMPLETED,
            notes = "Delivery return completed by '$actorId'."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun cancelReturn(
        returnId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturn> = mutex.withLock {
        val existing = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.CANCEL,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transCheck = DeliveryReturnLifecycleValidator.validateTransition(existing.status, DeliveryReturnStatus.CANCELLED)
        if (transCheck is DomainResult.Error) return transCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryReturnStatus.CANCELLED,
            cancelledAt = now,
            notes = if (existing.notes != null) "${existing.notes}\nCancellation Reason: $reason" else "Cancellation Reason: $reason",
            updatedAt = now
        )
        returnDataSource.updateReturn(updated)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = updated.projectId,
            returnId = updated.returnId,
            activityType = DeliveryReturnActivityType.CANCELLED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = existing.status,
            newStatus = DeliveryReturnStatus.CANCELLED,
            notes = "Return cancelled: $reason"
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }

    override suspend fun getEligibleReturnQuantity(
        deliveryOrderId: String,
        deliveryOrderLineId: String,
        callerProjectId: String?
    ): DomainResult<Double> = mutex.withLock {
        val doOrder = doDataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery Order '$deliveryOrderId' not found.")

        if (callerProjectId != null && callerProjectId != doOrder.projectId) {
            return DomainResult.Error(message = "Access denied: Project mismatch.")
        }

        val doLines = doDataSource.getDeliveryOrderLines(deliveryOrderId)
        val doLine = doLines.find { it.lineId == deliveryOrderLineId }
            ?: return DomainResult.Error(message = "Delivery Order Line '$deliveryOrderLineId' not found.")

        val existingReturns = returnDataSource.getReturnsByDeliveryOrder(deliveryOrderId)
        val existingReturnsWithLines = existingReturns.map { it to returnDataSource.getReturnLines(it.returnId) }

        val eligible = DeliveryReturnEligibilityCalculator.calculateEligibleReturnQuantity(
            deliveryOrderLineId = deliveryOrderLineId,
            deliveredQuantity = doLine.requestedQuantity,
            existingReturns = existingReturnsWithLines
        )

        DomainResult.Success(eligible)
    }

    override suspend fun getEvents(
        returnId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryReturnActivityEvent>> = mutex.withLock {
        val ret = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.VIEW,
                targetProjectId = ret.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val events = returnDataSource.observeEvents(returnId)
        val snapshot = events.first()
        DomainResult.Success(snapshot)
    }

    override fun observeReverseShipment(returnId: String): Flow<DeliveryReturnShipment?> {
        return returnDataSource.observeReverseShipment(returnId)
    }

    override suspend fun getReverseShipment(
        returnId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturnShipment> = mutex.withLock {
        val ret = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.VIEW,
                targetProjectId = ret.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val shipment = returnDataSource.getReverseShipment(returnId)
            ?: return DomainResult.Error(message = "Reverse Shipment for return '$returnId' not found.")

        DomainResult.Success(shipment)
    }

    override suspend fun createReverseShipment(
        shipment: DeliveryReturnShipment,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturnShipment> = mutex.withLock {
        val ret = returnDataSource.getReturn(shipment.returnId)
            ?: return DomainResult.Error(message = "Delivery Return '${shipment.returnId}' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.RECEIVE,
                targetProjectId = ret.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val validation = DeliveryReverseShipmentValidator.validateShipment(shipment, ret.projectId)
        if (validation is DomainResult.Error) return validation

        returnDataSource.insertReverseShipment(shipment)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = ret.projectId,
            returnId = ret.returnId,
            activityType = DeliveryReturnActivityType.REVERSE_SHIPMENT_CREATED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = ret.status,
            newStatus = ret.status,
            metadata = mapOf(
                "reverseShipmentId" to shipment.reverseShipmentId,
                "carrierName" to shipment.carrierName,
                "trackingNumber" to (shipment.trackingNumber ?: "")
            ),
            notes = "Reverse shipment '${shipment.reverseShipmentId}' created with carrier '${shipment.carrierName}'."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(shipment)
    }

    override suspend fun updateReverseShipmentStatus(
        returnId: String,
        newStatus: DeliveryReturnShipmentStatus,
        notes: String?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryReturnShipment> = mutex.withLock {
        val ret = returnDataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Delivery Return '$returnId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryReturnOperation.RECEIVE,
                targetProjectId = ret.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val existing = returnDataSource.getReverseShipment(returnId)
            ?: return DomainResult.Error(message = "Reverse shipment for return '$returnId' not found.")

        val transCheck = DeliveryReverseShipmentValidator.validateTransition(existing.status, newStatus)
        if (transCheck is DomainResult.Error) return transCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = newStatus,
            notes = notes ?: existing.notes,
            pickedUpAt = if (newStatus == DeliveryReturnShipmentStatus.PICKED_UP && existing.pickedUpAt == null) now else existing.pickedUpAt,
            receivedAt = if (newStatus == DeliveryReturnShipmentStatus.DELIVERED_TO_WAREHOUSE && existing.receivedAt == null) now else existing.receivedAt,
            updatedAt = now
        )

        returnDataSource.updateReverseShipment(updated)

        val event = DeliveryReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = ret.projectId,
            returnId = ret.returnId,
            activityType = DeliveryReturnActivityType.REVERSE_SHIPMENT_UPDATED,
            actorId = actorId,
            actorRole = callerRole,
            previousStatus = ret.status,
            newStatus = ret.status,
            metadata = mapOf(
                "reverseShipmentId" to updated.reverseShipmentId,
                "status" to newStatus.name
            ),
            notes = "Reverse shipment status updated to '${newStatus.defaultLabel}'."
        )
        returnDataSource.insertEvent(event)

        DomainResult.Success(updated)
    }
}
