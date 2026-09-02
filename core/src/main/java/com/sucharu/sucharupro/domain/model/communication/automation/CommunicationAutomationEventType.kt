package com.sucharu.sucharupro.domain.model.communication.automation

import com.sucharu.sucharupro.domain.model.notification.NotificationCategory
import com.sucharu.sucharupro.domain.model.notification.NotificationType

/**
 * Extensible business event types for Communication Automation (Module 10 Step 08).
 */
enum class CommunicationAutomationEventType(
    val defaultLabel: String,
    val category: NotificationCategory,
    val canonicalNotificationType: NotificationType
) {
    // Orders
    ORDER_CREATED("Order Created", NotificationCategory.ORDER, NotificationType.ORDER_CREATED),
    ORDER_STATUS_CHANGED("Order Status Changed", NotificationCategory.ORDER, NotificationType.ORDER_STATUS_CHANGED),

    // Design
    DESIGN_APPROVAL_REQUIRED("Design Approval Required", NotificationCategory.DESIGN, NotificationType.DESIGN_APPROVAL_REQUIRED),
    DESIGN_APPROVED("Design Approved", NotificationCategory.DESIGN, NotificationType.DESIGN_APPROVED),
    DESIGN_REJECTED("Design Rejected", NotificationCategory.DESIGN, NotificationType.DESIGN_REJECTED),

    // Production & QC
    PRODUCTION_STAGE_CHANGED("Production Stage Changed", NotificationCategory.PRODUCTION, NotificationType.PRODUCTION_STAGE_CHANGED),
    QC_PASSED("Quality Check Passed", NotificationCategory.QUALITY, NotificationType.QUALITY_APPROVED),
    QC_FAILED("Quality Check Failed", NotificationCategory.QUALITY, NotificationType.QUALITY_REJECTED),

    // Delivery & Logistics
    DELIVERY_SCHEDULED("Delivery Scheduled", NotificationCategory.DELIVERY, NotificationType.DELIVERY_CREATED),
    DELIVERY_DELAYED("Delivery Delayed Alert", NotificationCategory.DELIVERY, NotificationType.DELIVERY_DISPATCHED),
    DELIVERY_COMPLETED("Delivery Completed", NotificationCategory.DELIVERY, NotificationType.DELIVERY_DELIVERED),

    // Financial / Payments
    PAYMENT_RECEIVED("Payment Received", NotificationCategory.FINANCE, NotificationType.PAYMENT_RECEIVED),
    PAYMENT_DUE("Payment Due Reminder", NotificationCategory.FINANCE, NotificationType.PAYMENT_DUE),
    PAYMENT_OVERDUE("Payment Overdue Escalation", NotificationCategory.FINANCE, NotificationType.PAYMENT_OVERDUE),

    // Vendor Compliance
    VENDOR_DOCUMENT_EXPIRING("Vendor Document Expiring Soon", NotificationCategory.GENERAL, NotificationType.GENERAL),
    VENDOR_DOCUMENT_EXPIRED("Vendor Document Expired Alert", NotificationCategory.GENERAL, NotificationType.GENERAL),

    // Campaigns & System
    CAMPAIGN_SCHEDULED("Campaign Scheduled", NotificationCategory.GENERAL, NotificationType.GENERAL),
    CAMPAIGN_PUBLISHED("Campaign Published", NotificationCategory.GENERAL, NotificationType.GENERAL),
    APPROVAL_PENDING("Administrative Approval Pending", NotificationCategory.SYSTEM, NotificationType.SYSTEM_ALERT),
    SYSTEM_ALERT("System Critical Alert", NotificationCategory.SYSTEM, NotificationType.SYSTEM_ALERT),
    GENERAL_EVENT("General Business Event", NotificationCategory.GENERAL, NotificationType.GENERAL)
}
