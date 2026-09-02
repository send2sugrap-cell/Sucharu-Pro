package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.DeliveryActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.DeliveryDispatchRequest
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Delivery Orders, Lines, Dispatch Requests, and Activity Events (Module 08 Step 01).
 */
interface DeliveryOrderDataSource {

    // Delivery Orders
    fun observeDeliveryOrders(projectId: String): Flow<List<DeliveryOrder>>
    fun observeDeliveryOrder(deliveryOrderId: String): Flow<DeliveryOrder?>
    suspend fun getDeliveryOrder(deliveryOrderId: String): DeliveryOrder?
    suspend fun getDeliveryOrderByNo(projectId: String, deliveryOrderNo: String): DeliveryOrder?
    suspend fun insertDeliveryOrder(order: DeliveryOrder, lines: List<DeliveryOrderLine>)
    suspend fun updateDeliveryOrder(order: DeliveryOrder)
    suspend fun updateDeliveryOrderWithLines(order: DeliveryOrder, lines: List<DeliveryOrderLine>)

    // Delivery Order Lines
    fun observeDeliveryOrderLines(deliveryOrderId: String): Flow<List<DeliveryOrderLine>>
    suspend fun getDeliveryOrderLines(deliveryOrderId: String): List<DeliveryOrderLine>
    suspend fun getDeliveryOrderLine(lineId: String): DeliveryOrderLine?

    // Dispatch Requests
    fun observeDispatchRequests(projectId: String): Flow<List<DeliveryDispatchRequest>>
    fun observeDispatchRequest(dispatchRequestId: String): Flow<DeliveryDispatchRequest?>
    suspend fun getDispatchRequest(dispatchRequestId: String): DeliveryDispatchRequest?
    suspend fun getDispatchRequestForOrder(deliveryOrderId: String): DeliveryDispatchRequest?
    suspend fun insertDispatchRequest(request: DeliveryDispatchRequest)
    suspend fun updateDispatchRequest(request: DeliveryDispatchRequest)

    // Activity Events
    fun observeActivityEvents(deliveryOrderId: String): Flow<List<DeliveryActivityEvent>>
    suspend fun getActivityEvents(deliveryOrderId: String): List<DeliveryActivityEvent>
    suspend fun insertActivityEvent(event: DeliveryActivityEvent)
}
