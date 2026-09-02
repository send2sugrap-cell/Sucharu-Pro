package com.sucharu.sucharupro.data.auth.security

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.observability.logging.StructuredObservabilityLogger
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext

/**
 * Server-side Edge Security Filter and Interceptor (INFRA-05 Step 03).
 *
 * Enforces the canonical request security chain:
 * 1. Identifies public/unprotected routes.
 * 2. Extracts Bearer token from Authorization header.
 * 3. Validates cryptographic signature, algorithm allow-list, expiration, issuer, audience.
 * 4. Extracts trusted AuthenticatedPrincipal.
 * 5. Derives server-authoritative TenantContext (rejecting all client-supplied tenant spoofing).
 * 6. Binds RequestSecurityContext for the duration of the request.
 */
class EdgeSecurityInterceptor(
    private val securityContext: BackendSecurityContext,
    private val observabilityLogger: StructuredObservabilityLogger = StructuredObservabilityLogger()
) {

    /**
     * Determines whether an HTTP request targets a public, unauthenticated endpoint.
     */
    fun isPublicRoute(path: String, method: String): Boolean {
        if (method != "GET" && method != "POST") return false

        if (path.startsWith("/health")) return true
        if (path == "/ready" || path == "/health/ready" || path == "/health/readiness") return true
        if (path == "/metrics" && method == "GET") return true
        if (path == "/") return true
        if (path.startsWith("/api/v1/public/")) return true
        if (path.startsWith("/api/v1/webhooks") && method == "POST") return true

        // Authentication endpoints that must remain publicly accessible
        if (path == "/api/v1/auth/login" && method == "POST") return true
        if (path == "/api/v1/auth/register" && method == "POST") return true
        if (path == "/api/v1/auth/refresh" && method == "POST") return true
        if (path == "/api/v1/auth/password/recovery/request" && method == "POST") return true
        if (path == "/api/v1/auth/password/recovery/confirm" && method == "POST") return true

        return false
    }

    /**
     * Authenticates the request, deriving the server-authoritative [RequestSecurityContext].
     * Client-supplied tenant headers (e.g. X-Project-Id) are strictly ignored and will never
     * override the tenant derived from the verified cryptographic token.
     */
    fun authenticateRequest(request: HttpRequest): RequestSecurityContext {
        val principal = securityContext.authenticate(request.authorizationHeader)
        val authoritativeTenant = TenantContext(principal.projectId)

        // Log anti-spoofing warning if client attempted to send a mismatched X-Project-Id header
        val clientSuppliedProjectId = request.headers["X-Project-Id"] ?: request.headers["x-project-id"]
        if (clientSuppliedProjectId != null && clientSuppliedProjectId != principal.projectId) {
            observabilityLogger.log(
                projectId = principal.projectId,
                subsystem = "SECURITY_EDGE",
                operation = "TENANT_ANTI_SPOOFING_GUARD",
                level = "WARN",
                message = "Client spoofing attempt detected. Header 'X-Project-Id'='$clientSuppliedProjectId' discarded. Authoritative tenant bound to '${principal.projectId}' for principal '${principal.userId}'.",
                metadata = mapOf(
                    "correlationId" to request.correlationId,
                    "attemptedTenant" to clientSuppliedProjectId,
                    "authoritativeTenant" to principal.projectId,
                    "userId" to principal.userId
                )
            )
        }

        return RequestSecurityContext(
            principal = principal,
            tenantContext = authoritativeTenant,
            correlationId = request.correlationId,
            clientIp = request.clientIp
        )
    }
}
