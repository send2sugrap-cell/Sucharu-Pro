package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.DeliveryActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.DeliveryDispatchRequest
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake implementation of [DeliveryOrderDataSource] (Module 08 Step 01).
 */
class FakeDeliveryOrderDataSource : DeliveryOrderDataSource {

    private val mutex = Mutex()

    private val ordersFlow = MutableStateFlow<List<DeliveryOrder>>(emptyList())
    private val linesFlow = MutableStateFlow<List<DeliveryOrderLine>>(emptyList())
    private val dispatchRequestsFlow = MutableStateFlow<List<DeliveryDispatchRequest>>(emptyList())
    private val activityEventsFlow = MutableStateFlow<List<DeliveryActivityEvent>>(emptyList())

    // ──────────────────────────────────────────────────────────────
    // Delivery Orders
    // ──────────────────────────────────────────────────────────────

    override fun observeDeliveryOrders(projectId: String): Flow<List<DeliveryOrder>> {
        return ordersFlow.map { list -> list.filter { it.projectId == projectId } }
    }

    override fun observeDeliveryOrder(deliveryOrderId: String): Flow<DeliveryOrder?> {
        return ordersFlow.map { list -> list.find { it.deliveryOrderId == deliveryOrderId } }
    }

    override suspend fun getDeliveryOrder(deliveryOrderId: String): DeliveryOrder? = mutex.withLock {
        ordersFlow.value.find { it.deliveryOrderId == deliveryOrderId }
    }

    override suspend fun getDeliveryOrderByNo(projectId: String, deliveryOrderNo: String): DeliveryOrder? = mutex.withLock {
        ordersFlow.value.find { it.projectId == projectId && it.deliveryOrderNo.equals(deliveryOrderNo, ignoreCase = true) }
    }

    override suspend fun insertDeliveryOrder(order: DeliveryOrder, lines: List<DeliveryOrderLine>): Unit = mutex.withLock {
        val currentOrders = ordersFlow.value.toMutableList()
        currentOrders.add(order)
        ordersFlow.value = currentOrders

        val currentLines = linesFlow.value.toMutableList()
        currentLines.addAll(lines)
        linesFlow.value = currentLines
    }

    override suspend fun updateDeliveryOrder(order: DeliveryOrder): Unit = mutex.withLock {
        val current = ordersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.deliveryOrderId == order.deliveryOrderId }
        if (index != -1) {
            current[index] = order
            ordersFlow.value = current
        }
    }

    override suspend fun updateDeliveryOrderWithLines(
        order: DeliveryOrder,
        lines: List<DeliveryOrderLine>
    ): Unit = mutex.withLock {
        val currentOrders = ordersFlow.value.toMutableList()
        val index = currentOrders.indexOfFirst { it.deliveryOrderId == order.deliveryOrderId }
        if (index != -1) {
            currentOrders[index] = order
            ordersFlow.value = currentOrders
        }

        val currentLines = linesFlow.value.toMutableList()
        currentLines.removeAll { it.deliveryOrderId == order.deliveryOrderId }
        currentLines.addAll(lines)
        linesFlow.value = currentLines
    }

    // ──────────────────────────────────────────────────────────────
    // Delivery Order Lines
    // ──────────────────────────────────────────────────────────────

    override fun observeDeliveryOrderLines(deliveryOrderId: String): Flow<List<DeliveryOrderLine>> {
        return linesFlow.map { list -> list.filter { it.deliveryOrderId == deliveryOrderId } }
    }

    override suspend fun getDeliveryOrderLines(deliveryOrderId: String): List<DeliveryOrderLine> = mutex.withLock {
        linesFlow.value.filter { it.deliveryOrderId == deliveryOrderId }
    }

    override suspend fun getDeliveryOrderLine(lineId: String): DeliveryOrderLine? = mutex.withLock {
        linesFlow.value.find { it.lineId == lineId }
    }

    // ──────────────────────────────────────────────────────────────
    // Dispatch Requests
    // ──────────────────────────────────────────────────────────────

    override fun observeDispatchRequests(projectId: String): Flow<List<DeliveryDispatchRequest>> {
        return dispatchRequestsFlow.map { list -> list.filter { it.projectId == projectId } }
    }

    override fun observeDispatchRequest(dispatchRequestId: String): Flow<DeliveryDispatchRequest?> {
        return dispatchRequestsFlow.map { list -> list.find { it.dispatchRequestId == dispatchRequestId } }
    }

    override suspend fun getDispatchRequest(dispatchRequestId: String): DeliveryDispatchRequest? = mutex.withLock {
        dispatchRequestsFlow.value.find { it.dispatchRequestId == dispatchRequestId }
    }

    override suspend fun getDispatchRequestForOrder(deliveryOrderId: String): DeliveryDispatchRequest? = mutex.withLock {
        dispatchRequestsFlow.value.find { it.deliveryOrderId == deliveryOrderId }
    }

    override suspend fun insertDispatchRequest(request: DeliveryDispatchRequest): Unit = mutex.withLock {
        val current = dispatchRequestsFlow.value.toMutableList()
        current.add(request)
        dispatchRequestsFlow.value = current
    }

    override suspend fun updateDispatchRequest(request: DeliveryDispatchRequest): Unit = mutex.withLock {
        val current = dispatchRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.dispatchRequestId == request.dispatchRequestId }
        if (index != -1) {
            current[index] = request
            dispatchRequestsFlow.value = current
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Activity Events
    // ──────────────────────────────────────────────────────────────

    override fun observeActivityEvents(deliveryOrderId: String): Flow<List<DeliveryActivityEvent>> {
        return activityEventsFlow.map { list ->
            list.filter { it.deliveryOrderId == deliveryOrderId }.sortedByDescending { it.performedAt }
        }
    }

    override suspend fun getActivityEvents(deliveryOrderId: String): List<DeliveryActivityEvent> = mutex.withLock {
        activityEventsFlow.value.filter { it.deliveryOrderId == deliveryOrderId }.sortedByDescending { it.performedAt }
    }

    override suspend fun insertActivityEvent(event: DeliveryActivityEvent): Unit = mutex.withLock {
        val current = activityEventsFlow.value.toMutableList()
        current.add(event)
        activityEventsFlow.value = current
    }
}
