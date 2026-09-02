package com.sucharu.sucharupro.domain.model.delivery.dispatch

/**
 * Lifecycle states of an operational Dispatch Execution (Module 08 Step 03).
 */
enum class DispatchExecutionStatus {
    DRAFT,
    PENDING,
    APPROVED,
    READY_FOR_EXECUTION,
    EXECUTING,
    DISPATCHED,
    CANCELLED;

    val isTerminal: Boolean
        get() = this == DISPATCHED || this == CANCELLED
}
