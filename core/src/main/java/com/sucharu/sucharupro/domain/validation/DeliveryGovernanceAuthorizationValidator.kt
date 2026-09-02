package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Validates role-based permissions and separation of duties for Delivery Analytics and Governance.
 */
object DeliveryGovernanceAuthorizationValidator {

    fun validateViewAnalytics(role: UserRole): DomainResult<Unit> {
        return DomainResult.Success(Unit)
    }

    fun validateViewGovernance(role: UserRole): DomainResult<Unit> {
        return if (role.isInternal) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Access Denied: External role '${role.defaultLabel}' is not authorized to view internal governance alerts.")
        }
    }

    fun validateAcknowledgeAlert(role: UserRole): DomainResult<Unit> {
        val authorized = setOf(
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE,
            UserRole.QC_INSPECTOR,
            UserRole.ACCOUNTS
        )
        return if (role in authorized) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Access Denied: Role '${role.defaultLabel}' is not authorized to acknowledge governance alerts.")
        }
    }

    fun validateResolveAlert(role: UserRole): DomainResult<Unit> {
        val authorized = setOf(
            UserRole.ADMIN,
            UserRole.MANAGER
        )
        return if (role in authorized) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Access Denied: Role '${role.defaultLabel}' is not authorized to resolve governance alerts.")
        }
    }

    fun validateDismissAlert(role: UserRole): DomainResult<Unit> {
        val authorized = setOf(
            UserRole.ADMIN,
            UserRole.MANAGER
        )
        return if (role in authorized) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Access Denied: Role '${role.defaultLabel}' is not authorized to dismiss governance alerts.")
        }
    }
}
