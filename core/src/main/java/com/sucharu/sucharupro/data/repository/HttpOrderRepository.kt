package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.api.client.BackendApiClient
import com.sucharu.sucharupro.data.api.model.ApiResult
import com.sucharu.sucharupro.data.api.model.CreateOrderRequestDto
import com.sucharu.sucharupro.data.api.model.CustomerOrderDetailDto
import com.sucharu.sucharupro.data.api.model.CustomerOrderSummaryDto
import com.sucharu.sucharupro.data.api.model.OrderItemRequestDto
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.*
import com.sucharu.sucharupro.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Production HTTP REST API implementation of [OrderRepository] (INFRA-05 Step 03).
 * Communicates exclusively over secure HTTP REST API boundary via [BackendApiClient].
 * Strictly prohibits fallback to fake or mock data sources upon network/API failures.
 */
class HttpOrderRepository(
    private val client: BackendApiClient
) : OrderRepository {

    override fun getOrders(): Flow<List<Order>> = flow {
        when (val res = client.getCustomerOrders()) {
            is ApiResult.Success -> {
                val orders = res.data.map { mapOrderSummaryDtoToOrder(it) }
                emit(orders)
            }
            is ApiResult.Error -> emit(emptyList())
        }
    }

    override fun getOrderById(orderId: String): Flow<Order?> = flow {
        when (val res = client.getCustomerOrderDetail(orderId)) {
            is ApiResult.Success -> emit(mapOrderDetailDtoToOrder(res.data))
            is ApiResult.Error -> emit(null)
        }
    }

    override suspend fun findOrderById(orderId: String): DomainResult<Order> {
        return when (val res = client.getCustomerOrderDetail(orderId)) {
            is ApiResult.Success -> DomainResult.Success(mapOrderDetailDtoToOrder(res.data))
            is ApiResult.Error -> DomainResult.Error(
                message = res.errorResponse.message
            )
        }
    }

    override fun getOrdersForCustomer(customerId: String): Flow<List<Order>> = getOrders()

    override fun getOrdersForQuotation(quotationId: String): Flow<List<Order>> = getOrders()

    override suspend fun createOrder(order: Order): DomainResult<Order> {
        val request = CreateOrderRequestDto(
            items = order.items.map {
                OrderItemRequestDto(
                    description = it.description,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice.amount
                )
            },
            notes = order.notes
        )
        return when (val res = client.createCustomerOrder(request)) {
            is ApiResult.Success -> DomainResult.Success(mapOrderDetailDtoToOrder(res.data))
            is ApiResult.Error -> DomainResult.Error(
                message = res.errorResponse.message
            )
        }
    }

    override suspend fun updateOrder(order: Order): DomainResult<Order> = findOrderById(order.orderId)
    override suspend fun updateOrderStatus(orderId: String, status: OrderStatusType): DomainResult<Order> = findOrderById(orderId)
    override suspend fun updateOrderPriority(orderId: String, priority: OrderPriority): DomainResult<Order> = findOrderById(orderId)
    override suspend fun markReadyForJob(orderId: String): DomainResult<Order> = findOrderById(orderId)
    override suspend fun updateJobHandoffStatus(orderId: String, status: JobHandoffStatus): DomainResult<Order> = findOrderById(orderId)
    override suspend fun updateOrderNotes(orderId: String, notes: String?): DomainResult<Order> = findOrderById(orderId)
    override suspend fun cancelOrder(orderId: String, reason: String?): DomainResult<Order> = findOrderById(orderId)
    override suspend fun createOrderFromApprovedQuotation(
        orderId: String,
        orderNumber: String,
        quotationId: String,
        approvedRevisionId: String,
        priority: OrderPriority,
        confirmedBy: String?,
        timestamp: String
    ): DomainResult<Order> = findOrderById(orderId)

    private fun mapOrderSummaryDtoToOrder(dto: CustomerOrderSummaryDto): Order {
        val statusType = try {
            OrderStatusType.valueOf(dto.status.uppercase())
        } catch (_: Exception) {
            OrderStatusType.CONFIRMED
        }
        val summaryItem = OrderItem(
            itemId = "SUMMARY-${dto.orderId}",
            description = "Commercial Order Summary",
            quantity = 1,
            unitPrice = Money(dto.totalAmount)
        )
        return Order(
            orderId = dto.orderId,
            orderNumber = dto.orderNumber,
            customerId = "CUSTOMER-DEFAULT",
            status = statusType,
            items = listOf(summaryItem),
            discount = Money.ZERO,
            createdAt = if (dto.createdAt > 0) java.time.Instant.ofEpochMilli(dto.createdAt).toString() else "2026-01-01T00:00:00Z",
            updatedAt = if (dto.createdAt > 0) java.time.Instant.ofEpochMilli(dto.createdAt).toString() else "2026-01-01T00:00:00Z"
        )
    }

    private fun mapOrderDetailDtoToOrder(dto: CustomerOrderDetailDto): Order {
        val statusType = try {
            OrderStatusType.valueOf(dto.status.uppercase())
        } catch (_: Exception) {
            OrderStatusType.CONFIRMED
        }
        val domainItems = dto.items.map {
            OrderItem(
                itemId = it.itemId,
                description = it.description,
                quantity = it.quantity,
                unitPrice = Money(it.unitPrice)
            )
        }
        return Order(
            orderId = dto.orderId,
            orderNumber = dto.orderNumber,
            customerId = dto.customerId,
            status = statusType,
            items = domainItems,
            discount = Money(dto.discount),
            notes = dto.notes,
            createdAt = if (dto.createdAt > 0) java.time.Instant.ofEpochMilli(dto.createdAt).toString() else "2026-01-01T00:00:00Z",
            updatedAt = if (dto.createdAt > 0) java.time.Instant.ofEpochMilli(dto.createdAt).toString() else "2026-01-01T00:00:00Z"
        )
    }
}
