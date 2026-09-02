package com.sucharu.sucharupro.domain.model.notification

/**
 * Domain categories and types of business notifications (Module 10 Step 01).
 */
enum class NotificationType(val defaultLabel: String, val category: NotificationCategory, val isMandatory: Boolean = false) {
    // Orders
    ORDER_CREATED("Order Created", NotificationCategory.ORDER),
    ORDER_STATUS_CHANGED("Order Status Changed", NotificationCategory.ORDER),

    // Design / Artwork
    DESIGN_SUBMITTED("Design Submitted", NotificationCategory.DESIGN),
    DESIGN_APPROVAL_REQUIRED("Design Approval Required", NotificationCategory.DESIGN),
    DESIGN_APPROVED("Design Approved", NotificationCategory.DESIGN),
    DESIGN_REJECTED("Design Rejected", NotificationCategory.DESIGN),

    // Production
    PRODUCTION_STARTED("Production Started", NotificationCategory.PRODUCTION),
    PRODUCTION_STAGE_CHANGED("Production Stage Changed", NotificationCategory.PRODUCTION),
    PRODUCTION_COMPLETED("Production Completed", NotificationCategory.PRODUCTION),

    // Quality
    QUALITY_CHECK_REQUIRED("Quality Check Required", NotificationCategory.QUALITY),
    QUALITY_APPROVED("Quality Approved", NotificationCategory.QUALITY),
    QUALITY_REJECTED("Quality Rejected", NotificationCategory.QUALITY),

    // Delivery / Logistics
    DELIVERY_CREATED("Delivery Challan Created", NotificationCategory.DELIVERY),
    DELIVERY_DISPATCHED("Delivery Dispatched", NotificationCategory.DELIVERY),
    DELIVERY_DELIVERED("Delivery Completed", NotificationCategory.DELIVERY),
    DELIVERY_REJECTED("Delivery Rejected", NotificationCategory.DELIVERY),
    DELIVERY_RETURNED("Delivery Returned", NotificationCategory.DELIVERY),

    // Inventory / Stock
    STOCK_LOW("Low Stock Alert", NotificationCategory.INVENTORY),
    STOCK_OUT("Out of Stock Alert", NotificationCategory.INVENTORY),
    STOCK_RECEIVED("Stock Received", NotificationCategory.INVENTORY),

    // Financial / Customer Payments
    PAYMENT_RECEIVED("Payment Received", NotificationCategory.FINANCE),
    PAYMENT_DUE("Payment Due Reminder", NotificationCategory.FINANCE),
    PAYMENT_OVERDUE("Payment Overdue Alert", NotificationCategory.FINANCE),

    // Supplier Payments & Expenses
    SUPPLIER_PAYMENT_CREATED("Supplier Payment Created", NotificationCategory.FINANCE),
    SUPPLIER_PAYMENT_COMPLETED("Supplier Payment Completed", NotificationCategory.FINANCE),
    EXPENSE_CREATED("Expense Created", NotificationCategory.FINANCE),
    EXPENSE_APPROVAL_REQUIRED("Expense Approval Required", NotificationCategory.FINANCE),

    // Financial Governance & Alerts
    FINANCIAL_ALERT("Financial Alert", NotificationCategory.FINANCE, isMandatory = true),
    FINANCIAL_RECONCILIATION_ALERT("Financial Reconciliation Alert", NotificationCategory.FINANCE, isMandatory = true),

    // System & Security
    SYSTEM_ALERT("System Alert", NotificationCategory.SYSTEM, isMandatory = true),
    SECURITY_ALERT("Security Alert", NotificationCategory.SYSTEM, isMandatory = true),

    // General
    GENERAL("General Announcement", NotificationCategory.GENERAL)
}

enum class NotificationCategory(val defaultLabel: String) {
    ORDER("Orders"),
    DESIGN("Design & Artwork"),
    PRODUCTION("Production"),
    QUALITY("Quality Control"),
    DELIVERY("Delivery & Logistics"),
    INVENTORY("Inventory & Stock"),
    FINANCE("Financial & Payments"),
    SYSTEM("System & Security"),
    GENERAL("General")
}
