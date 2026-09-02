package com.sucharu.sucharupro.data.persistence.postgres

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Production Runtime, Operations, Observability & Disaster Recovery Test Suite (INFRA-02 Step 03).
 */
class PostgresProductionRuntimeOperationsTest {

    private lateinit var mockProvider: MockRuntimeConnectionProvider
    private lateinit var transactionManager: TransactionManager
    private lateinit var healthChecker: DatabaseHealthChecker
    private lateinit var migrationRunner: PostgresMigrationRunner
    private lateinit var backupOperations: PostgresBackupRestoreOperations

    @Before
    fun setUp() {
        mockProvider = MockRuntimeConnectionProvider()
        transactionManager = DefaultPostgresTransactionManager(mockProvider)
        healthChecker = DatabaseHealthChecker(mockProvider)
        migrationRunner = PostgresMigrationRunner(mockProvider)
        backupOperations = PostgresBackupRestoreOperations(mockProvider)
        PostgresObservability.resetMetrics()
        PostgresObservability.clearListeners()
    }

    // ====================================================================================
    // 1. CONFIGURATION VALIDATION & FAIL-FAST HARDENING
    // ====================================================================================

    @Test
    fun `Configuration - validates valid configuration and redacts credentials`() {
        val validConfig = PostgresConnectionConfig(
            host = "db.production.internal",
            port = 5432,
            database = "sucharu_pro_db",
            user = "sucharu_app",
            password = "superSecretPassword123!",
            maxPoolSize = 20,
            minIdleConnections = 5,
            sslMode = "require"
        )

        val errors = validConfig.validateForProduction()
        assertTrue("Valid config must have zero validation errors", errors.isEmpty())

        val safeString = validConfig.toSafeString()
        assertFalse("Safe string must not contain actual password", safeString.contains("superSecretPassword123!"))
        assertTrue("Safe string must indicate redacted password", safeString.contains("[REDACTED]"))
        assertTrue("JDBC URL must contain sslMode", validConfig.toJdbcUrl().contains("sslmode=require"))
    }

    @Test
    fun `Configuration - fails fast when mandatory production parameters are invalid`() {
        val invalidConfig = PostgresConnectionConfig(
            host = "",
            port = 99999, // Invalid port
            database = "",
            user = "",
            password = "",
            maxPoolSize = 0,
            minIdleConnections = -1
        )

        val errors = invalidConfig.validateForProduction()
        assertTrue(errors.size >= 5)
        assertTrue(errors.any { it.contains("DATABASE_HOST") })
        assertTrue(errors.any { it.contains("DATABASE_PORT") })
        assertTrue(errors.any { it.contains("DATABASE_NAME") })
        assertTrue(errors.any { it.contains("DATABASE_PASSWORD") })
    }

    // ====================================================================================
    // 2. CONNECTION POOL METRICS & GRACEFUL SHUTDOWN
    // ====================================================================================

    @Test
    fun `Connection Pool - tracks metrics and shuts down gracefully`() = runBlocking {
        val conn1 = mockProvider.acquireConnection()
        assertEquals(1, mockProvider.getActiveConnectionCount())
        assertEquals(1L, mockProvider.getTotalAcquisitions())

        mockProvider.releaseConnection(conn1)
        assertEquals(0, mockProvider.getActiveConnectionCount())
        assertEquals(1, mockProvider.getIdleConnectionCount())

        mockProvider.shutdownGracefully(drainTimeoutMs = 1000L)
        assertTrue(mockProvider.isClosed)
    }

    // ====================================================================================
    // 3. TENANT SESSION SAFETY ON POOLED CONNECTION REUSE
    // ====================================================================================

    @Test
    fun `Tenant Session Safety - pooled connection resets tenant session between Tenant A and Tenant B`() = runBlocking {
        val tenantA = TenantContext("PROJECT-TENANT-A")
        val tenantB = TenantContext("PROJECT-TENANT-B")

        // 1. Tenant A executes transaction
        transactionManager.inTransaction(tenantA) { ctx ->
            assertEquals("PROJECT-TENANT-A", mockProvider.currentSessionProjectId)
        }

        // Connection returned to pool and cleansed
        assertEquals("", mockProvider.currentSessionProjectId)

        // 2. Tenant B executes transaction on recycled connection
        transactionManager.inTransaction(tenantB) { ctx ->
            assertEquals("PROJECT-TENANT-B", mockProvider.currentSessionProjectId)
        }

        // Cleansed again
        assertEquals("", mockProvider.currentSessionProjectId)
    }

