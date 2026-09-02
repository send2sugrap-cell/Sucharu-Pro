package com.sucharu.sucharupro.domain.event.model

/**
 * Top-level domain event categories in the Sucharu Pro ecosystem.
 */
enum class EventCategory {
    ORDER,
    PRODUCTION,
    QC,
    INVENTORY,
    DELIVERY,
    RETURN,
    FINANCE,
    CUSTOMER,
    AFFILIATE,
    AUTHENTICATION,
    SECURITY,
    NOTIFICATION,
    SYSTEM
}

/**
 * Strongly typed canonical domain event types for Sucharu Pro (INFRA-04 Step 01).
 *
 * Each event type maps to an authoritative [EventCategory] and represents an immutable business fact.
 */
enum class DomainEventType(
    val category: EventCategory,
    val typeName: String,
    val currentVersion: String = "v1"
) {
    // Order events
    ORDER_CREATED(EventCategory.ORDER, "OrderCreated"),
    ORDER_UPDATED(EventCategory.ORDER, "OrderUpdated"),
    ORDER_CANCELLED(EventCategory.ORDER, "OrderCancelled"),

    // Production events
    PRODUCTION_STARTED(EventCategory.PRODUCTION, "ProductionStarted"),
    PRODUCTION_COMPLETED(EventCategory.PRODUCTION, "ProductionCompleted"),

    // QC events
    QC_PASSED(EventCategory.QC, "QcPassed"),
    QC_FAILED(EventCategory.QC, "QcFailed"),

    // Inventory events
    STOCK_RECEIVED(EventCategory.INVENTORY, "StockReceived"),
    STOCK_ISSUED(EventCategory.INVENTORY, "StockIssued"),
    STOCK_ADJUSTED(EventCategory.INVENTORY, "StockAdjusted"),

    // Delivery events
    DELIVERY_CREATED(EventCategory.DELIVERY, "DeliveryCreated"),
    DELIVERY_DISPATCHED(EventCategory.DELIVERY, "DeliveryDispatched"),
    DELIVERY_DELIVERED(EventCategory.DELIVERY, "DeliveryDelivered"),

    // Return events
    RETURN_REQUESTED(EventCategory.RETURN, "ReturnRequested"),
    RETURN_INSPECTED(EventCategory.RETURN, "ReturnInspected"),
    RETURN_APPROVED(EventCategory.RETURN, "ReturnApproved"),
    RETURN_REJECTED(EventCategory.RETURN, "ReturnRejected"),

    // Finance events
    INVOICE_CREATED(EventCategory.FINANCE, "InvoiceCreated"),
    PAYMENT_RECEIVED(EventCategory.FINANCE, "PaymentReceived"),
    PAYMENT_REFUNDED(EventCategory.FINANCE, "PaymentRefunded"),

    // Customer events
    CUSTOMER_REGISTERED(EventCategory.CUSTOMER, "CustomerRegistered"),
    CUSTOMER_VERIFIED(EventCategory.CUSTOMER, "CustomerVerified"),

    // Affiliate events
    AFFILIATE_REFERRAL_CREATED(EventCategory.AFFILIATE, "AffiliateReferralCreated"),
    AFFILIATE_COMMISSION_GENERATED(EventCategory.AFFILIATE, "AffiliateCommissionGenerated"),

    // Security & Authentication events
    AUTH_SUCCEEDED(EventCategory.AUTHENTICATION, "AuthenticationSucceeded"),
    AUTH_FAILED(EventCategory.SECURITY, "AuthenticationFailed"),
    SESSION_CREATED(EventCategory.AUTHENTICATION, "SessionCreated"),
    SESSION_REVOKED(EventCategory.SECURITY, "SessionRevoked"),
    AUTHZ_DENIED(EventCategory.SECURITY, "AuthorizationDenied"),
    ACCOUNT_LOCKED(EventCategory.SECURITY, "AccountLocked"),
    PASSWORD_CHANGED(EventCategory.SECURITY, "PasswordChanged"),

    // System events
    SYSTEM_MAINTENANCE_SCHEDULED(EventCategory.SYSTEM, "SystemMaintenanceScheduled"),
    SYSTEM_ALERT(EventCategory.SYSTEM, "SystemAlert"),

    // Notification Security Events (INFRA-04 Step 07)
    NOTIFICATION_AUTHORIZATION_DENIED(EventCategory.NOTIFICATION, "NotificationAuthorizationDenied"),
    NOTIFICATION_SUPPRESSED(EventCategory.NOTIFICATION, "NotificationSuppressed"),
    NOTIFICATION_RATE_LIMIT_TRIGGERED(EventCategory.NOTIFICATION, "NotificationRateLimitTriggered"),
    NOTIFICATION_ABUSE_DETECTED(EventCategory.NOTIFICATION, "NotificationAbuseDetected"),
    NOTIFICATION_REPLAY_DENIED(EventCategory.NOTIFICATION, "NotificationReplayDenied"),
    NOTIFICATION_PROVIDER_SECURITY_FAILURE(EventCategory.NOTIFICATION, "NotificationProviderSecurityFailure");

    /**
     * Canonical formatted event signature string, e.g. "OrderCreated:v1".
     */
    val versionedName: String get() = "$typeName:$currentVersion"
}
