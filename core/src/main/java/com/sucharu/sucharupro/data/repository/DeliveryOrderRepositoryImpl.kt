package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DeliveryOrderDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.DeliveryActivityType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryDispatchRequest
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryOrderRepository
import com.sucharu.sucharupro.domain.validation.DeliveryAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.DeliveryDispatchRequestValidator
import com.sucharu.sucharupro.domain.validation.DeliveryOperation
import com.sucharu.sucharupro.domain.validation.DeliveryOrderValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production implementation of [DeliveryOrderRepository] (Module 08 Step 01).
 *
 * Implements project isolation, RBAC enforcement, lifecycle control, and audit logging.
 * Does NOT mutate physical stock or ledger records.
 */
class DeliveryOrderRepositoryImpl(
    private val dataSource: DeliveryOrderDataSource
) : DeliveryOrderRepository {

    private val mutex = Mutex()

    // ──────────────────────────────────────────────────────────────
    // Delivery Order Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeDeliveryOrders(projectId: String): Flow<List<DeliveryOrder>> {
        return dataSource.observeDeliveryOrders(projectId)
    }

    override fun observeDeliveryOrder(deliveryOrderId: String): Flow<DeliveryOrder?> {
        return dataSource.observeDeliveryOrder(deliveryOrderId)
    }

    override suspend fun getDeliveryOrder(
        deliveryOrderId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryOrder> {
        val order = dataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery order '$deliveryOrderId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.VIEW,
                targetProjectId = order.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        return DomainResult.Success(order)
    }

    // ──────────────────────────────────────────────────────────────
    // Delivery Order Line Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeDeliveryOrderLines(deliveryOrderId: String): Flow<List<DeliveryOrderLine>> {
        return dataSource.observeDeliveryOrderLines(deliveryOrderId)
    }

    override suspend fun getDeliveryOrderLines(
        deliveryOrderId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryOrderLine>> {
        val order = dataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery order '$deliveryOrderId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.VIEW,
                targetProjectId = order.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lines = dataSource.getDeliveryOrderLines(deliveryOrderId)
        return DomainResult.Success(lines)
    }

    override suspend fun getDeliveryOrderLine(
        lineId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryOrderLine> {
        val line = dataSource.getDeliveryOrderLine(lineId)
            ?: return DomainResult.Error(message = "Delivery order line '$lineId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.VIEW,
                targetProjectId = line.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        return DomainResult.Success(line)
    }

    // ──────────────────────────────────────────────────────────────
    // Dispatch Request Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeDispatchRequests(projectId: String): Flow<List<DeliveryDispatchRequest>> {
        return dataSource.observeDispatchRequests(projectId)
    }

    override fun observeDispatchRequest(dispatchRequestId: String): Flow<DeliveryDispatchRequest?> {
        return dataSource.observeDispatchRequest(dispatchRequestId)
    }

    override suspend fun getDispatchRequest(
        dispatchRequestId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryDispatchRequest> {
        val request = dataSource.getDispatchRequest(dispatchRequestId)
            ?: return DomainResult.Error(message = "Dispatch request '$dispatchRequestId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.VIEW,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        return DomainResult.Success(request)
    }

    override suspend fun getDispatchRequestForOrder(
        deliveryOrderId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryDispatchRequest?> {
        val order = dataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery order '$deliveryOrderId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.VIEW,
                targetProjectId = order.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val request = dataSource.getDispatchRequestForOrder(deliveryOrderId)
        return DomainResult.Success(request)
    }

    // ──────────────────────────────────────────────────────────────
    // Activity Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeActivityEvents(deliveryOrderId: String): Flow<List<DeliveryActivityEvent>> {
        return dataSource.observeActivityEvents(deliveryOrderId)
    }

    override suspend fun getActivityEvents(
        deliveryOrderId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryActivityEvent>> {
        val order = dataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery order '$deliveryOrderId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.VIEW,
                targetProjectId = order.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val events = dataSource.getActivityEvents(deliveryOrderId)
        return DomainResult.Success(events)
    }

    // ──────────────────────────────────────────────────────────────
    // Delivery Order Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun createDeliveryOrder(
        order: DeliveryOrder,
        lines: List<DeliveryOrderLine>,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryOrder> = mutex.withLock {
        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.CREATE,
                targetProjectId = order.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Validation
        val validationResult = DeliveryOrderValidator.validateDeliveryOrder(order, lines)
        if (validationResult is DomainResult.Error) return validationResult

        // 3. Unique number check
        val existingOrder = dataSource.getDeliveryOrderByNo(order.projectId, order.deliveryOrderNo)
        if (existingOrder != null) {
            return DomainResult.Error(
                message = "Delivery order number '${order.deliveryOrderNo}' already exists in project '${order.projectId}'."
            )
        }

        // 4. Persistence
        dataSource.insertDeliveryOrder(order, lines)

        // 5. Audit trail
        val activity = DeliveryActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = order.projectId,
            deliveryOrderId = order.deliveryOrderId,
            activityType = DeliveryActivityType.CREATED,
            performedBy = order.createdBy,
            performedAt = order.createdAt,
            details = "Delivery order created with ${lines.size} item line(s).",
            newStatus = order.status.name
        )
        dataSource.insertActivityEvent(activity)

        DomainResult.Success(order)
    }

    override suspend fun updateDraftDeliveryOrder(
        deliveryOrderId: String,
        deliveryType: DeliveryOrderType,
        priority: DeliveryPriority,
        requestedDeliveryDate: Long,
        notes: String?,
        lines: List<DeliveryOrderLine>,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryOrder> = mutex.withLock {
        val existing = dataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery order '$deliveryOrderId' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.EDIT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Draft status check
        if (existing.status != DeliveryOrderStatus.DRAFT) {
            return DomainResult.Error(
                message = "Only DRAFT delivery orders can be modified. Current status: '${existing.status}'."
            )
        }

        val updatedOrder = existing.copy(
            deliveryType = deliveryType,
            priority = priority,
            requestedDeliveryDate = requestedDeliveryDate,
            notes = notes,
            updatedAt = System.currentTimeMillis()
        )

        // 3. Validation
        val validationResult = DeliveryOrderValidator.validateDeliveryOrder(updatedOrder, lines)
        if (validationResult is DomainResult.Error) return validationResult

        val immutabilityCheck = DeliveryOrderValidator.validateImmutableIdentity(existing, updatedOrder)
        if (immutabilityCheck is DomainResult.Error) return immutabilityCheck

        // 4. Persistence
        dataSource.updateDeliveryOrderWithLines(updatedOrder, lines)

        // 5. Audit
        val activity = DeliveryActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            deliveryOrderId = existing.deliveryOrderId,
            activityType = DeliveryActivityType.NOTES_UPDATED,
            performedBy = actorId,
            performedAt = updatedOrder.updatedAt,
            details = "Draft delivery order updated with ${lines.size} item line(s)."
        )
        dataSource.insertActivityEvent(activity)

        DomainResult.Success(updatedOrder)
    }

    override suspend fun submitDeliveryOrder(
        deliveryOrderId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryOrder> = mutex.withLock {
        val existing = dataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery order '$deliveryOrderId' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.SUBMIT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Lifecycle
        val transitionCheck = DeliveryOrderValidator.validateStatusTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryOrderStatus.PENDING
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryOrderStatus.PENDING,
            updatedAt = now
        )

        dataSource.updateDeliveryOrder(updated)

        // 3. Audit
        val activity = DeliveryActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            deliveryOrderId = existing.deliveryOrderId,
            activityType = DeliveryActivityType.STATUS_CHANGED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryOrderStatus.PENDING.name,
            details = "Delivery order submitted for approval."
        )
        dataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun approveDeliveryOrder(
        deliveryOrderId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryOrder> = mutex.withLock {
        val existing = dataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery order '$deliveryOrderId' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.APPROVE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Lifecycle
        val transitionCheck = DeliveryOrderValidator.validateStatusTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryOrderStatus.APPROVED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryOrderStatus.APPROVED,
            updatedAt = now
        )

        dataSource.updateDeliveryOrder(updated)

        // 3. Audit
        val activity = DeliveryActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            deliveryOrderId = existing.deliveryOrderId,
            activityType = DeliveryActivityType.STATUS_CHANGED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryOrderStatus.APPROVED.name,
            details = "Delivery order approved."
        )
        dataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun markReadyForDispatch(
        deliveryOrderId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryOrder> = mutex.withLock {
        val existing = dataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery order '$deliveryOrderId' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.READY_FOR_DISPATCH,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Lifecycle
        val transitionCheck = DeliveryOrderValidator.validateStatusTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryOrderStatus.READY_FOR_DISPATCH
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryOrderStatus.READY_FOR_DISPATCH,
            updatedAt = now
        )

        dataSource.updateDeliveryOrder(updated)

        // 3. Audit
        val activity = DeliveryActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            deliveryOrderId = existing.deliveryOrderId,
            activityType = DeliveryActivityType.STATUS_CHANGED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryOrderStatus.READY_FOR_DISPATCH.name,
            details = "Delivery order marked as ready for dispatch."
        )
        dataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun cancelDeliveryOrder(
        deliveryOrderId: String,
        actorId: String,
        reason: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryOrder> = mutex.withLock {
        val existing = dataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery order '$deliveryOrderId' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.CANCEL,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Lifecycle
        val transitionCheck = DeliveryOrderValidator.validateStatusTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryOrderStatus.CANCELLED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryOrderStatus.CANCELLED,
            updatedAt = now
        )

        dataSource.updateDeliveryOrder(updated)

        // 3. Audit
        val activity = DeliveryActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            deliveryOrderId = existing.deliveryOrderId,
            activityType = DeliveryActivityType.CANCELLED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryOrderStatus.CANCELLED.name,
            details = reason ?: "Delivery order cancelled."
        )
        dataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    // ──────────────────────────────────────────────────────────────
    // Dispatch Request Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun createDispatchRequest(
        request: DeliveryDispatchRequest,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryDispatchRequest> = mutex.withLock {
        val order = dataSource.getDeliveryOrder(request.deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery order '${request.deliveryOrderId}' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryOperation.CREATE_DISPATCH_REQUEST,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Project consistency
        if (request.projectId != order.projectId) {
            return DomainResult.Error(
                message = "Project ID mismatch: Dispatch request belongs to '${request.projectId}', but delivery order belongs to '${order.projectId}'."
            )
        }

        // 3. Structural validation
        val requestValidation = DeliveryDispatchRequestValidator.validateDispatchRequest(request)
        if (requestValidation is DomainResult.Error) return requestValidation

        // 4. Eligibility check (Order must be APPROVED or READY_FOR_DISPATCH)
        val eligibilityCheck = DeliveryDispatchRequestValidator.validateEligibilityForDispatch(order.status)
        if (eligibilityCheck is DomainResult.Error) return eligibilityCheck

        // 5. Prevent duplicate dispatch requests
        val existingRequest = dataSource.getDispatchRequestForOrder(request.deliveryOrderId)
        if (existingRequest != null) {
            return DomainResult.Error(
                message = "A dispatch request already exists for delivery order '${request.deliveryOrderId}'."
            )
        }

        // 6. Persistence
        dataSource.insertDispatchRequest(request)

        // 7. Audit
        val activity = DeliveryActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = order.projectId,
            deliveryOrderId = order.deliveryOrderId,
            activityType = DeliveryActivityType.DISPATCH_REQUESTED,
            performedBy = request.requestedBy,
            performedAt = request.requestedAt,
            details = "Dispatch request created (${request.priority} priority)."
        )
        dataSource.insertActivityEvent(activity)

        DomainResult.Success(request)
    }
}
