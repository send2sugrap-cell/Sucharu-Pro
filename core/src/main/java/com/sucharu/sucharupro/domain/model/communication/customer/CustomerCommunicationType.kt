package com.sucharu.sucharupro.domain.model.communication.customer

import com.sucharu.sucharupro.domain.model.notification.NotificationType

/**
 * Business communication categories for customer communications (Module 10 Step 02).
 *
 * Maps seamlessly to canonical [NotificationType] in Module 10 Step 01.
 */
enum class CustomerCommunicationType(
    val defaultLabel: String,
    val canonicalNotificationType: NotificationType,
    val requiresAcknowledgement: Boolean = false
) {
    ORDER_UPDATE("Order Update", NotificationType.ORDER_STATUS_CHANGED),
    DESIGN_UPDATE("Design Update", NotificationType.DESIGN_SUBMITTED),
    APPROVAL_REQUEST("Design Approval Request", NotificationType.DESIGN_APPROVAL_REQUIRED, requiresAcknowledgement = true),
    PRODUCTION_UPDATE("Production Status", NotificationType.PRODUCTION_STAGE_CHANGED),
    QUALITY_UPDATE("Quality Check Update", NotificationType.QUALITY_APPROVED),
    DELIVERY_UPDATE("Delivery Update", NotificationType.DELIVERY_DISPATCHED),

    PAYMENT_RECEIVED("Payment Receipt", NotificationType.PAYMENT_RECEIVED),
    PAYMENT_DUE("Payment Due Reminder", NotificationType.PAYMENT_DUE),
    PAYMENT_OVERDUE("Payment Overdue Notice", NotificationType.PAYMENT_OVERDUE),

    SERVICE_ANNOUNCEMENT("Service Announcement", NotificationType.GENERAL),
    IMPORTANT_NOTICE("Important Business Notice", NotificationType.GENERAL, requiresAcknowledgement = true),

    OFFER("Special Offer", NotificationType.GENERAL),
    PROMOTION("Promotional Campaign", NotificationType.GENERAL),

    ACCOUNT_UPDATE("Account Update", NotificationType.GENERAL),
    GENERAL_MESSAGE("Customer Message", NotificationType.GENERAL),

    SYSTEM_NOTIFICATION("System Notice", NotificationType.SYSTEM_ALERT, requiresAcknowledgement = true)
}
