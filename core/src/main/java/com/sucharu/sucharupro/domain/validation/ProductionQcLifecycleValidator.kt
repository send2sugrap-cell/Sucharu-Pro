package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Authoritative validator for [ProductionQc] lifecycle state transitions and terminal state protection.
 */
object ProductionQcLifecycleValidator {

    /**
     * Validates whether [qc] can transition from its current status to [targetStatus].
     */
    fun validateStatusTransition(
        qc: ProductionQc,
        targetStatus: QcStatus
    ): DomainResult<Unit> {
        val currentStatus = qc.status

        // 1. Reject self-transitions
        if (currentStatus == targetStatus) {
            return DomainResult.Error(
                message = "QC record '${qc.qcId}' is already in ${currentStatus.defaultLabel} state."
            )
        }

        // 2. Reject terminal state mutations
        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Cannot modify terminal QC record '${qc.qcId}' (Current Status: ${currentStatus.defaultLabel})."
            )
        }

        // 3. Validate transition matrix
        if (!currentStatus.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                message = "Invalid QC status transition from ${currentStatus.defaultLabel} to ${targetStatus.defaultLabel}."
            )
        }

        // 4. Starting inspection requires an assigned inspector
        if (targetStatus == QcStatus.IN_INSPECTION && !qc.isAssigned) {
            return DomainResult.Error(
                message = "Cannot start QC inspection without an assigned QC Inspector."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates inspection completion prerequisites.
     */
    fun validateCompletion(
        qc: ProductionQc,
        decision: QcDecision,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        if (qc.isTerminal) {
            return DomainResult.Error(message = "Cannot complete already terminal QC record '${qc.qcId}'.")
        }

        if (qc.status != QcStatus.IN_INSPECTION) {
            return DomainResult.Error(
                message = "Cannot complete QC inspection that is not IN_INSPECTION (Current: ${qc.status.defaultLabel})."
            )
        }

        if (decision == QcDecision.PENDING) {
            return DomainResult.Error(message = "Cannot complete QC with PENDING decision. Must be PASS or FAIL.")
        }

        val rbacResult = QcAssignmentValidator.validateInspectionPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates cancellation of a QC record.
     */
    fun validateCancellation(
        qc: ProductionQc,
        reason: String?,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        if (qc.isTerminal) {
            return DomainResult.Error(message = "Cannot cancel already terminal QC record '${qc.qcId}'.")
        }

        if (reason.isNullOrBlank()) {
            return DomainResult.Error(message = "Cancellation reason is required and cannot be blank.")
        }

        val rbacResult = ProductionQcValidator.validateQcManagementPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        return DomainResult.Success(Unit)
    }
}