    // ====================================================================================
    // 4. TRANSACTION ROLLBACK & RESOURCE CLEANUP
    // ====================================================================================

    @Test
    fun `Transaction Manager - unhandled exception rolls back transaction and cleans session`() = runBlocking {
        val tenantA = TenantContext("PROJECT-TENANT-A")
        var exceptionThrown = false

        try {
            transactionManager.inTransaction(tenantA) { ctx ->
                assertEquals("PROJECT-TENANT-A", mockProvider.currentSessionProjectId)
                throw RuntimeException("Forced simulated failure during checkout")
            }
        } catch (_: RuntimeException) {
            exceptionThrown = true
        }

        assertTrue("Exception must be rethrown", exceptionThrown)
        assertEquals("Session context must be cleansed after rollback", "", mockProvider.currentSessionProjectId)
        assertEquals(0, mockProvider.getActiveConnectionCount())
    }

    // ====================================================================================
    // 5. RETRY POLICY SAFETY CLASSIFICATION
    // ====================================================================================

    @Test
    fun `Retry Policy - accurately separates transient errors from non-retryable constraint violations`() {
        // Serialization failure (40001) -> Retryable
        val serializationEx = SQLException("Serialization conflict", "40001")
        assertEquals(PostgresFailureType.TRANSIENT_RETRYABLE, PostgresRetryPolicy.classifyFailure(serializationEx))
        assertTrue(PostgresRetryPolicy.isRetryable(serializationEx))

        // Deadlock detected (40P01) -> Retryable
        val deadlockEx = SQLException("Deadlock detected", "40P01")
        assertEquals(PostgresFailureType.TRANSIENT_RETRYABLE, PostgresRetryPolicy.classifyFailure(deadlockEx))
        assertTrue(PostgresRetryPolicy.isRetryable(deadlockEx))

        // Unique constraint violation (23505) -> NEVER RETRY
        val uniqueEx = SQLException("duplicate key value violates unique constraint", "23505")
        assertEquals(PostgresFailureType.NON_RETRYABLE_CONSTRAINT, PostgresRetryPolicy.classifyFailure(uniqueEx))
        assertFalse(PostgresRetryPolicy.isRetryable(uniqueEx))

        // Foreign key violation (23503) -> NEVER RETRY
        val fkEx = SQLException("insert or update on table violates foreign key constraint", "23503")
        assertEquals(PostgresFailureType.NON_RETRYABLE_CONSTRAINT, PostgresRetryPolicy.classifyFailure(fkEx))
        assertFalse(PostgresRetryPolicy.isRetryable(fkEx))

        // Check constraint violation (23514) -> NEVER RETRY
        val checkEx = SQLException("new row for relation violates check constraint", "23514")
        assertEquals(PostgresFailureType.NON_RETRYABLE_CONSTRAINT, PostgresRetryPolicy.classifyFailure(checkEx))
        assertFalse(PostgresRetryPolicy.isRetryable(checkEx))
    }

    // ====================================================================================
    // 6. HEALTH PROBE WITH TIMEOUT PROTECTION
    // ====================================================================================

    @Test
    fun `Health Probe - responds within timeout and exposes safe status`() = runBlocking {
        assertTrue(healthChecker.checkLiveness())

        val readiness = healthChecker.checkReadiness(timeoutMs = 2000L)
        assertTrue(readiness.isLive)
        assertTrue(readiness.isReady)
        assertEquals("sucharu_pro_db", readiness.databaseName)
        assertNotNull(readiness.latencyMs)
        assertNull(readiness.errorMessage)
    }

    // ====================================================================================
    // 7. FLYWAY MIGRATION RUNNER & VALIDATION
    // ====================================================================================

