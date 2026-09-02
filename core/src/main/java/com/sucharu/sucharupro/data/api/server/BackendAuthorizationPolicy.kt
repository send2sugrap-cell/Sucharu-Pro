package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.auth.authorization.BackendAuthorizationService

/**
 * Backward-compatible gateway to the server-side RBAC & ABAC authorization service (INFRA-02 Step 04, INFRA-03 Step 02).
 */
object BackendAuthorizationPolicy {

    private val service = BackendAuthorizationService()

    fun requireCapability(principal: AuthenticatedPrincipal?, capability: AuthorizationCapability) {
        service.requireCapability(principal, capability)
    }

    fun requirePermission(principal: AuthenticatedPrincipal, permission: UserPermission) {
        if (permission == UserPermission.ADMIN_ALL && principal.role != UserRole.ADMIN) {
            service.requireRole(principal, UserRole.ADMIN)
        } else {
            val capability = try {
                AuthorizationCapability.valueOf(permission.name)
            } catch (_: Throwable) {
                null
            }
            if (capability != null) {
                service.requireCapability(principal, capability)
            } else {
                if (!principal.hasPermission(permission)) {
                    throw com.sucharu.sucharupro.data.api.model.ForbiddenException("Access denied: Missing required permission '${permission.name}'.")
                }
            }
        }
    }

    fun requireRole(principal: AuthenticatedPrincipal, vararg allowedRoles: UserRole) {
        service.requireRole(principal, *allowedRoles)
    }

    fun enforceCustomerOwnership(principal: AuthenticatedPrincipal, targetCustomerId: String) {
        service.enforceCustomerOwnership(principal, targetCustomerId)
    }

    fun enforceAffiliateOwnership(principal: AuthenticatedPrincipal, targetAffiliateId: String) {
        service.enforceAffiliateOwnership(principal, targetAffiliateId)
    }

    fun enforceTenantIsolation(principal: AuthenticatedPrincipal, targetProjectId: String) {
        service.enforceTenantIsolation(principal, targetProjectId)
    }
}
