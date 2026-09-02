package com.sucharu.sucharupro.data.event.integration.notification

import com.sucharu.sucharupro.domain.event.boundary.NotificationEventBoundary
import com.sucharu.sucharupro.domain.event.boundary.NotificationIntent
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.*

/**
 * Production-grade notification intent resolver mapping domain events into sanitized,
 * human-readable notification intents (INFRA-04 Step 03).
 */
object NotificationIntentResolver {

    /**
     * Translates an event envelope into a structured [NotificationIntent], or returns null
     * if the event is strictly internal or ineligible for notifications.
     */
    fun resolve(envelope: EventEnvelope<*>): NotificationIntent? {
        val payload = envelope.payload

        return when (envelope.eventType) {
            DomainEventType.ORDER_CREATED -> {
                val e = payload as OrderCreatedEvent
                NotificationIntent(
                    eventId = envelope.eventId,
                    eventType = envelope.eventType,
                    projectId = envelope.projectId,
                    targetRecipientId = e.customerId,
                    targetChannels = NotificationEventBoundary.resolveChannelsForEventType(envelope.eventType),
                    title = "Order Confirmed: ${e.orderId}",
                    body = "Your order ${e.orderId} for ${e.currency} ${e.totalAmount} has been confirmed.",
                    correlationId = envelope.correlationId,
                    deepLinkUrl = "sucharu://orders/${e.orderId}"
                )
            }
            DomainEventType.ORDER_UPDATED -> {
                val e = payload as OrderUpdatedEvent
                NotificationIntent(
                    eventId = envelope.eventId,
                    eventType = envelope.eventType,
                    projectId = envelope.projectId,
                    targetRecipientId = e.customerId,
                    targetChannels = NotificationEventBoundary.resolveChannelsForEventType(envelope.eventType),
                    title = "Order Updated: ${e.orderId}",
                    body = "Your order ${e.orderId} has been updated. Reason: ${e.updateReason}",
                    correlationId = envelope.correlationId,
                    deepLinkUrl = "sucharu://orders/${e.orderId}"
                )
            }
            DomainEventType.ORDER_CANCELLED -> {
                val e = payload as OrderCancelledEvent
                NotificationIntent(
                    eventId = envelope.eventId,
                    eventType = envelope.eventType,
                    projectId = envelope.projectId,
                    targetRecipientId = e.customerId,
                    targetChannels = NotificationEventBoundary.resolveChannelsForEventType(envelope.eventType),
                    title = "Order Cancelled: ${e.orderId}",
                    body = "Your order ${e.orderId} was cancelled. Reason: ${e.cancellationReason}",
                    correlationId = envelope.correlationId,
                    deepLinkUrl = "sucharu://orders/${e.orderId}"
                )
            }
            DomainEventType.PRODUCTION_STARTED -> {
                val e = payload as ProductionStartedEvent
                NotificationIntent(
                    eventId = envelope.eventId,
                    eventType = envelope.eventType,
                    projectId = envelope.projectId,
                    targetRecipientId = envelope.actorId,
                    targetChannels = NotificationEventBoundary.resolveChannelsForEventType(envelope.eventType),
                    title = "Production Commenced: ${e.jobId}",
                    body = "Production job ${e.jobId} has started on workstation ${e.workstationId}.",
                    correlationId = envelope.correlationId,
                    deepLinkUrl = "sucharu://production/${e.jobId}"
                )
            }
            DomainEventType.PRODUCTION_COMPLETED -> {
                val e = payload as ProductionCompletedEvent
                NotificationIntent(
                    eventId = envelope.eventId,
                    eventType = envelope.eventType,
                    projectId = envelope.projectId,
                    targetRecipientId = envelope.actorId,
                    targetChannels = NotificationEventBoundary.resolveChannelsForEventType(envelope.eventType),
                    title = "Production Complete: ${e.jobId}",
                    body = "Production job ${e.jobId} completed. Produced: ${e.completedQuantity} units.",
                    correlationId = envelope.correlationId,
                    deepLinkUrl = "sucharu://production/${e.jobId}"
                )
            }
            DomainEventType.DELIVERY_DISPATCHED -> {
                val e = payload as DeliveryDispatchedEvent
                NotificationIntent(
                    eventId = envelope.eventId,
                    eventType = envelope.eventType,
                    projectId = envelope.projectId,
                    targetRecipientId = envelope.actorId,
                    targetChannels = NotificationEventBoundary.resolveChannelsForEventType(envelope.eventType),
                    title = "Delivery Dispatched: ${e.challanId}",
                    body = "Challan ${e.challanId} for order ${e.orderId} was dispatched via ${e.carrierName}.",
                    correlationId = envelope.correlationId,
                    deepLinkUrl = "sucharu://delivery/${e.challanId}"
                )
            }
            DomainEventType.DELIVERY_DELIVERED -> {
                val e = payload as DeliveryDeliveredEvent
                NotificationIntent(
                    eventId = envelope.eventId,
                    eventType = envelope.eventType,
                    projectId = envelope.projectId,
                    targetRecipientId = envelope.actorId,
                    targetChannels = NotificationEventBoundary.resolveChannelsForEventType(envelope.eventType),
                    title = "Delivery Completed: ${e.challanId}",
                    body = "Challan ${e.challanId} was successfully delivered to ${e.deliveredToPerson}.",
                    correlationId = envelope.correlationId,
                    deepLinkUrl = "sucharu://delivery/${e.challanId}"
                )
            }
            DomainEventType.RETURN_REQUESTED -> {
                val e = payload as ReturnRequestedEvent
                NotificationIntent(
                    eventId = envelope.eventId,
                    eventType = envelope.eventType,
                    projectId = envelope.projectId,
                    targetRecipientId = e.customerId,
                    targetChannels = NotificationEventBoundary.resolveChannelsForEventType(envelope.eventType),
                    title = "Return Request Logged: ${e.returnRequestId}",
                    body = "Return request ${e.returnRequestId} for ${e.requestedItemCount} item(s) has been received.",
                    correlationId = envelope.correlationId,
                    deepLinkUrl = "sucharu://returns/${e.returnRequestId}"
                )
            }
            DomainEventType.PAYMENT_RECEIVED -> {
                val e = payload as PaymentReceivedEvent
                NotificationIntent(
                    eventId = envelope.eventId,
                    eventType = envelope.eventType,
                    projectId = envelope.projectId,
                    targetRecipientId = e.customerId,
                    targetChannels = NotificationEventBoundary.resolveChannelsForEventType(envelope.eventType),
                    title = "Payment Received: ${e.currency} ${e.amount}",
                    body = "We received your payment of ${e.currency} ${e.amount} via ${e.paymentMethod}.",
                    correlationId = envelope.correlationId,
                    deepLinkUrl = "sucharu://payments/${e.paymentId}"
                )
            }
            DomainEventType.ACCOUNT_LOCKED -> {
                val e = payload as AccountLockedEvent
                NotificationIntent(
                    eventId = envelope.eventId,
                    eventType = envelope.eventType,
                    projectId = envelope.projectId,
                    targetRecipientId = e.userId,
                    targetChannels = NotificationEventBoundary.resolveChannelsForEventType(envelope.eventType),
                    title = "Security Alert: Account Locked",
                    body = "Your account has been locked for security reasons: ${e.lockReason}.",
                    correlationId = envelope.correlationId
                )
            }
            DomainEventType.PASSWORD_CHANGED -> {
                val e = payload as PasswordChangedEvent
                NotificationIntent(
                    eventId = envelope.eventId,
                    eventType = envelope.eventType,
                    projectId = envelope.projectId,
                    targetRecipientId = e.userId,
                    targetChannels = NotificationEventBoundary.resolveChannelsForEventType(envelope.eventType),
                    title = "Security Alert: Password Changed",
                    body = "Your account password was recently modified.",
                    correlationId = envelope.correlationId
                )
            }
            else -> null // Internal events or events without direct notification requirements
        }
    }
}
