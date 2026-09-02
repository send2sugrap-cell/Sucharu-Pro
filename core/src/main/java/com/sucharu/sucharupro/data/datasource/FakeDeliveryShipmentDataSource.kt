package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttempt
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory fake data source for Delivery Shipments (Module 08 Step 05).
 */
class FakeDeliveryShipmentDataSource : DeliveryShipmentDataSource {

    private val mutex = Mutex()
    private val shipmentsFlow = MutableStateFlow<Map<String, DeliveryShipment>>(emptyMap())
    private val trackingEventsFlow = MutableStateFlow<Map<String, List<DeliveryShipmentEvent>>>(emptyMap())
    private val attemptsFlow = MutableStateFlow<Map<String, List<DeliveryShipmentAttempt>>>(emptyMap())
    private val activityEventsFlow = MutableStateFlow<Map<String, List<DeliveryShipmentActivityEvent>>>(emptyMap())

    override fun observeShipments(projectId: String): Flow<List<DeliveryShipment>> {
        return shipmentsFlow.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeShipmentsForDispatch(dispatchExecutionId: String): Flow<List<DeliveryShipment>> {
        return shipmentsFlow.map { map ->
            map.values.filter { it.dispatchExecutionId == dispatchExecutionId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeShipment(shipmentId: String): Flow<DeliveryShipment?> {
        return shipmentsFlow.map { it[shipmentId] }
    }

    override suspend fun getShipment(shipmentId: String): DeliveryShipment? = mutex.withLock {
        shipmentsFlow.value[shipmentId]
    }

    override suspend fun getShipmentByNo(projectId: String, shipmentNo: String): DeliveryShipment? = mutex.withLock {
        shipmentsFlow.value.values.firstOrNull { it.projectId == projectId && it.shipmentNo.equals(shipmentNo, ignoreCase = true) }
    }

    override suspend fun getShipmentByTrackingNumber(projectId: String, trackingNumber: String): DeliveryShipment? = mutex.withLock {
        shipmentsFlow.value.values.firstOrNull {
            it.projectId == projectId && it.trackingNumber != null && it.trackingNumber.equals(trackingNumber, ignoreCase = true)
        }
    }

    override suspend fun getShipmentsForDispatch(dispatchExecutionId: String): List<DeliveryShipment> = mutex.withLock {
        shipmentsFlow.value.values.filter { it.dispatchExecutionId == dispatchExecutionId }.sortedByDescending { it.createdAt }
    }

    override suspend fun insertShipment(shipment: DeliveryShipment) = mutex.withLock {
        shipmentsFlow.update { it + (shipment.shipmentId to shipment) }
    }

    override suspend fun updateShipment(shipment: DeliveryShipment) = mutex.withLock {
        shipmentsFlow.update { it + (shipment.shipmentId to shipment) }
    }

    override fun observeTrackingEvents(shipmentId: String): Flow<List<DeliveryShipmentEvent>> {
        return trackingEventsFlow.map { it[shipmentId] ?: emptyList() }
    }

    override suspend fun getTrackingEvents(shipmentId: String): List<DeliveryShipmentEvent> = mutex.withLock {
        trackingEventsFlow.value[shipmentId] ?: emptyList()
    }

    override suspend fun insertTrackingEvent(event: DeliveryShipmentEvent) = mutex.withLock {
        trackingEventsFlow.update { current ->
            val list = current[event.shipmentId]?.toMutableList() ?: mutableListOf()
            list.add(event)
            current + (event.shipmentId to list)
        }
    }

    override fun observeDeliveryAttempts(shipmentId: String): Flow<List<DeliveryShipmentAttempt>> {
        return attemptsFlow.map { it[shipmentId] ?: emptyList() }
    }

    override suspend fun getDeliveryAttempts(shipmentId: String): List<DeliveryShipmentAttempt> = mutex.withLock {
        attemptsFlow.value[shipmentId] ?: emptyList()
    }

    override suspend fun insertDeliveryAttempt(attempt: DeliveryShipmentAttempt) = mutex.withLock {
        attemptsFlow.update { current ->
            val list = current[attempt.shipmentId]?.toMutableList() ?: mutableListOf()
            list.add(attempt)
            current + (attempt.shipmentId to list)
        }
    }

    override fun observeActivityEvents(shipmentId: String): Flow<List<DeliveryShipmentActivityEvent>> {
        return activityEventsFlow.map { it[shipmentId] ?: emptyList() }
    }

    override suspend fun getActivityEvents(shipmentId: String): List<DeliveryShipmentActivityEvent> = mutex.withLock {
        activityEventsFlow.value[shipmentId] ?: emptyList()
    }

    override suspend fun insertActivityEvent(event: DeliveryShipmentActivityEvent) = mutex.withLock {
        activityEventsFlow.update { current ->
            val list = current[event.shipmentId]?.toMutableList() ?: mutableListOf()
            list.add(event)
            current + (event.shipmentId to list)
        }
    }
}
