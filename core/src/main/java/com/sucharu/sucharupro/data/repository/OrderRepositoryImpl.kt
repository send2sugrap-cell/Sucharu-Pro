package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.OrderDataSource
import com.sucharu.sucharupro.data.datasource.QuotationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Production-ready implementation of [OrderRepository] delegating to [OrderDataSource].
 */
class OrderRepositoryImpl(
    private val dataSource: OrderDataSource,
    private val quotationDataSource: QuotationDataSource? = null
) : OrderRepository {

    override fun getOrders(): Flow<List<Order>> = dataSource.observeOrders()

    override fun getOrderById(orderId: String): Flow<Order?> {
        return dataSource.observeOrders().map { list ->
            list.find { it.orderId == orderId }
        }
    }

    override suspend fun findOrderById(orderId: String): DomainResult<Order> {
        return dataSource.fetchOrderById(orderId)
    }

    override fun getOrdersForCustomer(customerId: String): Flow<List<Order>> {
        return dataSource.observeOrders().map { list ->
            list.filter { it.customerId == customerId }
        }
    }

    override fun getOrdersForQuotation(quotationId: String): Flow<List<Order>> {
        return dataSource.observeOrders().map { list ->
            list.filter { it.quotationId == quotationId }
        }
    }

    override suspend fun createOrder(order: Order): DomainResult<Order> {
        if (order.orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        if (order.orderNumber.isBlank()) {
            return DomainResult.Error(message = "Order Number cannot be blank.")
        }
        if (order.customerId.isBlank()) {
            return DomainResult.Error(message = "Customer ID cannot be blank.")
        }
        val integrityResult = com.sucharu.sucharupro.domain.validation.OrderLifecycleValidator.validateOrderIntegrity(order)
        if (integrityResult is DomainResult.Error) {
            return integrityResult
        }
        return dataSource.insertOrder(order)
    }

    override suspend fun updateOrder(order: Order): DomainResult<Order> {
        if (order.orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        val integrityResult = com.sucharu.sucharupro.domain.validation.OrderLifecycleValidator.validateOrderIntegrity(order)
        if (integrityResult is DomainResult.Error) {
            return integrityResult
        }
        return dataSource.updateOrder(order)
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        status: OrderStatusType
    ): DomainResult<Order> {
        if (orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        return dataSource.updateOrderStatus(orderId, status)
    }

    override suspend fun updateOrderPriority(
        orderId: String,
        priority: OrderPriority
    ): DomainResult<Order> {
        if (orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        return dataSource.updateOrderPriority(orderId, priority)
    }

    override suspend fun markReadyForJob(orderId: String): DomainResult<Order> {
        if (orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }

        // 1. Fetch order
        val order = when (val orderResult = dataSource.fetchOrderById(orderId)) {
            is DomainResult.Success -> orderResult.data
            is DomainResult.Error -> return DomainResult.Error(message = "Order not found: $orderId")
            DomainResult.Loading -> return DomainResult.Error(message = "Order data is loading.")
        }

        // 2. Pure business validation for handoff readiness
        val validation = com.sucharu.sucharupro.domain.validation.OrderLifecycleValidator.validateJobHandoffReadiness(order)
        if (validation is DomainResult.Error) {
            return validation
        }

        // 3. Update handoff status
        return dataSource.updateJobHandoffStatus(orderId, com.sucharu.sucharupro.domain.model.order.JobHandoffStatus.READY_FOR_JOB)
    }

    override suspend fun updateJobHandoffStatus(
        orderId: String,
        status: com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
    ): DomainResult<Order> {
        if (orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        return dataSource.updateJobHandoffStatus(orderId, status)
    }

    override suspend fun updateOrderNotes(
        orderId: String,
        notes: String?
    ): DomainResult<Order> {
        if (orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        return dataSource.updateOrderNotes(orderId, notes)
    }

    override suspend fun cancelOrder(orderId: String, reason: String?): DomainResult<Order> {
        if (orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        return dataSource.cancelOrder(orderId, reason)
    }

    override suspend fun createOrderFromApprovedQuotation(
        orderId: String,
        orderNumber: String,
        quotationId: String,
        approvedRevisionId: String,
        priority: OrderPriority,
        confirmedBy: String?,
        timestamp: String
    ): DomainResult<Order> {
        if (orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        if (orderNumber.isBlank()) {
            return DomainResult.Error(message = "Order Number cannot be blank.")
        }
        if (quotationDataSource == null) {
            return DomainResult.Error(message = "QuotationDataSource is required to create an Order from an approved quotation.")
        }

        // 1. Fetch quotation
        val quotation = when (val qResult = quotationDataSource.fetchQuotationById(quotationId)) {
            is DomainResult.Success -> qResult.data
            is DomainResult.Error -> return DomainResult.Error(
                message = "Failed to fetch quotation: ${qResult.message}"
            )
            DomainResult.Loading -> return DomainResult.Error(message = "Quotation data is still loading.")
        }

        // 2. Validate quotation approval
        if (!quotation.isApproved) {
            return DomainResult.Error(
                message = "Quotation '$quotationId' is not in APPROVED state (current status: ${quotation.status.defaultLabel})."
            )
        }

        // 3. Validate revision
        val revision = quotation.revisions.find { it.revisionId == approvedRevisionId }
            ?: return DomainResult.Error(
                message = "Revision '$approvedRevisionId' not found in quotation '$quotationId'."
            )

        if (quotation.approvedRevisionId != approvedRevisionId) {
            return DomainResult.Error(
                message = "Revision '$approvedRevisionId' is not the currently approved revision of quotation '$quotationId'."
            )
        }

        // 4. Create Order snapshot using domain factory
        val orderSnapshot = try {
            Order.fromApprovedQuotation(
                orderId = orderId,
                orderNumber = orderNumber,
                quotation = quotation,
                revision = revision,
                priority = priority,
                confirmedBy = confirmedBy,
                timestamp = timestamp
            )
        } catch (e: IllegalArgumentException) {
            return DomainResult.Error(message = e.message ?: "Invalid order snapshot parameters.", exception = e)
        }

        // 5. Insert order into repository data source
        return dataSource.insertOrder(orderSnapshot)
    }
}
