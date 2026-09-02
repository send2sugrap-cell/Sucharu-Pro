package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttempt
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEvent
import kotlinx.coroutines.flow.Flow

/**
 * Reactive data source interface for Delivery Shipments (Module 08 Step 05).
 */
interface DeliveryShipmentDataSource {
    fun observeShipments(projectId: String): Flow<List<DeliveryShipment>>
    fun observeShipmentsForDispatch(dispatchExecutionId: String): Flow<List<DeliveryShipment>>
    fun observeShipment(shipmentId: String): Flow<DeliveryShipment?>
    suspend fun getShipment(shipmentId: String): DeliveryShipment?
    suspend fun getShipmentByNo(projectId: String, shipmentNo: String): DeliveryShipment?
    suspend fun getShipmentByTrackingNumber(projectId: String, trackingNumber: String): DeliveryShipment?
    suspend fun getShipmentsForDispatch(dispatchExecutionId: String): List<DeliveryShipment>
    suspend fun insertShipment(shipment: DeliveryShipment)
    suspend fun updateShipment(shipment: DeliveryShipment)

    fun observeTrackingEvents(shipmentId: String): Flow<List<DeliveryShipmentEvent>>
    suspend fun getTrackingEvents(shipmentId: String): List<DeliveryShipmentEvent>
    suspend fun insertTrackingEvent(event: DeliveryShipmentEvent)

    fun observeDeliveryAttempts(shipmentId: String): Flow<List<DeliveryShipmentAttempt>>
    suspend fun getDeliveryAttempts(shipmentId: String): List<DeliveryShipmentAttempt>
    suspend fun insertDeliveryAttempt(attempt: DeliveryShipmentAttempt)

    fun observeActivityEvents(shipmentId: String): Flow<List<DeliveryShipmentActivityEvent>>
    suspend fun getActivityEvents(shipmentId: String): List<DeliveryShipmentActivityEvent>
    suspend fun insertActivityEvent(event: DeliveryShipmentActivityEvent)
}
