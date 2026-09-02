package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import kotlinx.coroutines.flow.Flow

/**
 * Data source abstraction for Commercial Customer Orders in Sucharu Pro.
 */
interface OrderDataSource {

    /** Continuous reactive stream of all orders. */
    fun observeOrders(): Flow<List<Order>>

    /** One-shot fetch of an order by ID. */
    suspend fun fetchOrderById(orderId: String): DomainResult<Order>

    /** Inserts a new order. */
    suspend fun insertOrder(order: Order): DomainResult<Order>

    /** Updates an existing order. */
    suspend fun updateOrder(order: Order): DomainResult<Order>

    /** Updates the status of an order. */
    suspend fun updateOrderStatus(orderId: String, status: OrderStatusType): DomainResult<Order>

    /** Updates the priority of an order. */
    suspend fun updateOrderPriority(orderId: String, priority: OrderPriority): DomainResult<Order>

    /** Updates the job handoff status of an order. */
    suspend fun updateJobHandoffStatus(orderId: String, status: JobHandoffStatus): DomainResult<Order>

    /** Updates operational notes on an order. */
    suspend fun updateOrderNotes(orderId: String, notes: String?): DomainResult<Order>

    /** Cancels an order. */
    suspend fun cancelOrder(orderId: String, reason: String? = null): DomainResult<Order>
}
