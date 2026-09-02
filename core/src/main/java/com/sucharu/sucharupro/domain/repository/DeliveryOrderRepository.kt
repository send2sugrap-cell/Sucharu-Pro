package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.DeliveryDispatchRequest
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Delivery Order & Dispatch Management (Module 08 Step 01).
 *
 * Enforces project isolation, RBAC permissions, lifecycle validations, and audit trail logging.
 * Does NOT mutate physical stock or ledger records.
 */
interface DeliveryOrderRepository {

    // ──────────────────────────────────────────────────────────────
    // Delivery Order Queries
    // ──────────────────────────────────────────────────────────────

    fun observeDeliveryOrders(projectId: String): Flow<List<DeliveryOrder>>

    fun observeDeliveryOrder(deliveryOrderId: String): Flow<DeliveryOrder?>

    suspend fun getDeliveryOrder(
        deliveryOrderId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryOrder>

    // ──────────────────────────────────────────────────────────────
    // Delivery Order Line Queries
    // ──────────────────────────────────────────────────────────────

    fun observeDeliveryOrderLines(deliveryOrderId: String): Flow<List<DeliveryOrderLine>>

    suspend fun getDeliveryOrderLines(
        deliveryOrderId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryOrderLine>>

    suspend fun getDeliveryOrderLine(
        lineId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryOrderLine>

    // ──────────────────────────────────────────────────────────────
    // Dispatch Request Queries
    // ──────────────────────────────────────────────────────────────

    fun observeDispatchRequests(projectId: String): Flow<List<DeliveryDispatchRequest>>

    fun observeDispatchRequest(dispatchRequestId: String): Flow<DeliveryDispatchRequest?>

    suspend fun getDispatchRequest(
        dispatchRequestId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryDispatchRequest>

    suspend fun getDispatchRequestForOrder(
        deliveryOrderId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryDispatchRequest?>

    // ──────────────────────────────────────────────────────────────
    // Activity Queries
    // ──────────────────────────────────────────────────────────────

    fun observeActivityEvents(deliveryOrderId: String): Flow<List<DeliveryActivityEvent>>

    suspend fun getActivityEvents(
        deliveryOrderId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryActivityEvent>>

    // ──────────────────────────────────────────────────────────────
    // Delivery Order Mutations
    // ──────────────────────────────────────────────────────────────

    suspend fun createDeliveryOrder(
        order: DeliveryOrder,
        lines: List<DeliveryOrderLine>,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryOrder>

    suspend fun updateDraftDeliveryOrder(
        deliveryOrderId: String,
        deliveryType: DeliveryOrderType,
        priority: DeliveryPriority,
        requestedDeliveryDate: Long,
        notes: String?,
        lines: List<DeliveryOrderLine>,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryOrder>

    suspend fun submitDeliveryOrder(
        deliveryOrderId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryOrder>

    suspend fun approveDeliveryOrder(
        deliveryOrderId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryOrder>

    suspend fun markReadyForDispatch(
        deliveryOrderId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryOrder>

    suspend fun cancelDeliveryOrder(
        deliveryOrderId: String,
        actorId: String,
        reason: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryOrder>

    // ──────────────────────────────────────────────────────────────
    // Dispatch Request Mutations
    // ──────────────────────────────────────────────────────────────

    suspend fun createDispatchRequest(
        request: DeliveryDispatchRequest,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryDispatchRequest>
}
