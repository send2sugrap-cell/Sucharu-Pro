package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Commercial Customer Orders in Sucharu Pro.
 */
interface OrderRepository {

    /** Reactive stream of all customer orders. */
    fun getOrders(): Flow<List<Order>>

    /** Reactive stream observing a single order by [orderId]. */
    fun getOrderById(orderId: String): Flow<Order?>

    /** Direct lookup of an order by [orderId]. */
    suspend fun findOrderById(orderId: String): DomainResult<Order>

    /** Reactive stream of orders for a specific customer. */
    fun getOrdersForCustomer(customerId: String): Flow<List<Order>>

    /** Reactive stream of orders originating from a specific quotation. */
    fun getOrdersForQuotation(quotationId: String): Flow<List<Order>>

    /** Creates a direct order. */
    suspend fun createOrder(order: Order): DomainResult<Order>

    /** Updates an existing order while preserving its ID and snapshot integrity. */
    suspend fun updateOrder(order: Order): DomainResult<Order>

    /** Updates commercial order status following domain transition rules. */
    suspend fun updateOrderStatus(orderId: String, status: OrderStatusType): DomainResult<Order>

    /** Updates commercial priority of an order. */
    suspend fun updateOrderPriority(orderId: String, priority: OrderPriority): DomainResult<Order>

    /** Validates business rules and marks the order as ready for job handoff. */
    suspend fun markReadyForJob(orderId: String): DomainResult<Order>

    /** Updates the job handoff status of an order. */
    suspend fun updateJobHandoffStatus(orderId: String, status: JobHandoffStatus): DomainResult<Order>

    /** Updates operational notes on an order. */
    suspend fun updateOrderNotes(orderId: String, notes: String?): DomainResult<Order>

    /** Cancels an active order. */
    suspend fun cancelOrder(orderId: String, reason: String? = null): DomainResult<Order>

    /**
     * Converts an approved quotation revision into a confirmed order.
     * Enforces the commercial snapshot principle.
     */
    suspend fun createOrderFromApprovedQuotation(
        orderId: String,
        orderNumber: String,
        quotationId: String,
        approvedRevisionId: String,
        priority: OrderPriority = OrderPriority.NORMAL,
        confirmedBy: String? = null,
        timestamp: String
    ): DomainResult<Order>
}
