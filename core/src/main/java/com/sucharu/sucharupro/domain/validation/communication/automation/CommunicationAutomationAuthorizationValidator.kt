package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC and Separation of Duties validator for Automation Rules and Executions (Module 10 Step 08).
 */
object CommunicationAutomationAuthorizationValidator {

    fun validateRuleManagement(callerRole: UserRole): DomainResult<Unit> {
        if (callerRole != UserRole.ADMIN && callerRole != UserRole.MANAGER) {
            return DomainResult.Error(message = "Role '$callerRole' is not authorized to create or modify automation rules.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateRuleApproval(
        callerRole: UserRole,
        creatorUserId: String,
        approverUserId: String
    ): DomainResult<Unit> {
        if (callerRole != UserRole.ADMIN && callerRole != UserRole.MANAGER) {
            return DomainResult.Error(message = "Role '$callerRole' cannot approve automation rules.")
        }

        // Separation of Duties: Creator cannot approve their own rule unless ADMIN
        if (callerRole != UserRole.ADMIN && creatorUserId == approverUserId) {
            return DomainResult.Error(message = "Separation of Duties violation: Rule creator cannot approve their own automation rule.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateViewAnalytics(callerRole: UserRole): DomainResult<Unit> {
        if (!callerRole.isInternal) {
            return DomainResult.Error(message = "External roles cannot view automation analytics.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateTriggerSubmission(callerRole: UserRole): DomainResult<Unit> {
        // Internal users or authorized system integrations can trigger events
        return DomainResult.Success(Unit)
    }
}
