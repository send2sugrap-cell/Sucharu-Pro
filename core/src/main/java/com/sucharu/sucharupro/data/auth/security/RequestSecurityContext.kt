package com.sucharu.sucharupro.data.auth.security

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationContext
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext

/**
 * Immutable request-scoped security context (INFRA-05 Step 03).
 *
 * Encapsulates the verified server-authoritative principal, derived tenant context,
 * correlation identifier, client IP, and authorization parameters for the current request.
 */
data class RequestSecurityContext(
    val principal: AuthenticatedPrincipal,
    val tenantContext: TenantContext = TenantContext(principal.projectId),
    val correlationId: String,
    val clientIp: String = "127.0.0.1",
    val authorizationContext: AuthorizationContext? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    init {
        require(tenantContext.projectId == principal.projectId) {
            "TenantContext projectId ('${tenantContext.projectId}') must match AuthenticatedPrincipal projectId ('${principal.projectId}')."
        }
    }
}