    @Test
    fun `Flyway Runner - validates canonical schema history V1 and V20260824`() = runBlocking {
        val result = migrationRunner.validateMigrations()
        assertTrue("Canonical migrations must be validated", result.isSuccess)
        assertEquals("20260824", result.currentVersion)
        assertEquals(2, result.appliedMigrations.size)
        assertTrue(result.pendingMigrations.isEmpty())
    }

    // ====================================================================================
    // 8. OBSERVABILITY METRICS & STRUCTURED LOGGING
    // ====================================================================================

    @Test
    fun `Observability - captures structured persistence events and aggregates metrics`() {
        val capturedEvents = mutableListOf<Pair<PersistenceLogEvent, Map<String, Any?>>>()
        PostgresObservability.addEventListener { event, metadata ->
            capturedEvents.add(Pair(event, metadata))
        }

        PostgresObservability.recordTransactionCommit(latencyMs = 15L)
        PostgresObservability.recordTransactionRollback(latencyMs = 25L, reason = "OptimisticLockException")

        val snapshot = PostgresObservability.getMetricsSnapshot(mockProvider)
        assertEquals(1L, snapshot.transactionsCommitted)
        assertEquals(1L, snapshot.transactionsRolledBack)
        assertEquals(20.0, snapshot.averageLatencyMs, 0.01)

        assertEquals(2, capturedEvents.size)
        assertEquals(PersistenceLogEvent.DATABASE_TRANSACTION_COMMIT, capturedEvents[0].first)
        assertEquals(PersistenceLogEvent.DATABASE_TRANSACTION_ROLLBACK, capturedEvents[1].first)
    }

    // ====================================================================================
    // 9. BACKUP & RESTORE DISASTER RECOVERY VALIDATION
    // ====================================================================================

    @Test
    fun `Disaster Recovery - extracts backup metadata and validates restore integrity`() = runBlocking {
        val backupMeta = backupOperations.createBackupMetadata("BKP-20260824-001", "sucharu_pro_db")
        assertNotNull(backupMeta)
        assertEquals("BKP-20260824-001", backupMeta.backupId)
        assertEquals("20260824", backupMeta.schemaVersion)
        assertTrue(backupMeta.tableCount >= 5)

        val restoreVerified = backupOperations.verifyRestoredDatabase(backupMeta)
        assertTrue("Restored database must satisfy table and row count verification", restoreVerified)
    }
}

/**
 * Mock connection provider supporting runtime operations testing for INFRA-02 Step 03.
 */
class MockRuntimeConnectionProvider : PostgresConnectionProvider {

    var isClosed = false
    var currentSessionProjectId: String = ""
    private val activeCount = AtomicInteger(0)
    private val idleCount = AtomicInteger(0)
    private val totalAcquisitionsCount = java.util.concurrent.atomic.AtomicLong(0)
    private val failureCount = java.util.concurrent.atomic.AtomicLong(0)

    override suspend fun acquireConnection(): java.sql.Connection {
        check(!isClosed) { "Pool is closed" }
        activeCount.incrementAndGet()
        totalAcquisitionsCount.incrementAndGet()
        return java.lang.reflect.Proxy.newProxyInstance(
            java.sql.Connection::class.java.classLoader,
            arrayOf(java.sql.Connection::class.java),
            MockConnectionInvocationHandler()
        ) as java.sql.Connection
    }

    override suspend fun releaseConnection(connection: java.sql.Connection) {
        activeCount.decrementAndGet()
        idleCount.incrementAndGet()
        currentSessionProjectId = ""
    }

    override fun getActiveConnectionCount(): Int = activeCount.get()
    override fun getIdleConnectionCount(): Int = idleCount.get()
    override fun getTotalAcquisitions(): Long = totalAcquisitionsCount.get()
    override fun getAcquisitionFailureCount(): Long = failureCount.get()

    override suspend fun shutdownGracefully(drainTimeoutMs: Long) {
        isClosed = true
        activeCount.set(0)
        idleCount.set(0)
    }

    override fun close() {
        isClosed = true
    }

