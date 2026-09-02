package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryShipmentDataSource
import com.sucharu.sucharupro.data.datasource.DispatchExecutionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipment
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentActivityType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttempt
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentAttemptStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEvent
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentEventType
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentSummary
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryShipmentRepository
import com.sucharu.sucharupro.domain.validation.DeliveryShipmentAttemptValidator
import com.sucharu.sucharupro.domain.validation.DeliveryShipmentAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.DeliveryShipmentLifecycleValidator
import com.sucharu.sucharupro.domain.validation.DeliveryShipmentOperation
import com.sucharu.sucharupro.domain.validation.DeliveryShipmentValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production implementation of [DeliveryShipmentRepository] (Module 08 Step 05).
 */
class DeliveryShipmentRepositoryImpl(
    private val shipmentDataSource: DeliveryShipmentDataSource,
    private val dispatchDataSource: DispatchExecutionDataSource,
    private val verificationDataSource: DeliveryItemVerificationDataSource? = null,
    private val challanDataSource: DeliveryChallanDataSource? = null,
    private val deliveryOrderDataSource: DeliveryOrderDataSource? = null
) : DeliveryShipmentRepository {

    private val mutex = Mutex()

    // ──────────────────────────────────────────────────────────────
    // Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeShipments(projectId: String): Flow<List<DeliveryShipment>> {
        return shipmentDataSource.observeShipments(projectId)
    }

    override fun observeShipmentsForDispatch(dispatchExecutionId: String): Flow<List<DeliveryShipment>> {
        return shipmentDataSource.observeShipmentsForDispatch(dispatchExecutionId)
    }

    override fun observeShipment(shipmentId: String): Flow<DeliveryShipment?> {
        return shipmentDataSource.observeShipment(shipmentId)
    }

    override suspend fun getShipment(
        shipmentId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipment> {
        val shipment = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.VIEW,
                targetProjectId = shipment.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        return DomainResult.Success(shipment)
    }

    override suspend fun getShipmentByNo(
        projectId: String,
        shipmentNo: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipment> {
        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.VIEW,
                targetProjectId = projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val shipment = shipmentDataSource.getShipmentByNo(projectId, shipmentNo)
            ?: return DomainResult.Error(message = "Shipment number '$shipmentNo' not found in project '$projectId'.")

        return DomainResult.Success(shipment)
    }

    override suspend fun getShipmentsForDispatch(
        dispatchExecutionId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryShipment>> {
        val dispatch = dispatchDataSource.getDispatch(dispatchExecutionId)
            ?: return DomainResult.Error(message = "Dispatch execution '$dispatchExecutionId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.VIEW,
                targetProjectId = dispatch.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val list = shipmentDataSource.getShipmentsForDispatch(dispatchExecutionId)
        return DomainResult.Success(list)
    }

    override fun observeTrackingEvents(shipmentId: String): Flow<List<DeliveryShipmentEvent>> {
        return shipmentDataSource.observeTrackingEvents(shipmentId)
    }

    override suspend fun getTrackingEvents(
        shipmentId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryShipmentEvent>> {
        val shipment = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.VIEW,
                targetProjectId = shipment.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val events = shipmentDataSource.getTrackingEvents(shipmentId)
        return DomainResult.Success(events)
    }

    override fun observeDeliveryAttempts(shipmentId: String): Flow<List<DeliveryShipmentAttempt>> {
        return shipmentDataSource.observeDeliveryAttempts(shipmentId)
    }

    override suspend fun getDeliveryAttempts(
        shipmentId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryShipmentAttempt>> {
        val shipment = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.VIEW,
                targetProjectId = shipment.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val attempts = shipmentDataSource.getDeliveryAttempts(shipmentId)
        return DomainResult.Success(attempts)
    }

    override fun observeActivityEvents(shipmentId: String): Flow<List<DeliveryShipmentActivityEvent>> {
        return shipmentDataSource.observeActivityEvents(shipmentId)
    }

    override suspend fun getActivityEvents(
        shipmentId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryShipmentActivityEvent>> {
        val shipment = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.VIEW,
                targetProjectId = shipment.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val events = shipmentDataSource.getActivityEvents(shipmentId)
        return DomainResult.Success(events)
    }

    override suspend fun getShipmentSummary(
        projectId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipmentSummary> {
        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.VIEW,
                targetProjectId = projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val shipments = shipmentDataSource.observeShipments(projectId).first()
        var totalAttempts = 0
        for (s in shipments) {
            totalAttempts += shipmentDataSource.getDeliveryAttempts(s.shipmentId).size
        }

        val summary = DeliveryShipmentSummary(
            totalShipments = shipments.size,
            draftCount = shipments.count { it.currentStatus == DeliveryShipmentStatus.DRAFT },
            readyCount = shipments.count { it.currentStatus == DeliveryShipmentStatus.READY },
            dispatchedCount = shipments.count { it.currentStatus == DeliveryShipmentStatus.DISPATCHED },
            inTransitCount = shipments.count { it.currentStatus == DeliveryShipmentStatus.IN_TRANSIT },
            outForDeliveryCount = shipments.count { it.currentStatus == DeliveryShipmentStatus.OUT_FOR_DELIVERY },
            deliveryAttemptedCount = shipments.count { it.currentStatus == DeliveryShipmentStatus.DELIVERY_ATTEMPTED },
            delayedCount = shipments.count { it.currentStatus == DeliveryShipmentStatus.DELAYED },
            onHoldCount = shipments.count { it.currentStatus == DeliveryShipmentStatus.ON_HOLD },
            deliveredCount = shipments.count { it.currentStatus == DeliveryShipmentStatus.DELIVERED },
            cancelledCount = shipments.count { it.currentStatus == DeliveryShipmentStatus.CANCELLED },
            totalAttempts = totalAttempts
        )

        return DomainResult.Success(summary)
    }

    // ──────────────────────────────────────────────────────────────
    // Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun createShipment(
        shipment: DeliveryShipment,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipment> = mutex.withLock {
        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.CREATE,
                targetProjectId = shipment.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Structural validation
        val validationResult = DeliveryShipmentValidator.validateShipment(shipment)
        if (validationResult is DomainResult.Error) return validationResult

        // 3. Unique shipment number check
        val existingNo = shipmentDataSource.getShipmentByNo(shipment.projectId, shipment.shipmentNo)
        if (existingNo != null) {
            return DomainResult.Error(
                message = "Shipment number '${shipment.shipmentNo}' already exists in project '${shipment.projectId}'."
            )
        }

        // 4. Unique tracking number check
        if (!shipment.trackingNumber.isNullOrBlank()) {
            val existingTracking = shipmentDataSource.getShipmentByTrackingNumber(shipment.projectId, shipment.trackingNumber)
            if (existingTracking != null) {
                return DomainResult.Error(
                    message = "Tracking number '${shipment.trackingNumber}' already exists in project '${shipment.projectId}'."
                )
            }
        }

        // 5. Dispatch eligibility check
        val dispatch = dispatchDataSource.getDispatch(shipment.dispatchExecutionId)
            ?: return DomainResult.Error(message = "Referenced Dispatch Execution '${shipment.dispatchExecutionId}' not found.")

        val dispatchEligibility = DeliveryShipmentValidator.validateDispatchEligibility(
            dispatch = dispatch,
            targetProjectId = shipment.projectId,
            deliveryOrderId = shipment.deliveryOrderId,
            deliveryChallanId = shipment.deliveryChallanId
        )
        if (dispatchEligibility is DomainResult.Error) return dispatchEligibility

        // 6. Verification reference validation (if specified)
        if (shipment.verificationId != null && verificationDataSource != null) {
            val verif = verificationDataSource.getVerification(shipment.verificationId)
            if (verif == null) {
                return DomainResult.Error(message = "Referenced Delivery Verification '${shipment.verificationId}' not found.")
            }
            if (verif.projectId != shipment.projectId) {
                return DomainResult.Error(message = "Referenced Delivery Verification belongs to project '${verif.projectId}', not '${shipment.projectId}'.")
            }
        }

        // 7. Persist shipment
        shipmentDataSource.insertShipment(shipment)

        // 8. Tracking event
        val trackingEvent = DeliveryShipmentEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = shipment.projectId,
            shipmentId = shipment.shipmentId,
            eventType = DeliveryShipmentEventType.CREATED,
            status = shipment.currentStatus,
            eventTime = shipment.createdAt,
            description = "Shipment created for dispatch '${dispatch.dispatchNo}'.",
            actorId = shipment.createdBy,
            createdAt = shipment.createdAt
        )
        shipmentDataSource.insertTrackingEvent(trackingEvent)

        // 9. Activity audit
        val activity = DeliveryShipmentActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = shipment.projectId,
            shipmentId = shipment.shipmentId,
            activityType = DeliveryShipmentActivityType.CREATED,
            performedBy = shipment.createdBy,
            performedAt = shipment.createdAt,
            newStatus = shipment.currentStatus.name,
            details = "Shipment '${shipment.shipmentNo}' created for dispatch '${dispatch.dispatchNo}'."
        )
        shipmentDataSource.insertActivityEvent(activity)

        DomainResult.Success(shipment)
    }

    override suspend fun updateDraftShipment(
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
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipment> = mutex.withLock {
        val existing = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.EDIT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        if (existing.currentStatus != DeliveryShipmentStatus.DRAFT && existing.currentStatus != DeliveryShipmentStatus.READY) {
            return DomainResult.Error(
                message = "Shipment details can only be edited in DRAFT or READY status (Current status: '${existing.currentStatus.defaultLabel}')."
            )
        }

        if (!trackingNumber.isNullOrBlank() && trackingNumber != existing.trackingNumber) {
            val existingTracking = shipmentDataSource.getShipmentByTrackingNumber(existing.projectId, trackingNumber)
            if (existingTracking != null && existingTracking.shipmentId != existing.shipmentId) {
                return DomainResult.Error(
                    message = "Tracking number '$trackingNumber' already exists in project '${existing.projectId}'."
                )
            }
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            carrierName = carrierName ?: existing.carrierName,
            carrierReference = carrierReference ?: existing.carrierReference,
            trackingNumber = trackingNumber ?: existing.trackingNumber,
            destinationAddress = destinationAddress ?: existing.destinationAddress,
            destinationContactName = destinationContactName ?: existing.destinationContactName,
            destinationContactPhone = destinationContactPhone ?: existing.destinationContactPhone,
            destinationNotes = destinationNotes ?: existing.destinationNotes,
            estimatedDeliveryAt = estimatedDeliveryAt ?: existing.estimatedDeliveryAt,
            notes = notes ?: existing.notes,
            updatedBy = actorId,
            updatedAt = now
        )

        val validationResult = DeliveryShipmentValidator.validateShipment(updated)
        if (validationResult is DomainResult.Error) return validationResult

        val immutabilityCheck = DeliveryShipmentValidator.validateImmutableIdentity(existing, updated)
        if (immutabilityCheck is DomainResult.Error) return immutabilityCheck

        shipmentDataSource.updateShipment(updated)

        val activity = DeliveryShipmentActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            activityType = DeliveryShipmentActivityType.UPDATED,
            performedBy = actorId,
            performedAt = now,
            details = "Shipment details updated."
        )
        shipmentDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun markReady(
        shipmentId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipment> = mutex.withLock {
        val existing = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.MARK_READY,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryShipmentLifecycleValidator.validateTransition(
            currentStatus = existing.currentStatus,
            targetStatus = DeliveryShipmentStatus.READY
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            currentStatus = DeliveryShipmentStatus.READY,
            updatedBy = actorId,
            updatedAt = now
        )
        shipmentDataSource.updateShipment(updated)

        val trackingEvent = DeliveryShipmentEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            eventType = DeliveryShipmentEventType.READY,
            status = DeliveryShipmentStatus.READY,
            eventTime = now,
            description = "Shipment marked ready for carrier pickup/dispatch.",
            actorId = actorId,
            createdAt = now
        )
        shipmentDataSource.insertTrackingEvent(trackingEvent)

        val activity = DeliveryShipmentActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            activityType = DeliveryShipmentActivityType.READY,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.currentStatus.name,
            newStatus = DeliveryShipmentStatus.READY.name,
            details = "Shipment marked ready."
        )
        shipmentDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun markDispatched(
        shipmentId: String,
        actualDispatchAt: Long?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipment> = mutex.withLock {
        val existing = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.MARK_DISPATCHED,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryShipmentLifecycleValidator.validateTransition(
            currentStatus = existing.currentStatus,
            targetStatus = DeliveryShipmentStatus.DISPATCHED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val dispatchTime = actualDispatchAt ?: now
        val updated = existing.copy(
            currentStatus = DeliveryShipmentStatus.DISPATCHED,
            actualDispatchAt = dispatchTime,
            updatedBy = actorId,
            updatedAt = now
        )
        shipmentDataSource.updateShipment(updated)

        val trackingEvent = DeliveryShipmentEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            eventType = DeliveryShipmentEventType.DISPATCHED,
            status = DeliveryShipmentStatus.DISPATCHED,
            eventTime = dispatchTime,
            description = "Shipment departed facility.",
            actorId = actorId,
            createdAt = now
        )
        shipmentDataSource.insertTrackingEvent(trackingEvent)

        val activity = DeliveryShipmentActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            activityType = DeliveryShipmentActivityType.DISPATCHED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.currentStatus.name,
            newStatus = DeliveryShipmentStatus.DISPATCHED.name,
            details = "Shipment dispatched from origin."
        )
        shipmentDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun markInTransit(
        shipmentId: String,
        locationText: String?,
        note: String?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipment> = mutex.withLock {
        val existing = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.UPDATE_STATUS,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryShipmentLifecycleValidator.validateTransition(
            currentStatus = existing.currentStatus,
            targetStatus = DeliveryShipmentStatus.IN_TRANSIT
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            currentStatus = DeliveryShipmentStatus.IN_TRANSIT,
            updatedBy = actorId,
            updatedAt = now
        )
        shipmentDataSource.updateShipment(updated)

        val trackingEvent = DeliveryShipmentEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            eventType = DeliveryShipmentEventType.IN_TRANSIT,
            status = DeliveryShipmentStatus.IN_TRANSIT,
            eventTime = now,
            locationText = locationText,
            description = note ?: "Shipment is in transit.",
            actorId = actorId,
            createdAt = now
        )
        shipmentDataSource.insertTrackingEvent(trackingEvent)

        val activity = DeliveryShipmentActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            activityType = DeliveryShipmentActivityType.IN_TRANSIT,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.currentStatus.name,
            newStatus = DeliveryShipmentStatus.IN_TRANSIT.name,
            details = note ?: "Shipment moved to IN_TRANSIT."
        )
        shipmentDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun markOutForDelivery(
        shipmentId: String,
        locationText: String?,
        note: String?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipment> = mutex.withLock {
        val existing = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.UPDATE_STATUS,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryShipmentLifecycleValidator.validateTransition(
            currentStatus = existing.currentStatus,
            targetStatus = DeliveryShipmentStatus.OUT_FOR_DELIVERY
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            currentStatus = DeliveryShipmentStatus.OUT_FOR_DELIVERY,
            updatedBy = actorId,
            updatedAt = now
        )
        shipmentDataSource.updateShipment(updated)

        val trackingEvent = DeliveryShipmentEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            eventType = DeliveryShipmentEventType.OUT_FOR_DELIVERY,
            status = DeliveryShipmentStatus.OUT_FOR_DELIVERY,
            eventTime = now,
            locationText = locationText,
            description = note ?: "Shipment is out for delivery with local courier.",
            actorId = actorId,
            createdAt = now
        )
        shipmentDataSource.insertTrackingEvent(trackingEvent)

        val activity = DeliveryShipmentActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            activityType = DeliveryShipmentActivityType.OUT_FOR_DELIVERY,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.currentStatus.name,
            newStatus = DeliveryShipmentStatus.OUT_FOR_DELIVERY.name,
            details = note ?: "Shipment marked OUT_FOR_DELIVERY."
        )
        shipmentDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun markDelayed(
        shipmentId: String,
        reason: String,
        locationText: String?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipment> = mutex.withLock {
        val existing = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.UPDATE_STATUS,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryShipmentLifecycleValidator.validateTransition(
            currentStatus = existing.currentStatus,
            targetStatus = DeliveryShipmentStatus.DELAYED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            currentStatus = DeliveryShipmentStatus.DELAYED,
            updatedBy = actorId,
            updatedAt = now
        )
        shipmentDataSource.updateShipment(updated)

        val trackingEvent = DeliveryShipmentEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            eventType = DeliveryShipmentEventType.DELAYED,
            status = DeliveryShipmentStatus.DELAYED,
            eventTime = now,
            locationText = locationText,
            description = "Shipment delayed: $reason",
            actorId = actorId,
            createdAt = now
        )
        shipmentDataSource.insertTrackingEvent(trackingEvent)

        val activity = DeliveryShipmentActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            activityType = DeliveryShipmentActivityType.DELAYED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.currentStatus.name,
            newStatus = DeliveryShipmentStatus.DELAYED.name,
            details = "Shipment delayed: $reason"
        )
        shipmentDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun putOnHold(
        shipmentId: String,
        reason: String,
        locationText: String?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipment> = mutex.withLock {
        val existing = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.UPDATE_STATUS,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryShipmentLifecycleValidator.validateTransition(
            currentStatus = existing.currentStatus,
            targetStatus = DeliveryShipmentStatus.ON_HOLD
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            currentStatus = DeliveryShipmentStatus.ON_HOLD,
            updatedBy = actorId,
            updatedAt = now
        )
        shipmentDataSource.updateShipment(updated)

        val trackingEvent = DeliveryShipmentEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            eventType = DeliveryShipmentEventType.ON_HOLD,
            status = DeliveryShipmentStatus.ON_HOLD,
            eventTime = now,
            locationText = locationText,
            description = "Shipment placed on hold: $reason",
            actorId = actorId,
            createdAt = now
        )
        shipmentDataSource.insertTrackingEvent(trackingEvent)

        val activity = DeliveryShipmentActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            activityType = DeliveryShipmentActivityType.ON_HOLD,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.currentStatus.name,
            newStatus = DeliveryShipmentStatus.ON_HOLD.name,
            details = "Shipment placed on hold: $reason"
        )
        shipmentDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun recordDeliveryAttempt(
        shipmentId: String,
        status: DeliveryShipmentAttemptStatus,
        reason: String?,
        notes: String?,
        attemptedAt: Long?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipmentAttempt> = mutex.withLock {
        val existing = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.RECORD_ATTEMPT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val existingAttempts = shipmentDataSource.getDeliveryAttempts(shipmentId)
        val nextAttemptNo = (existingAttempts.maxOfOrNull { it.attemptNo } ?: 0) + 1
        val now = System.currentTimeMillis()
        val attemptTimestamp = attemptedAt ?: now

        val attempt = DeliveryShipmentAttempt(
            attemptId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            attemptNo = nextAttemptNo,
            attemptedAt = attemptTimestamp,
            status = status,
            reason = reason,
            notes = notes,
            createdBy = actorId,
            createdAt = now
        )

        val validation = DeliveryShipmentAttemptValidator.validateAttempt(existing, attempt, existingAttempts)
        if (validation is DomainResult.Error) return validation

        shipmentDataSource.insertDeliveryAttempt(attempt)

        // Update shipment status if successful or failed attempt
        val targetStatus = if (status == DeliveryShipmentAttemptStatus.SUCCESSFUL) {
            DeliveryShipmentStatus.DELIVERED
        } else {
            DeliveryShipmentStatus.DELIVERY_ATTEMPTED
        }

        val updatedShipment = existing.copy(
            currentStatus = targetStatus,
            actualDeliveryAt = if (status == DeliveryShipmentAttemptStatus.SUCCESSFUL) attemptTimestamp else existing.actualDeliveryAt,
            updatedBy = actorId,
            updatedAt = now
        )
        shipmentDataSource.updateShipment(updatedShipment)

        val trackingEvent = DeliveryShipmentEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            eventType = if (status == DeliveryShipmentAttemptStatus.SUCCESSFUL) DeliveryShipmentEventType.DELIVERED else DeliveryShipmentEventType.DELIVERY_ATTEMPTED,
            status = targetStatus,
            eventTime = attemptTimestamp,
            description = "Delivery Attempt #$nextAttemptNo - ${status.defaultLabel}${if (!reason.isNullOrBlank()) ": $reason" else ""}",
            actorId = actorId,
            createdAt = now
        )
        shipmentDataSource.insertTrackingEvent(trackingEvent)

        val activity = DeliveryShipmentActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            activityType = DeliveryShipmentActivityType.ATTEMPT_RECORDED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.currentStatus.name,
            newStatus = targetStatus.name,
            details = "Recorded attempt #$nextAttemptNo (${status.name})."
        )
        shipmentDataSource.insertActivityEvent(activity)

        DomainResult.Success(attempt)
    }

    override suspend fun markDelivered(
        shipmentId: String,
        actualDeliveryAt: Long?,
        notes: String?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipment> = mutex.withLock {
        val existing = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.UPDATE_STATUS,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryShipmentLifecycleValidator.validateTransition(
            currentStatus = existing.currentStatus,
            targetStatus = DeliveryShipmentStatus.DELIVERED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val deliveryTime = actualDeliveryAt ?: now
        val updated = existing.copy(
            currentStatus = DeliveryShipmentStatus.DELIVERED,
            actualDeliveryAt = deliveryTime,
            updatedBy = actorId,
            updatedAt = now
        )
        shipmentDataSource.updateShipment(updated)

        val trackingEvent = DeliveryShipmentEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            eventType = DeliveryShipmentEventType.DELIVERED,
            status = DeliveryShipmentStatus.DELIVERED,
            eventTime = deliveryTime,
            description = notes ?: "Shipment delivered.",
            actorId = actorId,
            createdAt = now
        )
        shipmentDataSource.insertTrackingEvent(trackingEvent)

        val activity = DeliveryShipmentActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            activityType = DeliveryShipmentActivityType.DELIVERED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.currentStatus.name,
            newStatus = DeliveryShipmentStatus.DELIVERED.name,
            details = "Shipment marked delivered."
        )
        shipmentDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun cancelShipment(
        shipmentId: String,
        reason: String?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipment> = mutex.withLock {
        val existing = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.CANCEL,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryShipmentLifecycleValidator.validateTransition(
            currentStatus = existing.currentStatus,
            targetStatus = DeliveryShipmentStatus.CANCELLED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            currentStatus = DeliveryShipmentStatus.CANCELLED,
            updatedBy = actorId,
            updatedAt = now
        )
        shipmentDataSource.updateShipment(updated)

        val trackingEvent = DeliveryShipmentEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            eventType = DeliveryShipmentEventType.CANCELLED,
            status = DeliveryShipmentStatus.CANCELLED,
            eventTime = now,
            description = reason ?: "Shipment cancelled.",
            actorId = actorId,
            createdAt = now
        )
        shipmentDataSource.insertTrackingEvent(trackingEvent)

        val activity = DeliveryShipmentActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            activityType = DeliveryShipmentActivityType.CANCELLED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.currentStatus.name,
            newStatus = DeliveryShipmentStatus.CANCELLED.name,
            details = reason ?: "Shipment cancelled."
        )
        shipmentDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun addTrackingEvent(
        shipmentId: String,
        eventType: DeliveryShipmentEventType,
        locationText: String?,
        description: String?,
        eventTime: Long?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryShipmentEvent> = mutex.withLock {
        val existing = shipmentDataSource.getShipment(shipmentId)
            ?: return DomainResult.Error(message = "Shipment '$shipmentId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryShipmentAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryShipmentOperation.ADD_EVENT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val now = System.currentTimeMillis()
        val time = eventTime ?: now

        val event = DeliveryShipmentEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            eventType = eventType,
            status = existing.currentStatus,
            eventTime = time,
            locationText = locationText,
            description = description,
            actorId = actorId,
            createdAt = now
        )

        shipmentDataSource.insertTrackingEvent(event)

        val activity = DeliveryShipmentActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            shipmentId = existing.shipmentId,
            activityType = DeliveryShipmentActivityType.TRACKING_EVENT_ADDED,
            performedBy = actorId,
            performedAt = now,
            details = "Added tracking event '${eventType.defaultLabel}'${if (locationText != null) " at '$locationText'" else ""}."
        )
        shipmentDataSource.insertActivityEvent(activity)

        DomainResult.Success(event)
    }
}
