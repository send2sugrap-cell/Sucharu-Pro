package com.sucharu.sucharupro.domain.validation.notification

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.Notification
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Role-Based Access Control & Tenant Boundary Validator for Notifications (Module 10 Step 01).
 */
object NotificationAuthorizationValidator {

    fun validateNotificationView(
        notification: Notification,
        requestProjectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Unit> {
        if (notification.projectId != requestProjectId) {
            return DomainResult.Error(message = "Cross-project notification access is strictly prohibited.")
        }

        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            UserRole.ACCOUNTS -> {
                if (notification.recipientUserId == actorId || notification.notificationType.category.name == "FINANCE") {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Accounts role is only authorized to view own and finance notifications.")
                }
            }
            UserRole.STAFF,
            UserRole.DESIGNER,
            UserRole.QC_INSPECTOR,
            UserRole.WAREHOUSE,
            UserRole.AFFILIATE -> {
                if (notification.recipientUserId == actorId) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Staff users are only authorized to view notifications directed to themselves.")
                }
            }
            UserRole.CUSTOMER -> {
                if (notification.recipientUserId == actorId) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Customers are strictly restricted to their own notifications.")
                }
            }
            UserRole.VENDOR -> {
                if (notification.recipientUserId == actorId) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Vendors are strictly restricted to their own notifications.")
                }
            }
        }
    }

    fun validateUserQuery(
        targetUserId: String,
        requestProjectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Unit> {
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> {
                if (targetUserId == actorId) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Role $callerRole cannot query notifications for user '$targetUserId'.")
                }
            }
        }
    }

    fun validateAdminDashboardAccess(callerRole: UserRole): DomainResult<Unit> {
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Only ADMIN and MANAGER roles can access the Notification Admin Dashboard.")
        }
    }

    fun validateTemplateManagement(callerRole: UserRole): DomainResult<Unit> {
        return when (callerRole) {
            UserRole.ADMIN -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Only ADMIN users are authorized to manage notification templates.")
        }
    }
}
