package com.sucharu.sucharupro.domain.validation.communication.customer

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.customer.CustomerCommunication
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Role-Based Access Control and Customer Boundary Validator (Module 10 Step 02).
 */
object CustomerCommunicationAuthorizationValidator {

    fun validateView(
        communication: CustomerCommunication,
        requestProjectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Unit> {
        if (communication.projectId != requestProjectId) {
            return DomainResult.Error(message = "Cross-project customer communication access is strictly prohibited.")
        }

        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            UserRole.ACCOUNTS -> {
                if (communication.communicationType.name.startsWith("PAYMENT") || communication.recipientUserId == actorId) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Accounts role is only authorized to access payment/finance customer communications.")
                }
            }
            UserRole.STAFF,
            UserRole.DESIGNER,
            UserRole.QC_INSPECTOR,
            UserRole.WAREHOUSE,
            UserRole.AFFILIATE -> {
                if (communication.recipientUserId == actorId) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Staff role is only authorized to view communications directed to themselves.")
                }
            }
            UserRole.CUSTOMER -> {
                if (communication.customerId == actorId || communication.recipientUserId == actorId) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Customers are strictly restricted to their own communications.")
                }
            }
            UserRole.VENDOR -> {
                DomainResult.Error(message = "Vendors are strictly prohibited from accessing customer communications.")
            }
        }
    }

    fun validateCustomerQuery(
        targetCustomerId: String,
        requestProjectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Unit> {
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.ACCOUNTS -> DomainResult.Success(Unit)
            UserRole.CUSTOMER -> {
                if (targetCustomerId == actorId) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Customers can only query their own communications.")
                }
            }
            else -> {
                if (targetCustomerId == actorId) DomainResult.Success(Unit)
                else DomainResult.Error(message = "Unauthorized customer communication query for role $callerRole.")
            }
        }
    }

    fun validateAdminOperations(callerRole: UserRole): DomainResult<Unit> {
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Only ADMIN and MANAGER roles can perform administrative customer communication operations.")
        }
    }
}
