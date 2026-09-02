package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnShipment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory data source for Delivery Returns (Module 08 Step 07).
 */
class FakeDeliveryReturnDataSource : DeliveryReturnDataSource {

    private val mutex = Mutex()
    private val returnsFlow = MutableStateFlow<Map<String, DeliveryReturn>>(emptyMap())
    private val linesFlow = MutableStateFlow<Map<String, DeliveryReturnLine>>(emptyMap())
    private val eventsFlow = MutableStateFlow<List<DeliveryReturnActivityEvent>>(emptyList())
    private val reverseShipmentsFlow = MutableStateFlow<Map<String, DeliveryReturnShipment>>(emptyMap())

    override fun observeReturns(projectId: String): Flow<List<DeliveryReturn>> {
        return returnsFlow.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeReturn(returnId: String): Flow<DeliveryReturn?> {
        return returnsFlow.map { it[returnId] }
    }

    override suspend fun getReturn(returnId: String): DeliveryReturn? = mutex.withLock {
        returnsFlow.value[returnId]
    }

    override suspend fun getReturnByNumber(projectId: String, returnNo: String): DeliveryReturn? = mutex.withLock {
        returnsFlow.value.values.find { it.projectId == projectId && it.returnNo == returnNo }
    }

    override suspend fun getReturnsByDeliveryOrder(deliveryOrderId: String): List<DeliveryReturn> = mutex.withLock {
        returnsFlow.value.values.filter { it.deliveryOrderId == deliveryOrderId }
    }

    override suspend fun getReturnsByShipment(shipmentId: String): List<DeliveryReturn> = mutex.withLock {
        returnsFlow.value.values.filter { it.shipmentId == shipmentId }
    }

    override suspend fun getReturnsByCustomer(projectId: String, customerId: String): List<DeliveryReturn> = mutex.withLock {
        returnsFlow.value.values.filter { it.projectId == projectId && it.customerId == customerId }
    }

    override suspend fun getReturnLines(returnId: String): List<DeliveryReturnLine> = mutex.withLock {
        linesFlow.value.values.filter { it.returnId == returnId }
    }

    override suspend fun getReturnLine(returnLineId: String): DeliveryReturnLine? = mutex.withLock {
        linesFlow.value[returnLineId]
    }

    override suspend fun insertReturn(ret: DeliveryReturn, lines: List<DeliveryReturnLine>) = mutex.withLock {
        returnsFlow.update { it + (ret.returnId to ret) }
        linesFlow.update { current ->
            current + lines.associateBy { it.returnLineId }
        }
    }

    override suspend fun updateReturn(ret: DeliveryReturn) = mutex.withLock {
        returnsFlow.update { it + (ret.returnId to ret) }
    }

    override suspend fun updateReturnLine(line: DeliveryReturnLine) = mutex.withLock {
        linesFlow.update { it + (line.returnLineId to line) }
    }

    override suspend fun removeReturnLine(returnLineId: String) = mutex.withLock {
        linesFlow.update { it - returnLineId }
    }

    override fun observeEvents(returnId: String): Flow<List<DeliveryReturnActivityEvent>> {
        return eventsFlow.map { list ->
            list.filter { it.returnId == returnId }.sortedBy { it.timestamp }
        }
    }

    override suspend fun insertEvent(event: DeliveryReturnActivityEvent) = mutex.withLock {
        eventsFlow.update { it + event }
    }

    override fun observeReverseShipment(returnId: String): Flow<DeliveryReturnShipment?> {
        return reverseShipmentsFlow.map { it.values.find { s -> s.returnId == returnId } }
    }

    override suspend fun getReverseShipment(returnId: String): DeliveryReturnShipment? = mutex.withLock {
        reverseShipmentsFlow.value.values.find { it.returnId == returnId }
    }

    override suspend fun getReverseShipmentById(reverseShipmentId: String): DeliveryReturnShipment? = mutex.withLock {
        reverseShipmentsFlow.value[reverseShipmentId]
    }

    override suspend fun insertReverseShipment(shipment: DeliveryReturnShipment) = mutex.withLock {
        reverseShipmentsFlow.update { it + (shipment.reverseShipmentId to shipment) }
    }

    override suspend fun updateReverseShipment(shipment: DeliveryReturnShipment) = mutex.withLock {
        reverseShipmentsFlow.update { it + (shipment.reverseShipmentId to shipment) }
    }
}
