package com.sucharu.sucharupro.domain.model.communication.internal

import com.sucharu.sucharupro.domain.model.notification.NotificationType

/**
 * Internal communication categories and topics (Module 10 Step 03).
 */
enum class InternalCommunicationType(
    val defaultLabel: String,
    val canonicalNotificationType: NotificationType
) {
    DIRECT_MESSAGE("Direct Message", NotificationType.GENERAL),
    TEAM_MESSAGE("Team Message", NotificationType.GENERAL),
    DEPARTMENT_MESSAGE("Department Message", NotificationType.GENERAL),
    ROLE_ANNOUNCEMENT("Role Announcement", NotificationType.GENERAL),
    GENERAL_ANNOUNCEMENT("General Announcement", NotificationType.GENERAL),
    JOB_DISCUSSION("Job Discussion", NotificationType.PRODUCTION_STAGE_CHANGED),
    ORDER_DISCUSSION("Order Discussion", NotificationType.ORDER_STATUS_CHANGED),
    PRODUCTION_DISCUSSION("Production Discussion", NotificationType.PRODUCTION_STAGE_CHANGED),
    DESIGN_DISCUSSION("Design Discussion", NotificationType.DESIGN_SUBMITTED),
    QC_DISCUSSION("Quality Check Discussion", NotificationType.QUALITY_REJECTED),
    DELIVERY_DISCUSSION("Delivery Discussion", NotificationType.DELIVERY_DISPATCHED),
    FINANCE_DISCUSSION("Finance Discussion", NotificationType.PAYMENT_DUE),
    INVENTORY_DISCUSSION("Inventory Discussion", NotificationType.STOCK_LOW),
    TASK_MESSAGE("Task Assignment / Update", NotificationType.GENERAL),
    MENTION("User Mention", NotificationType.GENERAL),
    SYSTEM_NOTICE("Internal System Notice", NotificationType.SYSTEM_ALERT),
    URGENT_NOTICE("Urgent Notice", NotificationType.SECURITY_ALERT)
}

/**
 * Lifecycle states of an internal communication (Module 10 Step 03).
 */
enum class InternalCommunicationStatus(val defaultLabel: String, val isTerminal: Boolean = false) {
    DRAFT("Draft", false),
    SCHEDULED("Scheduled", false),
    QUEUED("Queued", false),
    SENT("Sent", false),
    DELIVERED("Delivered", false),
    READ("Read", false),
    ACKNOWLEDGED("Acknowledged", false),
    FAILED("Failed", false),
    CANCELLED("Cancelled", true),
    ARCHIVED("Archived", true)
}

/**
 * Priority levels for internal communications (Module 10 Step 03).
 */
enum class InternalCommunicationPriority(val defaultLabel: String, val level: Int) {
    LOW("Low", 1),
    NORMAL("Normal", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4),
    CRITICAL("Critical", 5)
}

/**
 * Target recipient scope for internal communications (Module 10 Step 03).
 */
enum class InternalCommunicationRecipientType(val defaultLabel: String) {
    USER("Specific User"),
    ROLE("Role Based"),
    TEAM("Team Based"),
    DEPARTMENT("Department Based"),
    PROJECT("Entire Project"),
    ALL_INTERNAL_USERS("All Internal Users")
}
