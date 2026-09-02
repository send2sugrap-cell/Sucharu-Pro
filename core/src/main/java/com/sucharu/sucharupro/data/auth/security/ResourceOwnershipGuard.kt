package com.sucharu.sucharupro.data.auth.security

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UnauthenticatedException
import com.sucharu.sucharupro.data.api.model.UserRole

/**
 * Server-authoritative resource ownership and multi-tenant isolation guard (INFRA-05 Step 03).
 *
 * Enforces zero client trust:
 * - Customers cannot access other customers' records.
 * - Affiliates cannot access other affiliates' records.
 * - Cross-tenant requests are unconditionally rejected.
 */
object ResourceOwnershipGuard {

    /**
     * Enforces that the authenticated customer matches the target customerId.
     * Staff/Manager/Admin roles bypass customer ownership checks to perform management tasks.
     */
    fun enforceCustomerOwnership(principal: AuthenticatedPrincipal?, targetCustomerId: String) {
        if (principal == null) {
            throw UnauthenticatedException("Authentication is required.")
        }
        if (principal.isStaff) return

        if (principal.role == UserRole.CUSTOMER && principal.effectiveCustomerId != targetCustomerId) {
            throw ForbiddenException("Access denied: You do not have permission to access records belonging to customer '$targetCustomerId'.")
        }
    }

    /**
     * Enforces that the authenticated affiliate matches the target affiliateId.
     * Staff/Manager/Admin roles bypass affiliate ownership checks to perform management tasks.
     */
    fun enforceAffiliateOwnership(principal: AuthenticatedPrincipal?, targetAffiliateId: String) {
        if (principal == null) {
            throw UnauthenticatedException("Authentication is required.")
        }
        if (principal.isStaff) return

        if (principal.role == UserRole.AFFILIATE && principal.effectiveAffiliateId != targetAffiliateId) {
            throw ForbiddenException("Access denied: You do not have permission to access records belonging to affiliate '$targetAffiliateId'.")
        }
    }

    /**
     * Enforces strict multi-tenant boundary matching between principal and target project ID.
     */
    fun enforceTenantIsolation(principal: AuthenticatedPrincipal?, targetProjectId: String) {
        if (principal == null) {
            throw UnauthenticatedException("Authentication is required.")
        }
        if (principal.projectId != targetProjectId) {
            throw ForbiddenException("Access denied: Cross-tenant operation blocked. Principal belongs to '${principal.projectId}', target is '$targetProjectId'.")
        }
    }
}
