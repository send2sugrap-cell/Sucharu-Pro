package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.DeliveryType
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.order.PaymentTermType
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory reactive implementation of [OrderDataSource] for development and testing.
 */
class FakeOrderDataSource(
    initialOrders: List<Order> = defaultSampleOrders()
) : OrderDataSource {

    private val mutex = Mutex()
    private val _orders = MutableStateFlow<List<Order>>(initialOrders)

    override fun observeOrders(): Flow<List<Order>> = _orders.asStateFlow()

    override suspend fun fetchOrderById(orderId: String): DomainResult<Order> = mutex.withLock {
        val order = _orders.value.find { it.orderId == orderId }
        return if (order != null) {
            DomainResult.Success(order)
        } else {
            DomainResult.Error(message = "Order not found with ID: $orderId")
        }
    }

    override suspend fun insertOrder(order: Order): DomainResult<Order> = mutex.withLock {
        if (_orders.value.any { it.orderId == order.orderId }) {
            return DomainResult.Error(message = "Order with ID '${order.orderId}' already exists.")
        }
        if (_orders.value.any { it.orderNumber.equals(order.orderNumber, ignoreCase = true) }) {
            return DomainResult.Error(message = "Order with Number '${order.orderNumber}' already exists.")
        }
        if (order.items.isEmpty()) {
            return DomainResult.Error(message = "Order must contain at least one line item.")
        }

        _orders.value = _orders.value + order
        DomainResult.Success(order)
    }

    override suspend fun updateOrder(order: Order): DomainResult<Order> = mutex.withLock {
        val index = _orders.value.indexOfFirst { it.orderId == order.orderId }
        if (index == -1) {
            return DomainResult.Error(message = "Cannot update non-existent order: ${order.orderId}")
        }

        val existing = _orders.value[index]
        val updated = order.copy(
            orderId = existing.orderId,
            createdAt = existing.createdAt
        )

        val currentList = _orders.value.toMutableList()
        currentList[index] = updated
        _orders.value = currentList.toList()
        DomainResult.Success(updated)
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        status: OrderStatusType
    ): DomainResult<Order> = mutex.withLock {
        val index = _orders.value.indexOfFirst { it.orderId == orderId }
        if (index == -1) {
            return DomainResult.Error(message = "Order not found: $orderId")
        }

        val existing = _orders.value[index]
        val validation = com.sucharu.sucharupro.domain.validation.OrderLifecycleValidator.validateStatusTransition(existing, status)
        if (validation is DomainResult.Error) {
            return validation
        }

        val updated = existing.copy(status = status)
        val currentList = _orders.value.toMutableList()
        currentList[index] = updated
        _orders.value = currentList.toList()
        DomainResult.Success(updated)
    }

    override suspend fun updateOrderPriority(
        orderId: String,
        priority: OrderPriority
    ): DomainResult<Order> = mutex.withLock {
        val index = _orders.value.indexOfFirst { it.orderId == orderId }
        if (index == -1) {
            return DomainResult.Error(message = "Order not found: $orderId")
        }

        val existing = _orders.value[index]
        val validation = com.sucharu.sucharupro.domain.validation.OrderLifecycleValidator.validatePriorityChange(existing, priority)
        if (validation is DomainResult.Error) {
            return validation
        }

        val updated = existing.copy(priority = priority)
        val currentList = _orders.value.toMutableList()
        currentList[index] = updated
        _orders.value = currentList.toList()
        DomainResult.Success(updated)
    }

    override suspend fun updateJobHandoffStatus(
        orderId: String,
        status: JobHandoffStatus
    ): DomainResult<Order> = mutex.withLock {
        val index = _orders.value.indexOfFirst { it.orderId == orderId }
        if (index == -1) {
            return DomainResult.Error(message = "Order not found: $orderId")
        }

        val existing = _orders.value[index]
        if (com.sucharu.sucharupro.domain.validation.OrderLifecycleValidator.isTerminal(existing)) {
            return DomainResult.Error(message = "Cannot update handoff status of a terminal order (${existing.status.defaultLabel}).")
        }

        val updated = existing.copy(jobHandoffStatus = status)
        val currentList = _orders.value.toMutableList()
        currentList[index] = updated
        _orders.value = currentList.toList()
        DomainResult.Success(updated)
    }

    override suspend fun updateOrderNotes(
        orderId: String,
        notes: String?
    ): DomainResult<Order> = mutex.withLock {
        val index = _orders.value.indexOfFirst { it.orderId == orderId }
        if (index == -1) {
            return DomainResult.Error(message = "Order not found: $orderId")
        }

        val existing = _orders.value[index]
        if (com.sucharu.sucharupro.domain.validation.OrderLifecycleValidator.isTerminal(existing)) {
            return DomainResult.Error(message = "Cannot update remarks on a terminal order (${existing.status.defaultLabel}).")
        }

        val updated = existing.copy(notes = notes)
        val currentList = _orders.value.toMutableList()
        currentList[index] = updated
        _orders.value = currentList.toList()
        DomainResult.Success(updated)
    }

    override suspend fun cancelOrder(
        orderId: String,
        reason: String?
    ): DomainResult<Order> = mutex.withLock {
        val index = _orders.value.indexOfFirst { it.orderId == orderId }
        if (index == -1) {
            return DomainResult.Error(message = "Order not found: $orderId")
        }

        val existing = _orders.value[index]
        val validation = com.sucharu.sucharupro.domain.validation.OrderLifecycleValidator.validateCancellation(existing, reason)
        if (validation is DomainResult.Error) {
            return validation
        }

        val trimmedReason = reason!!.trim()
        val updatedNotes = if (!existing.notes.isNullOrBlank()) {
            "${existing.notes}\nCancellation Reason: $trimmedReason"
        } else {
            "Cancellation Reason: $trimmedReason"
        }

        val updated = existing.copy(
            status = OrderStatusType.CANCELLED,
            notes = updatedNotes
        )
        val currentList = _orders.value.toMutableList()
        currentList[index] = updated
        _orders.value = currentList.toList()
        DomainResult.Success(updated)
    }

    companion object {
        fun defaultSampleOrders(): List<Order> = listOf(
            Order(
                orderId = "ord-001",
                orderNumber = "ORD-000001",
                customerId = "cus-001",
                quotationId = "qt-001",
                approvedQuotationRevisionId = "rev-001-v2",
                status = OrderStatusType.CONFIRMED,
                priority = OrderPriority.NORMAL,
                items = listOf(
                    OrderItem(
                        itemId = "qt-item-01",
                        description = "Visiting Card (300 GSM Art Card, Matte + Spot UV)",
                        specification = "3.25x2.0 in, 4/4 Color + Spot UV, 1000 Pcs",
                        quantity = 1000,
                        unit = "Pcs",
                        unitPrice = 1.20.toMoney(),
                        discount = 100.toMoney()
                    )
                ),
                discount = Money.ZERO,
                paymentTerms = PaymentTerms(
                    type = PaymentTermType.PARTIAL_ADVANCE,
                    advancePercentage = 50
                ),
                deliveryRequirement = DeliveryRequirement.DEFAULT_PICKUP,
                jobHandoffStatus = JobHandoffStatus.READY_FOR_JOB,
                notes = "Advance deposit of ৳ 550 received via Cash.",
                confirmedAt = "2026-08-13T11:30:00Z",
                confirmedBy = "Sales Desk",
                createdAt = "2026-08-13T11:30:00Z",
                updatedAt = "2026-08-13T11:30:00Z"
            ),
            Order(
                orderId = "ord-002",
                orderNumber = "ORD-000002",
                customerId = "cus-002",
                quotationId = null,
                approvedQuotationRevisionId = null,
                status = OrderStatusType.IN_PRODUCTION,
                priority = OrderPriority.HIGH,
                items = listOf(
                    OrderItem(
                        itemId = "item-02",
                        description = "Corporate Brochure (170 GSM Art Paper, 8 Pages)",
                        specification = "A4 Folded, 4/4 Color, Center Pin",
                        quantity = 2500,
                        unit = "Pcs",
                        unitPrice = 22.0.toMoney(),
                        discount = 1000.toMoney()
                    )
                ),
                discount = Money.ZERO,
                paymentTerms = PaymentTerms(
                    type = PaymentTermType.FULL_ADVANCE,
                    advancePercentage = 100
                ),
                deliveryRequirement = DeliveryRequirement(
                    deliveryType = DeliveryType.BUSINESS_DELIVERY,
                    requiredDate = "2026-08-20",
                    address = "Plot 7, Kawran Bazar, Dhaka",
                    contactName = "Mr. Shakil",
                    contactPhone = "+880 1712-345678"
                ),
                jobHandoffStatus = JobHandoffStatus.READY_FOR_JOB,
                notes = "High-priority direct order for upcoming trade fair.",
                confirmedAt = "2026-08-14T10:00:00Z",
                confirmedBy = "Account Executive",
                createdAt = "2026-08-14T10:00:00Z",
                updatedAt = "2026-08-14T15:00:00Z"
            )
        )
    }
}
