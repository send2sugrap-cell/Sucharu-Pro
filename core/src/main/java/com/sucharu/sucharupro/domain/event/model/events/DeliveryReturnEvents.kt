package com.sucharu.sucharupro.domain.event.model.events

import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType

/**
 * Emitted when a delivery challan is created.
 */
data class DeliveryCreatedEvent(
    val challanId: String,
    val orderId: String,
    val customerId: String,
    val deliveryAddress: String,
    val totalPackages: Int,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.DELIVERY_CREATED
    override val aggregateId: String get() = challanId
    override val aggregateType: String get() = "DELIVERY"

    init {
        require(challanId.isNotBlank()) { "challanId cannot be blank" }
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
        require(customerId.isNotBlank()) { "customerId cannot be blank" }
        require(totalPackages > 0) { "totalPackages must be positive" }
    }
}

/**
 * Emitted when delivery is dispatched from facility.
 */
data class DeliveryDispatchedEvent(
    val challanId: String,
    val orderId: String,
    val carrierName: String,
    val trackingNumber: String? = null,
    override val aggregateVersion: Long
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.DELIVERY_DISPATCHED
    override val aggregateId: String get() = challanId
    override val aggregateType: String get() = "DELIVERY"

    init {
        require(challanId.isNotBlank()) { "challanId cannot be blank" }
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
        require(carrierName.isNotBlank()) { "carrierName cannot be blank" }
    }
}

/**
 * Emitted when delivery has been successfully delivered and acknowledged.
 */
data class DeliveryDeliveredEvent(
    val challanId: String,
    val orderId: String,
    val deliveredToPerson: String,
    val deliveredTimestamp: Long = System.currentTimeMillis(),
    override val aggregateVersion: Long
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.DELIVERY_DELIVERED
    override val aggregateId: String get() = challanId
    override val aggregateType: String get() = "DELIVERY"

    init {
        require(challanId.isNotBlank()) { "challanId cannot be blank" }
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
        require(deliveredToPerson.isNotBlank()) { "deliveredToPerson cannot be blank" }
    }
}

/**
 * Emitted when a customer requests a return or replacement.
 */
data class ReturnRequestedEvent(
    val returnRequestId: String,
    val orderId: String,
    val customerId: String,
    val reason: String,
    val requestedItemCount: Int,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.RETURN_REQUESTED
    override val aggregateId: String get() = returnRequestId
    override val aggregateType: String get() = "RETURN"

    init {
        require(returnRequestId.isNotBlank()) { "returnRequestId cannot be blank" }
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
        require(customerId.isNotBlank()) { "customerId cannot be blank" }
        require(reason.isNotBlank()) { "reason cannot be blank" }
        require(requestedItemCount > 0) { "requestedItemCount must be positive" }
    }
}

/**
 * Emitted when returned items have been physically inspected by QC.
 */
data class ReturnInspectedEvent(
    val returnRequestId: String,
    val inspectedBy: String,
    val inspectionNotes: String,
    val restockableCount: Int,
    val damagedCount: Int,
    override val aggregateVersion: Long
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.RETURN_INSPECTED
    override val aggregateId: String get() = returnRequestId
    override val aggregateType: String get() = "RETURN"

    init {
        require(returnRequestId.isNotBlank()) { "returnRequestId cannot be blank" }
        require(inspectedBy.isNotBlank()) { "inspectedBy cannot be blank" }
    }
}

/**
 * Emitted when a return request is approved by a manager.
 */
data class ReturnApprovedEvent(
    val returnRequestId: String,
    val orderId: String,
    val approvedBy: String,
    val refundActionRequired: Boolean,
    override val aggregateVersion: Long
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.RETURN_APPROVED
    override val aggregateId: String get() = returnRequestId
    override val aggregateType: String get() = "RETURN"

    init {
        require(returnRequestId.isNotBlank()) { "returnRequestId cannot be blank" }
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
        require(approvedBy.isNotBlank()) { "approvedBy cannot be blank" }
    }
}

/**
 * Emitted when a return request is rejected.
 */
data class ReturnRejectedEvent(
    val returnRequestId: String,
    val orderId: String,
    val rejectedBy: String,
    val rejectionReason: String,
    override val aggregateVersion: Long
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.RETURN_REJECTED
    override val aggregateId: String get() = returnRequestId
    override val aggregateType: String get() = "RETURN"

    init {
        require(returnRequestId.isNotBlank()) { "returnRequestId cannot be blank" }
        require(orderId.isNotBlank()) { "orderId cannot be blank" }
        require(rejectionReason.isNotBlank()) { "rejectionReason cannot be blank" }
    }
}
