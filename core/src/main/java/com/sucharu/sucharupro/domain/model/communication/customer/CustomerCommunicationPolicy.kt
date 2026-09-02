package com.sucharu.sucharupro.domain.model.communication.customer

import com.sucharu.sucharupro.domain.model.notification.NotificationPriority

/**
 * Domain policy mapping upstream business events to appropriate CustomerCommunication triggers (Module 10 Step 02).
 */
object CustomerCommunicationPolicy {

    data class CommunicationDecision(
        val shouldCommunicate: Boolean,
        val communicationType: CustomerCommunicationType,
        val defaultPriority: NotificationPriority,
        val templateCode: String,
        val requiresAcknowledgement: Boolean
    )

    fun evaluateEvent(
        eventType: String,
        isMajorUpdate: Boolean = true
    ): CommunicationDecision {
        return when (eventType) {
            "ORDER_CREATED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = CustomerCommunicationType.ORDER_UPDATE,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "ORDER_CONFIRMATION",
                requiresAcknowledgement = false
            )
            "ORDER_STATUS_CHANGED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = CustomerCommunicationType.ORDER_UPDATE,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "ORDER_STATUS_UPDATE",
                requiresAcknowledgement = false
            )
            "DESIGN_APPROVAL_REQUIRED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = CustomerCommunicationType.APPROVAL_REQUEST,
                defaultPriority = NotificationPriority.HIGH,
                templateCode = "DESIGN_APPROVAL_REQUEST",
                requiresAcknowledgement = true
            )
            "DESIGN_APPROVED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = CustomerCommunicationType.DESIGN_UPDATE,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "DESIGN_APPROVED_NOTICE",
                requiresAcknowledgement = false
            )
            "PRODUCTION_STARTED" -> CommunicationDecision(
                shouldCommunicate = isMajorUpdate,
                communicationType = CustomerCommunicationType.PRODUCTION_UPDATE,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "PRODUCTION_STARTED_NOTICE",
                requiresAcknowledgement = false
            )
            "PRODUCTION_COMPLETED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = CustomerCommunicationType.PRODUCTION_UPDATE,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "PRODUCTION_COMPLETED_NOTICE",
                requiresAcknowledgement = false
            )
            "DELIVERY_DISPATCHED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = CustomerCommunicationType.DELIVERY_UPDATE,
                defaultPriority = NotificationPriority.HIGH,
                templateCode = "DELIVERY_DISPATCHED_NOTICE",
                requiresAcknowledgement = false
            )
            "DELIVERY_DELIVERED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = CustomerCommunicationType.DELIVERY_UPDATE,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "DELIVERY_DELIVERED_NOTICE",
                requiresAcknowledgement = false
            )
            "PAYMENT_RECEIVED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = CustomerCommunicationType.PAYMENT_RECEIVED,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "PAYMENT_RECEIPT_NOTICE",
                requiresAcknowledgement = false
            )
            "PAYMENT_DUE" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = CustomerCommunicationType.PAYMENT_DUE,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "PAYMENT_DUE_REMINDER",
                requiresAcknowledgement = false
            )
            "PAYMENT_OVERDUE" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = CustomerCommunicationType.PAYMENT_OVERDUE,
                defaultPriority = NotificationPriority.URGENT,
                templateCode = "PAYMENT_OVERDUE_NOTICE",
                requiresAcknowledgement = true
            )
            else -> CommunicationDecision(
                shouldCommunicate = false,
                communicationType = CustomerCommunicationType.GENERAL_MESSAGE,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "GENERAL_NOTIFICATION",
                requiresAcknowledgement = false
            )
        }
    }
}
