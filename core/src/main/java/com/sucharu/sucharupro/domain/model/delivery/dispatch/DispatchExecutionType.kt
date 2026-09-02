package com.sucharu.sucharupro.domain.model.delivery.dispatch

/**
 * Categorization of dispatch executions (Module 08 Step 03).
 */
enum class DispatchExecutionType(val defaultLabel: String) {
    STANDARD("Standard Dispatch"),
    CUSTOMER_DELIVERY("Customer Delivery"),
    INTERNAL("Internal Transfer"),
    REPLACEMENT("Replacement Dispatch"),
    EMERGENCY("Emergency Dispatch"),
    OTHER("Other")
}
