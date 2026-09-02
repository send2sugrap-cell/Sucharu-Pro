package com.sucharu.sucharupro.ui.features.auth

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.model.AccountStatus
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Legacy Server-Authoritative Navigation Destinations (INFRA-03 Step 04 & Step 05).
 * Preserved for backward compatibility.
 */
sealed class AuthDestination(val route: String) {
    object Login : AuthDestination("auth/login")
    object Register : AuthDestination("auth/register")
    object Verification : AuthDestination("auth/verification")
    object ForgotPassword : AuthDestination("auth/forgot-password")
    object ResetPassword : AuthDestination("auth/reset-password")
    object SessionManagement : AuthDestination("auth/sessions")

    // Role-Aware Workspaces
    object CustomerWorkspace : AuthDestination("workspace/customer")
    object AffiliateWorkspace : AuthDestination("workspace/affiliate")
    object StaffWorkspace : AuthDestination("workspace/staff")
    object ManagerWorkspace : AuthDestination("workspace/manager")
    object AdminWorkspace : AuthDestination("workspace/admin")
    object PublicGuestHome : AuthDestination("public/home")
}

/**
 * Server-authoritative post-login route resolver (INFRA-03 Step 06).
 * Evaluates the authenticated principal's role, account status, and verification state to determine target destination.
 */
object PostLoginRouter {

    /**
     * Resolves canonical [AppDestination] based strictly on server-authoritative principal.
     */
    fun resolveAppDestination(principal: AuthenticatedPrincipal): AppDestination {
        // 1. AI_AGENT machine principal boundary check
        if (principal.role == UserRole.AI_AGENT) {
            // Machine principals MUST NOT enter human interactive workspaces
            return AppDestination.Security.Forbidden
        }

        // 2. Account status routing
        val status = principal.accountStatus ?: AccountStatus.ACTIVE
        return when (status) {
            AccountStatus.PENDING -> AppDestination.Security.VerificationRequired
            AccountStatus.LOCKED,
            AccountStatus.SUSPENDED,
            AccountStatus.DEACTIVATED,
            AccountStatus.DELETED,
            AccountStatus.INACTIVE -> AppDestination.Security.AccountUnavailable
            AccountStatus.SECURITY_REVIEW -> AppDestination.Security.SecurityReview
            AccountStatus.ACTIVE -> {
                // 3. Role workspace routing
                when (principal.role) {
                    UserRole.ADMIN -> AppDestination.Admin.FullAdministration
                    UserRole.MANAGER -> AppDestination.Manager.Operations
                    UserRole.STAFF -> AppDestination.Staff.AssignedWork
                    UserRole.CUSTOMER -> AppDestination.Customer.Home
                    UserRole.AFFILIATE -> AppDestination.Affiliate.Home
                    UserRole.VENDOR -> AppDestination.Public.Home
                    UserRole.GUEST -> AppDestination.Public.Home
                    UserRole.AI_AGENT -> AppDestination.Security.Forbidden
                }
            }
        }
    }

    /**
     * Legacy workspace destination resolver preserved for backward compatibility.
     */
    fun resolveWorkspaceDestination(principal: AuthenticatedPrincipal): AuthDestination {
        val dest = resolveAppDestination(principal)
        return when (dest) {
            is AppDestination.Admin -> AuthDestination.AdminWorkspace
            is AppDestination.Manager -> AuthDestination.ManagerWorkspace
            is AppDestination.Staff -> AuthDestination.StaffWorkspace
            is AppDestination.Customer -> AuthDestination.CustomerWorkspace
            is AppDestination.Affiliate -> AuthDestination.AffiliateWorkspace
            else -> AuthDestination.PublicGuestHome
        }
    }
}

