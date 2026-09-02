package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttempt
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttemptStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEvent
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEventType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentSummary
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Shipment Tracking & Delivery Status (Module 08 Step 05).
 */
interface DeliveryShipmentRepository {

    // Queries
    fun observeShipments(projectId: String): Flow<List<DeliveryShipment>>
    fun observeShipmentsForDispatch(dispatchExecutionId: String): Flow<List<DeliveryShipment>>
    fun observeShipment(shipmentId: String): Flow<DeliveryShipment?>
    suspend fun getShipment(
        shipmentId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipment>

    suspend fun getShipmentByNo(
        projectId: String,
        shipmentNo: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipment>

    suspend fun getShipmentsForDispatch(
        dispatchExecutionId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryShipment>>

    fun observeTrackingEvents(shipmentId: String): Flow<List<DeliveryShipmentEvent>>
    suspend fun getTrackingEvents(
        shipmentId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryShipmentEvent>>

    fun observeDeliveryAttempts(shipmentId: String): Flow<List<DeliveryShipmentAttempt>>
    suspend fun getDeliveryAttempts(
        shipmentId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryShipmentAttempt>>

    fun observeActivityEvents(shipmentId: String): Flow<List<DeliveryShipmentActivityEvent>>
    suspend fun getActivityEvents(
        shipmentId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryShipmentActivityEvent>>

    suspend fun getShipmentSummary(
        projectId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipmentSummary>

    // Mutations
    suspend fun createShipment(
        shipment: DeliveryShipment,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipment>

    suspend fun updateDraftShipment(
        shipmentId: String,
        carrierName: String?,
        carrierReference: String?,
        trackingNumber: String?,
        destinationAddress: String?,
        destinationContactName: String?,
        destinationContactPhone: String?,
        destinationNotes: String?,
        estimatedDeliveryAt: Long?,
        notes: String?,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipment>

    suspend fun markReady(
        shipmentId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipment>

    suspend fun markDispatched(
        shipmentId: String,
        actualDispatchAt: Long? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipment>

    suspend fun markInTransit(
        shipmentId: String,
        locationText: String? = null,
        note: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipment>

    suspend fun markOutForDelivery(
        shipmentId: String,
        locationText: String? = null,
        note: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipment>

    suspend fun markDelayed(
        shipmentId: String,
        reason: String,
        locationText: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipment>

    suspend fun putOnHold(
        shipmentId: String,
        reason: String,
        locationText: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipment>

    suspend fun recordDeliveryAttempt(
        shipmentId: String,
        status: DeliveryShipmentAttemptStatus,
        reason: String? = null,
        notes: String? = null,
        attemptedAt: Long? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipmentAttempt>

    suspend fun markDelivered(
        shipmentId: String,
        actualDeliveryAt: Long? = null,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipment>

    suspend fun cancelShipment(
        shipmentId: String,
        reason: String? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipment>

    suspend fun addTrackingEvent(
        shipmentId: String,
        eventType: DeliveryShipmentEventType,
        locationText: String? = null,
        description: String? = null,
        eventTime: Long? = null,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryShipmentEvent>
}
