package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Validator for [ProductionQc] entity fields, required parameters, and creation constraints.
 */
object ProductionQcValidator {

    val AUTHORIZED_MANAGEMENT_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER)

    /**
     * Validates whether a caller with [callerRole] can create or supervise QC records.
     */
    fun validateQcManagementPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_MANAGEMENT_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to manage QC records."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates QC creation parameters.
     */
    fun validateCreation(
        productionJobId: String,
        qcType: QcType,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateQcManagementPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates entity internal state consistency.
     */
    fun validateQc(qc: ProductionQc): DomainResult<Unit> {
        if (qc.qcId.isBlank()) {
            return DomainResult.Error(message = "QC ID cannot be blank.")
        }
        if (qc.productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (qc.createdAt.isBlank()) {
            return DomainResult.Error(message = "Creation timestamp cannot be blank.")
        }
        if (qc.updatedAt.isBlank()) {
            return DomainResult.Error(message = "Update timestamp cannot be blank.")
        }

        if (qc.status == QcStatus.PASSED && qc.decision != QcDecision.PASS) {
            return DomainResult.Error(message = "QC marked as PASSED must have PASS decision.")
        }
        if (qc.status == QcStatus.FAILED && qc.decision != QcDecision.FAIL) {
            return DomainResult.Error(message = "QC marked as FAILED must have FAIL decision.")
        }
        if (qc.status in listOf(QcStatus.DRAFT, QcStatus.PENDING_INSPECTION, QcStatus.IN_INSPECTION) && qc.decision != QcDecision.PENDING) {
            return DomainResult.Error(message = "Non-terminal QC must have PENDING decision.")
        }

        if (qc.status in listOf(QcStatus.PASSED, QcStatus.FAILED) && qc.completedAt.isNullOrBlank()) {
            return DomainResult.Error(message = "Completed QC must have a completion timestamp.")
        }

        return DomainResult.Success(Unit)
    }
}