    private inner class MockConnectionInvocationHandler : java.lang.reflect.InvocationHandler {
        private var inTx = false

        override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any? {
            val name = method.name
            val methodArgs = args ?: emptyArray()

            return when (name) {
                "setAutoCommit" -> {
                    inTx = !(methodArgs[0] as Boolean)
                    null
                }
                "getAutoCommit" -> !inTx
                "commit", "rollback" -> {
                    inTx = false
                    null
                }
                "isClosed" -> isClosed
                "isValid" -> true
                "close" -> null
                "prepareStatement" -> {
                    val sql = methodArgs[0] as String
                    createMockPreparedStatement(sql)
                }
                else -> null
            }
        }
    }

    private fun createMockPreparedStatement(sql: String): java.sql.PreparedStatement {
        val params = mutableListOf<Any?>()

        return java.lang.reflect.Proxy.newProxyInstance(
            java.sql.PreparedStatement::class.java.classLoader,
            arrayOf(java.sql.PreparedStatement::class.java),
            java.lang.reflect.InvocationHandler { _, method, args ->
                val mArgs = args ?: emptyArray()
                when (method.name) {
                    "setString", "setInt", "setLong", "setBigDecimal" -> {
                        val idx = mArgs[0] as Int
                        val v = mArgs[1]
                        while (params.size < idx) params.add(null)
                        params[idx - 1] = v
                        null
                    }
                    "execute" -> {
                        if (sql.contains("set_config")) {
                            currentSessionProjectId = params.getOrNull(0) as? String ?: ""
                        }
                        true
                    }
                    "executeQuery" -> {
                        createMockResultSet(sql)
                    }
                    "executeUpdate" -> 1
                    "close" -> null
                    else -> null
                }
            }
        ) as java.sql.PreparedStatement
    }

    private fun createMockResultSet(sql: String): java.sql.ResultSet {
        val rows = mutableListOf<Map<String, Any?>>()

        if (sql.contains("SELECT current_database()")) {
            rows.add(mapOf("1" to "sucharu_pro_db"))
        } else if (sql.contains("FROM flyway_schema_history")) {
            rows.add(
                mapOf(
                    "version" to "1",
                    "description" to "canonical postgresql schema",
                    "type" to "SQL",
                    "script" to "V1__canonical_postgresql_schema.sql",
                    "checksum" to 12345678,
                    "installed_by" to "sucharu_admin",
                    "installed_on_ms" to System.currentTimeMillis(),
                    "execution_time" to 1500L,
                    "success" to true
                )
            )
            rows.add(
                mapOf(
                    "version" to "20260824",
                    "description" to "add missing indexes and constraints",
                    "type" to "SQL",
                    "script" to "V20260824__add_missing_indexes_and_constraints.sql",
                    "checksum" to 87654321,
                    "installed_by" to "sucharu_admin",
                    "installed_on_ms" to System.currentTimeMillis(),
                    "execution_time" to 400L,
                    "success" to true
                )
            )
        } else if (sql.contains("SELECT COUNT(*)")) {
            rows.add(mapOf("1" to 10L))
        }

        var idx = -1

        return java.lang.reflect.Proxy.newProxyInstance(
            java.sql.ResultSet::class.java.classLoader,
            arrayOf(java.sql.ResultSet::class.java),
            java.lang.reflect.InvocationHandler { _, method, args ->
                val mArgs = args ?: emptyArray()
                when (method.name) {
                    "next" -> {
                        idx++
                        idx < rows.size
                    }
                    "getString" -> {
                        val col = mArgs[0] as? String
                        val colIdx = mArgs[0] as? Int
                        if (col != null) rows[idx][col] as? String
                        else if (colIdx != null && colIdx == 1) rows[idx].values.firstOrNull() as? String
                        else null
                    }
                    "getInt" -> {
                        val col = mArgs[0] as String
                        (rows[idx][col] as? Number)?.toInt() ?: 0
                    }
                    "getLong" -> {
                        val col = mArgs[0] as? String
                        val colIdx = mArgs[0] as? Int
                        if (col != null) (rows[idx][col] as? Number)?.toLong() ?: 0L
                        else if (colIdx != null && colIdx == 1) (rows[idx].values.firstOrNull() as? Number)?.toLong() ?: 0L
                        else 0L
                    }
                    "getBoolean" -> {
                        val col = mArgs[0] as String
                        (rows[idx][col] as? Boolean) ?: false
                    }
                    "close" -> null
                    else -> null
                }
            }
        ) as java.sql.ResultSet
    }
}
