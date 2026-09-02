package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.AnalyticsFilter
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Role-Based Access Control Validator for Finance Analytics & Governance (Module 09 Step 10).
 */
object FinanceAnalyticsAuthorizationValidator {

    fun validateAnalyticsAccess(
        filter: AnalyticsFilter,
        callerRole: UserRole,
        actorId: String
    ): DomainResult<Unit> {
        if (filter.projectId.isBlank() || filter.projectId == "*") {
            return DomainResult.Error(message = "Project ID cannot be blank or wildcard.")
        }

        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.ACCOUNTS -> DomainResult.Success(Unit)
            UserRole.STAFF -> {
                DomainResult.Error(message = "Role STAFF is not authorized to access executive financial governance.")
            }
            UserRole.CUSTOMER,
            UserRole.VENDOR -> {
                DomainResult.Error(message = "External parties are strictly prohibited from accessing internal financial analytics.")
            }
            else -> DomainResult.Error(message = "Role $callerRole is not authorized to access financial analytics.")
        }
    }

    fun validateSnapshotCreation(callerRole: UserRole): DomainResult<Unit> {
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.ACCOUNTS -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Only ADMIN and ACCOUNTS roles are authorized to create immutable analytics snapshots.")
        }
    }

    fun validateExport(callerRole: UserRole): DomainResult<Unit> {
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.ACCOUNTS -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Caller is not authorized to export executive analytics.")
        }
    }
}
