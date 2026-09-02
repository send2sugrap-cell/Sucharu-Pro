package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus

/**
 * Validates lifecycle transitions for Dispatch Executions (Module 08 Step 03).
 */
object DispatchExecutionLifecycleValidator {

    fun validateTransition(
        currentStatus: DispatchExecutionStatus,
        targetStatus: DispatchExecutionStatus
    ): DomainResult<Unit> {
        return when (currentStatus) {
            DispatchExecutionStatus.DRAFT -> {
                if (targetStatus == DispatchExecutionStatus.PENDING || targetStatus == DispatchExecutionStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Draft dispatch execution can only transition to Pending or Cancelled.")
                }
            }
            DispatchExecutionStatus.PENDING -> {
                if (targetStatus == DispatchExecutionStatus.APPROVED ||
                    targetStatus == DispatchExecutionStatus.DRAFT ||
                    targetStatus == DispatchExecutionStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Pending dispatch execution can transition to Approved, Draft, or Cancelled.")
                }
            }
            DispatchExecutionStatus.APPROVED -> {
                if (targetStatus == DispatchExecutionStatus.READY_FOR_EXECUTION || targetStatus == DispatchExecutionStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Approved dispatch execution can transition to Ready for Execution or Cancelled.")
                }
            }
            DispatchExecutionStatus.READY_FOR_EXECUTION -> {
                if (targetStatus == DispatchExecutionStatus.EXECUTING || targetStatus == DispatchExecutionStatus.DISPATCHED || targetStatus == DispatchExecutionStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Ready for Execution can transition to Executing, Dispatched, or Cancelled.")
                }
            }
            DispatchExecutionStatus.EXECUTING -> {
                if (targetStatus == DispatchExecutionStatus.DISPATCHED || targetStatus == DispatchExecutionStatus.CANCELLED) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Executing dispatch can only transition to Dispatched or Cancelled.")
                }
            }
            DispatchExecutionStatus.DISPATCHED -> {
                DomainResult.Error(message = "Dispatched is a terminal status and cannot be changed.")
            }
            DispatchExecutionStatus.CANCELLED -> {
                DomainResult.Error(message = "Cancelled is a terminal status and cannot be changed.")
            }
        }
    }
}
