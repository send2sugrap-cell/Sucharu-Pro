package com.sucharu.sucharupro.data.auth

import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Mock Connection Provider for UI Auth Integration Testing in :app.
 */
class MockIdentityConnectionProvider : PostgresConnectionProvider {
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

    override suspend fun acquireConnection(): Connection {
        return java.lang.reflect.Proxy.newProxyInstance(
            Connection::class.java.classLoader,
            arrayOf(Connection::class.java),
            java.lang.reflect.InvocationHandler { _, method, args ->
                when (method.name) {
                    "prepareStatement" -> createMockPreparedStatement()
                    "setAutoCommit", "commit", "rollback", "close" -> null
                    "isClosed" -> isClosed
                    "isValid" -> true
                    else -> null
                }
            }
        ) as Connection
    }

    override suspend fun releaseConnection(connection: Connection) {}

    private fun createMockPreparedStatement(): PreparedStatement {
        return java.lang.reflect.Proxy.newProxyInstance(
            PreparedStatement::class.java.classLoader,
            arrayOf(PreparedStatement::class.java),
            java.lang.reflect.InvocationHandler { _, method, _ ->
                when (method.name) {
                    "setString", "setObject", "setBigDecimal", "setInt", "setLong", "setBoolean", "setTimestamp", "setNull" -> null
                    "execute", "executeUpdate" -> 1
                    "executeQuery" -> createMockResultSet()
                    "close" -> null
                    else -> null
                }
            }
        ) as PreparedStatement
    }

    private fun createMockResultSet(): ResultSet {
        return java.lang.reflect.Proxy.newProxyInstance(
            ResultSet::class.java.classLoader,
            arrayOf(ResultSet::class.java),
            java.lang.reflect.InvocationHandler { _, method, _ ->
                when (method.name) {
                    "next" -> false
                    "close" -> null
                    else -> null
                }
            }
        ) as ResultSet
    }
}
