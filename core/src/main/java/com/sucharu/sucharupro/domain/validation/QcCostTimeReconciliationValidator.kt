package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeReconciliation
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Domain validator for QC Cost & Time Reconciliation, locking, and RBAC governance (Module 06 Step 08).
 */
object QcCostTimeReconciliationValidator {

    /**
     * Validates reconciliation calculation input benchmarks.
     */
    fun validateCalculationParams(
        plannedCost: Double,
        plannedMinutes: Long
    ): DomainResult<Unit> {
        if (plannedCost < 0.0) {
            return DomainResult.Error(message = "Planned cost cannot be negative. Provided: $plannedCost")
        }
        if (plannedMinutes < 0L) {
            return DomainResult.Error(message = "Planned minutes cannot be negative. Provided: $plannedMinutes")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates prerequisites for permanently locking a reconciliation record.
     */
    fun validateLockPrerequisites(reconciliation: QcCostTimeReconciliation): DomainResult<Unit> {
        if (reconciliation.isLocked) {
            return DomainResult.Error(message = "Reconciliation record '${reconciliation.id}' is already LOCKED.")
        }
        if (reconciliation.status != QcCostStatus.RECONCILED && reconciliation.status != QcCostStatus.ADJUSTED) {
            return DomainResult.Error(
                message = "Cannot lock reconciliation in status '${reconciliation.status}'. Must be in RECONCILED or ADJUSTED state."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates prerequisites for applying an adjustment to reconciliation.
     */
    fun validateAdjustmentPrerequisites(
        reconciliation: QcCostTimeReconciliation,
        adjustmentReason: String
    ): DomainResult<Unit> {
        if (reconciliation.isLocked) {
            return DomainResult.Error(message = "Cannot adjust locked reconciliation record '${reconciliation.id}'.")
        }
        if (adjustmentReason.isBlank()) {
            return DomainResult.Error(message = "Adjustment reason cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Enforces immutability on locked reconciliation records.
     */
    fun validateImmutability(reconciliation: QcCostTimeReconciliation): DomainResult<Unit> {
        if (reconciliation.isLocked) {
            return DomainResult.Error(message = "Reconciliation '${reconciliation.id}' is permanently LOCKED and cannot be modified.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates role permissions for recording QC cost/time entries.
     */
    fun validateRecordPermission(role: UserRole?): DomainResult<Unit> {
        if (role == null) return DomainResult.Success(Unit)
        return when (role) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.QC_INSPECTOR -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Role '$role' is not authorized to record QC cost or time entries.")
        }
    }

    /**
     * Validates role permissions for calculating and generating reconciliation.
     */
    fun validateReconcilePermission(role: UserRole?): DomainResult<Unit> {
        if (role == null) return DomainResult.Success(Unit)
        return when (role) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.QC_INSPECTOR -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Role '$role' is not authorized to execute QC cost/time reconciliation.")
        }
    }

    /**
     * Validates role permissions for adjusting reconciliation records.
     */
    fun validateAdjustmentPermission(role: UserRole?): DomainResult<Unit> {
        if (role == null) return DomainResult.Success(Unit)
        return when (role) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Role '$role' is not authorized to adjust QC cost/time reconciliation records.")
        }
    }

    /**
     * Validates role permissions for permanently locking reconciliation records (Separation of Duties).
     */
    fun validateLockPermission(role: UserRole?): DomainResult<Unit> {
        if (role == null) return DomainResult.Success(Unit)
        return when (role) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            UserRole.QC_INSPECTOR -> DomainResult.Error(
                message = "Separation of duties violation: QC Inspectors cannot permanently lock reconciliation records. Management approval (ADMIN or MANAGER) is required."
            )
            else -> DomainResult.Error(message = "Role '$role' is not authorized to lock QC reconciliation records.")
        }
    }
}
