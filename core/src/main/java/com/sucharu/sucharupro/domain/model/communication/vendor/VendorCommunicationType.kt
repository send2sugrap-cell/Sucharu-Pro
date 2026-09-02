package com.sucharu.sucharupro.domain.model.communication.vendor

import com.sucharu.sucharupro.domain.model.notification.NotificationType

/**
 * Business-semantic communication categories for Vendor & Supplier communications (Module 10 Step 05).
 *
 * Distinct from [NotificationType] (which represents delivery classification).
 * Each type maps to the canonical delivery [NotificationType] in Module 10 Step 01.
 */
enum class VendorCommunicationType(
    val defaultLabel: String,
    val canonicalNotificationType: NotificationType,
    val requiresAcknowledgement: Boolean = false
) {
    // Purchase & Supply
    PURCHASE_UPDATE("Purchase Order Update", NotificationType.ORDER_STATUS_CHANGED),
    SUPPLY_REQUEST("Supply Request", NotificationType.ORDER_CREATED),
    SUPPLY_CONFIRMATION("Supply Confirmation", NotificationType.ORDER_STATUS_CHANGED),

    // Bill & Payable
    PURCHASE_BILL_UPDATE("Purchase Bill Update", NotificationType.SUPPLIER_PAYMENT_CREATED),
    PAYABLE_UPDATE("Payable Update", NotificationType.SUPPLIER_PAYMENT_CREATED, requiresAcknowledgement = true),

    // Payment
    PAYMENT_RECEIVED("Payment Received Confirmation", NotificationType.SUPPLIER_PAYMENT_COMPLETED),
    PAYMENT_DUE("Payment Due Reminder", NotificationType.PAYMENT_DUE),
    PAYMENT_OVERDUE("Payment Overdue Notice", NotificationType.PAYMENT_OVERDUE, requiresAcknowledgement = true),
    PAYMENT_STATUS("Payment Status Update", NotificationType.SUPPLIER_PAYMENT_COMPLETED),

    // Delivery & Receiving
    DELIVERY_UPDATE("Delivery Status Update", NotificationType.DELIVERY_DISPATCHED),
    RECEIVING_UPDATE("Goods Receiving Update", NotificationType.STOCK_RECEIVED, requiresAcknowledgement = true),

    // Quality
    QUALITY_UPDATE("Quality Check Update", NotificationType.QUALITY_CHECK_REQUIRED),
    QUALITY_REJECTION("Quality Rejection Notice", NotificationType.QUALITY_REJECTED, requiresAcknowledgement = true),

    // Return & Replacement
    RETURN_UPDATE("Return & Rejection Update", NotificationType.DELIVERY_RETURNED, requiresAcknowledgement = true),
    REPLACEMENT_UPDATE("Replacement Request", NotificationType.ORDER_STATUS_CHANGED, requiresAcknowledgement = true),

    // Documents
    DOCUMENT_REQUEST("Document Request", NotificationType.GENERAL, requiresAcknowledgement = true),
    DOCUMENT_RECEIVED("Document Received Confirmation", NotificationType.GENERAL),

    // Account & Admin
    VENDOR_ACCOUNT_UPDATE("Vendor Account Update", NotificationType.GENERAL, requiresAcknowledgement = true),
    VENDOR_NOTICE("Vendor Notice", NotificationType.GENERAL, requiresAcknowledgement = true),

    // Announcements
    SERVICE_ANNOUNCEMENT("Service Announcement", NotificationType.GENERAL),
    IMPORTANT_NOTICE("Important Business Notice", NotificationType.SYSTEM_ALERT, requiresAcknowledgement = true),

    // General
    GENERAL_MESSAGE("General Message", NotificationType.GENERAL),
    SYSTEM_NOTIFICATION("System Notification", NotificationType.SYSTEM_ALERT, requiresAcknowledgement = true)
}
