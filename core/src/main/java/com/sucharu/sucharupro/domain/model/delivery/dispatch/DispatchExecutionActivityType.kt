package com.sucharu.sucharupro.domain.model.delivery.dispatch

/**
 * Audit event types for Dispatch Execution lifecycle (Module 08 Step 03).
 */
enum class DispatchExecutionActivityType(val defaultLabel: String) {
    CREATED("Created"),
    UPDATED("Updated"),
    SUBMITTED("Submitted"),
    APPROVED("Approved"),
    READY_FOR_EXECUTION("Ready for Execution"),
    EXECUTION_STARTED("Execution Started"),
    STOCK_OUT_CREATED("Stock Out Created"),
    DISPATCHED("Dispatched"),
    CANCELLED("Cancelled"),
    FAILED("Execution Failed")
}
