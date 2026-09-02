package com.sucharu.sucharupro.domain.model.inventory.analytics

/**
 * Enumeration of audit event types for inventory analytics and governance (Module 07 Step 10).
 */
enum class InventoryAnalyticsActivityType(val defaultLabel: String) {
    EXCEPTION_LOGGED("Exception Logged"),
    EXCEPTION_ACKNOWLEDGED("Exception Acknowledged"),
    EXCEPTION_RESOLVED("Exception Resolved"),
    EXCEPTION_DISMISSED("Exception Dismissed"),
    REPORT_GENERATED("Analytics Report Generated"),
    THRESHOLD_BREACHED("Inventory Threshold Breached")
}
