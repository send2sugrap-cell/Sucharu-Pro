package com.sucharu.sucharupro.domain.model.communication.vendor

import com.sucharu.sucharupro.domain.model.notification.NotificationPriority

/**
 * Domain policy mapping upstream business events to VendorCommunication triggers (Module 10 Step 05).
 *
 * IMPORTANT: This policy creates communication requests ONLY.
 * It does NOT mutate VendorPayable, SupplierPayment, Inventory, Delivery, or any source business record.
 */
object VendorCommunicationPolicy {

    data class CommunicationDecision(
        val shouldCommunicate: Boolean,
        val communicationType: VendorCommunicationType,
        val defaultPriority: NotificationPriority,
        val templateCode: String,
        val requiresAcknowledgement: Boolean
    )

    fun evaluateEvent(
        eventType: String,
        isMajorUpdate: Boolean = true
    ): CommunicationDecision {
        return when (eventType) {
            "PAYABLE_CREATED" -> CommunicationDecision(
                shouldCommunicate = isMajorUpdate,
                communicationType = VendorCommunicationType.PAYABLE_UPDATE,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "VENDOR_PAYABLE_CREATED",
                requiresAcknowledgement = true
            )
            "SUPPLIER_PAYMENT_POSTED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = VendorCommunicationType.PAYMENT_STATUS,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "VENDOR_PAYMENT_STATUS",
                requiresAcknowledgement = false
            )
            "SUPPLIER_PAYMENT_COMPLETED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = VendorCommunicationType.PAYMENT_RECEIVED,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "VENDOR_PAYMENT_RECEIVED",
                requiresAcknowledgement = false
            )
            "PAYMENT_DUE" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = VendorCommunicationType.PAYMENT_DUE,
                defaultPriority = NotificationPriority.HIGH,
                templateCode = "VENDOR_PAYMENT_DUE",
                requiresAcknowledgement = false
            )
            "PAYMENT_OVERDUE" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = VendorCommunicationType.PAYMENT_OVERDUE,
                defaultPriority = NotificationPriority.URGENT,
                templateCode = "VENDOR_PAYMENT_OVERDUE",
                requiresAcknowledgement = true
            )
            "PURCHASE_UPDATED" -> CommunicationDecision(
                shouldCommunicate = isMajorUpdate,
                communicationType = VendorCommunicationType.PURCHASE_UPDATE,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "VENDOR_PURCHASE_UPDATE",
                requiresAcknowledgement = false
            )
            "RECEIVING_COMPLETED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = VendorCommunicationType.RECEIVING_UPDATE,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "VENDOR_RECEIVING_UPDATE",
                requiresAcknowledgement = true
            )
            "QUALITY_REJECTION" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = VendorCommunicationType.QUALITY_REJECTION,
                defaultPriority = NotificationPriority.HIGH,
                templateCode = "VENDOR_QUALITY_REJECTION",
                requiresAcknowledgement = true
            )
            "RETURN_INITIATED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = VendorCommunicationType.RETURN_UPDATE,
                defaultPriority = NotificationPriority.HIGH,
                templateCode = "VENDOR_RETURN_INITIATED",
                requiresAcknowledgement = true
            )
            "REPLACEMENT_INITIATED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = VendorCommunicationType.REPLACEMENT_UPDATE,
                defaultPriority = NotificationPriority.HIGH,
                templateCode = "VENDOR_REPLACEMENT_INITIATED",
                requiresAcknowledgement = true
            )
            "DOCUMENT_REQUIRED" -> CommunicationDecision(
                shouldCommunicate = true,
                communicationType = VendorCommunicationType.DOCUMENT_REQUEST,
                defaultPriority = NotificationPriority.HIGH,
                templateCode = "VENDOR_DOCUMENT_REQUEST",
                requiresAcknowledgement = true
            )
            else -> CommunicationDecision(
                shouldCommunicate = false,
                communicationType = VendorCommunicationType.GENERAL_MESSAGE,
                defaultPriority = NotificationPriority.NORMAL,
                templateCode = "VENDOR_GENERAL_NOTIFICATION",
                requiresAcknowledgement = false
            )
        }
    }
}
