package com.sucharu.sucharupro.data.persistence.postgres

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

/**
 * Context carried within an active PostgreSQL transaction (INFRA-01 Step 03).
 */
data class TransactionContext(
    val tenantContext: TenantContext,
    val sqlExecutor: SqlExecutor,
    val connection: Connection
)

/**
 * Coroutine-safe, multi-tenant aware transaction manager (INFRA-01 Step 03).
 */
interface TransactionManager {
    suspend fun <T> inTransaction(
        tenantContext: TenantContext,
        block: suspend (TransactionContext) -> T
    ): T

    suspend fun <T> inReadOnly(
        tenantContext: TenantContext,
        block: suspend (TransactionContext) -> T
    ): T
}

/**
 * Default implementation of [TransactionManager] enforcing RLS tenant context scoping.
 */
class DefaultPostgresTransactionManager(
    private val connectionProvider: PostgresConnectionProvider
) : TransactionManager {

    private fun setTenantSessionContext(connection: Connection, projectId: String) {
        connection.prepareStatement("SELECT set_config('app.current_project_id', ?, true)").use { stmt ->
            stmt.setString(1, projectId)
            stmt.execute()
        }
    }

    override suspend fun <T> inTransaction(
        tenantContext: TenantContext,
        block: suspend (TransactionContext) -> T
    ): T = withContext(Dispatchers.IO) {
        val connection = connectionProvider.acquireConnection()
        try {
            connection.autoCommit = false
            setTenantSessionContext(connection, tenantContext.projectId)

            val txContext = TransactionContext(
                tenantContext = tenantContext,
                sqlExecutor = SqlExecutor(connection),
                connection = connection
            )

            val result = block(txContext)
            connection.commit()
            result
        } catch (e: Throwable) {
            try {
                if (!connection.isClosed) {
                    connection.rollback()
                }
            } catch (_: Exception) {}
            throw e
        } finally {
            connectionProvider.releaseConnection(connection)
        }
    }

    override suspend fun <T> inReadOnly(
        tenantContext: TenantContext,
        block: suspend (TransactionContext) -> T
    ): T = withContext(Dispatchers.IO) {
        val connection = connectionProvider.acquireConnection()
        try {
            connection.autoCommit = false
            setTenantSessionContext(connection, tenantContext.projectId)

            val txContext = TransactionContext(
                tenantContext = tenantContext,
                sqlExecutor = SqlExecutor(connection),
                connection = connection
            )

            val result = block(txContext)
            connection.commit()
            result
        } catch (e: Throwable) {
            try {
                if (!connection.isClosed) {
                    connection.rollback()
                }
            } catch (_: Exception) {}
            throw e
        } finally {
            connectionProvider.releaseConnection(connection)
        }
    }
}
