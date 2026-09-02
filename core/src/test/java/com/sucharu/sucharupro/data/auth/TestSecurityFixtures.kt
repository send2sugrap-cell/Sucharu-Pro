package com.sucharu.sucharupro.data.auth

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext

/**
 * Explicit test fixture token registry for integration tests.
 * Test-only tokens isolated strictly to the test source tree.
 */
object TestSecurityFixtures {

    fun registerStandardTestTokens(securityContext: BackendSecurityContext) {
        securityContext.registerToken(
            token = "token-customer-100",
            principal = AuthenticatedPrincipal(
                userId = "CUST-100",
                projectId = "TENANT-001",
                username = "customer_acme",
                role = UserRole.CUSTOMER,
                permissions = setOf(
                    UserPermission.READ_OWN_PROFILE,
                    UserPermission.READ_OWN_ORDERS,
                    UserPermission.CREATE_ORDER,
                    UserPermission.READ_OWN_INVOICES,
                    UserPermission.READ_OWN_DELIVERY
                ),
                email = "acme@example.com"
            )
        )
        securityContext.registerToken(
            token = "token-customer-200",
            principal = AuthenticatedPrincipal(
                userId = "CUST-200",
                projectId = "TENANT-001",
                username = "customer_beta",
                role = UserRole.CUSTOMER,
                permissions = setOf(
                    UserPermission.READ_OWN_PROFILE,
                    UserPermission.READ_OWN_ORDERS,
                    UserPermission.CREATE_ORDER,
                    UserPermission.READ_OWN_INVOICES
                ),
                email = "beta@example.com"
            )
        )
        securityContext.registerToken(
            token = "token-affiliate-100",
            principal = AuthenticatedPrincipal(
                userId = "AFF-100",
                projectId = "TENANT-001",
                username = "affiliate_top",
                role = UserRole.AFFILIATE,
                permissions = setOf(
                    UserPermission.READ_OWN_PROFILE,
                    UserPermission.READ_OWN_AFFILIATE
                ),
                email = "affiliate100@sucharu.pro"
            )
        )
        securityContext.registerToken(
            token = "token-affiliate-200",
            principal = AuthenticatedPrincipal(
                userId = "AFF-200",
                projectId = "TENANT-001",
                username = "affiliate_secondary",
                role = UserRole.AFFILIATE,
                permissions = setOf(
                    UserPermission.READ_OWN_PROFILE,
                    UserPermission.READ_OWN_AFFILIATE
                ),
                email = "affiliate200@sucharu.pro"
            )
        )
        securityContext.registerToken(
            token = "token-tenant-b-customer",
            principal = AuthenticatedPrincipal(
                userId = "CUST-B-999",
                projectId = "TENANT-002",
                username = "tenant_b_user",
                role = UserRole.CUSTOMER,
                permissions = setOf(UserPermission.READ_OWN_PROFILE, UserPermission.READ_OWN_ORDERS),
                email = "tenantb@example.com"
            )
        )
        securityContext.registerToken(
            token = "token-staff-admin",
            principal = AuthenticatedPrincipal(
                userId = "STAFF-001",
                projectId = "TENANT-001",
                username = "admin_master",
                role = UserRole.ADMIN,
                permissions = setOf(UserPermission.ADMIN_ALL),
                email = "admin@sucharu.pro"
            )
        )
    }
}

/**
 * Mock Connection Provider for Identity Lifecycle Integration Testing.
 */
class MockIdentityConnectionProvider : com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider {
    var isClosed = false

    override fun getActiveConnectionCount(): Int = 0
    override fun getIdleConnectionCount(): Int = 1
    override fun getTotalAcquisitions(): Long = 1L
    override fun getAcquisitionFailureCount(): Long = 0L

    override suspend fun shutdownGracefully(drainTimeoutMs: Long) {
        isClosed = true
    }

    override fun close() {
        isClosed = true
    }

    override suspend fun acquireConnection(): java.sql.Connection {
        return java.lang.reflect.Proxy.newProxyInstance(
            java.sql.Connection::class.java.classLoader,
            arrayOf(java.sql.Connection::class.java),
            java.lang.reflect.InvocationHandler { _, method, args ->
                when (method.name) {
                    "prepareStatement" -> createMockPreparedStatement()
                    "setAutoCommit", "commit", "rollback", "close" -> null
                    "isClosed" -> isClosed
                    "isValid" -> true
                    else -> null
                }
            }
        ) as java.sql.Connection
    }

    override suspend fun releaseConnection(connection: java.sql.Connection) {}

    private fun createMockPreparedStatement(): java.sql.PreparedStatement {
        return java.lang.reflect.Proxy.newProxyInstance(
            java.sql.PreparedStatement::class.java.classLoader,
            arrayOf(java.sql.PreparedStatement::class.java),
            java.lang.reflect.InvocationHandler { _, method, _ ->
                when (method.name) {
                    "setString", "setObject", "setBigDecimal", "setInt", "setLong", "setBoolean", "setTimestamp", "setNull" -> null
                    "execute", "executeUpdate" -> 1
                    "executeQuery" -> createMockResultSet()
                    "close" -> null
                    else -> null
                }
            }
        ) as java.sql.PreparedStatement
    }

    private fun createMockResultSet(): java.sql.ResultSet {
        return java.lang.reflect.Proxy.newProxyInstance(
            java.sql.ResultSet::class.java.classLoader,
            arrayOf(java.sql.ResultSet::class.java),
            java.lang.reflect.InvocationHandler { _, method, _ ->
                when (method.name) {
                    "next" -> false
                    "close" -> null
                    else -> null
                }
            }
        ) as java.sql.ResultSet
    }
}
