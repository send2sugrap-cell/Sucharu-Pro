package com.sucharu.sucharupro.domain.validation.communication.internal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunication
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationType
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Role-Based Access Control and Tenant Boundary Validator for Internal Communications (Module 10 Step 03).
 */
object InternalCommunicationAuthorizationValidator {

    fun validateInternalUser(callerRole: UserRole): DomainResult<Unit> {
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.ACCOUNTS,
            UserRole.STAFF,
            UserRole.DESIGNER,
            UserRole.QC_INSPECTOR,
            UserRole.WAREHOUSE,
            UserRole.AFFILIATE -> DomainResult.Success(Unit)
            UserRole.VENDOR -> DomainResult.Error(
                message = "Role 'VENDOR' is external and strictly prohibited from accessing internal communications (VENDOR role is blocked)."
            )
            UserRole.CUSTOMER -> DomainResult.Error(
                message = "Role 'CUSTOMER' is external and strictly prohibited from accessing internal communications."
            )
        }
    }

    fun validateView(
        communication: InternalCommunication,
        requestProjectId: String,
        actorUserId: String,
        callerRole: UserRole
    ): DomainResult<Unit> {
        if (communication.projectId != requestProjectId) {
            return DomainResult.Error(message = "Cross-project internal communication access is strictly prohibited.")
        }

        val internalCheck = validateInternalUser(callerRole)
        if (internalCheck is DomainResult.Error) return internalCheck

        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            UserRole.ACCOUNTS -> {
                if (communication.senderUserId == actorUserId ||
                    communication.recipientUserIds.contains(actorUserId) ||
                    communication.communicationType == InternalCommunicationType.FINANCE_DISCUSSION ||
                    communication.recipientType == InternalCommunicationRecipientType.ALL_INTERNAL_USERS ||
                    communication.recipientType == InternalCommunicationRecipientType.PROJECT
                ) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Accounts role is only authorized to view finance-related and direct internal messages (Recipient isolation).")
                }
            }
            UserRole.STAFF,
            UserRole.DESIGNER,
            UserRole.QC_INSPECTOR,
            UserRole.WAREHOUSE,
            UserRole.AFFILIATE -> {
                if (communication.senderUserId == actorUserId ||
                    communication.recipientUserIds.contains(actorUserId) ||
                    communication.recipientType == InternalCommunicationRecipientType.ALL_INTERNAL_USERS ||
                    communication.recipientType == InternalCommunicationRecipientType.PROJECT
                ) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "User is not authorized to view this communication (Recipient isolation).")
                }
            }
            UserRole.CUSTOMER -> DomainResult.Error(message = "CUSTOMER role is blocked from accessing internal communications.")
            UserRole.VENDOR -> DomainResult.Error(message = "VENDOR role is blocked from accessing internal communications.")
        }
    }

    fun validateBroadcast(
        recipientType: InternalCommunicationRecipientType,
        callerRole: UserRole
    ): DomainResult<Unit> {
        val internalCheck = validateInternalUser(callerRole)
        if (internalCheck is DomainResult.Error) return internalCheck

        return when (callerRole) {
            UserRole.ADMIN -> DomainResult.Success(Unit)
            UserRole.MANAGER -> {
                DomainResult.Success(Unit)
            }
            UserRole.ACCOUNTS -> {
                if (recipientType == InternalCommunicationRecipientType.DEPARTMENT || recipientType == InternalCommunicationRecipientType.TEAM) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Accounts role cannot broadcast across the entire organization.")
                }
            }
            else -> DomainResult.Error(message = "Staff role '$callerRole' is not authorized to initiate organizational broadcasts.")
        }
    }
}
