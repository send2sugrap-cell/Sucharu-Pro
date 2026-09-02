package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.persistence.postgres.SqlExecutor
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import kotlinx.coroutines.sync.withLock
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * High-fidelity in-memory simulated PostgreSQL transaction manager for event persistence and integration tests.
 */
class MockPostgresEventDatabase : TransactionManager {

    val eventStoreTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val outboxTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val idempotencyTable = ConcurrentHashMap<String, MutableMap<String, Any?>>()
    val deadLetterTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val integrationDeliveryTable = ConcurrentHashMap<String, MutableMap<String, Any?>>()

    // INFRA-04 Step 04 Background Job Tables
    val backgroundJobsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val jobExecutionsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val jobSchedulesTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val jobDependenciesTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val jobDeadLettersTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()

    // INFRA-04 Step 05 Workflow & Approval Tables
    val workflowDefinitionsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val workflowVersionsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val workflowStepsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val workflowInstancesTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val workflowStepExecutionsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val workflowTransitionsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val workflowCompensationsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val workflowApprovalPoliciesTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val workflowApprovalRequestsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val workflowApprovalDecisionsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val workflowEscalationsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()

    // INFRA-04 Step 07 Notification Security Tables
    val notificationSecurityAuditTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val notificationSuppressionsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val notificationRateLimitTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()

    // INFRA-04 Step 08 AI Agent Notification Tables
    val aiNotificationActionRecordsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val aiNotificationConfirmationsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val aiNotificationAuditTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()

    // INFRA-04 Step 09 Observability Tables
    val operationalAlertsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val sloDefinitionsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val sloMeasurementsTable = CopyOnWriteArrayList<MutableMap<String, Any?>>()

    var currentTenantSetting: String? = null
    var failNextOperation: Boolean = false

    private val dbMutex = kotlinx.coroutines.sync.Mutex()

    override suspend fun <T> inTransaction(
        tenantContext: TenantContext,
        block: suspend (TransactionContext) -> T
    ): T = dbMutex.withLock {
        if (failNextOperation) {
            failNextOperation = false
            throw java.sql.SQLException("Simulated database failure during transaction")
        }

        currentTenantSetting = tenantContext.projectId
        val snapshotEventStore = eventStoreTable.map { HashMap(it) }
        val snapshotOutbox = outboxTable.map { HashMap(it) }
        val snapshotIdempotency = HashMap(idempotencyTable)
        val snapshotDeadLetter = deadLetterTable.map { HashMap(it) }
        val snapshotIntegration = HashMap(integrationDeliveryTable)
        val snapshotBackgroundJobs = backgroundJobsTable.map { HashMap(it) }
        val snapshotWfDefs = workflowDefinitionsTable.map { HashMap(it) }
        val snapshotWfVers = workflowVersionsTable.map { HashMap(it) }
        val snapshotWfSteps = workflowStepsTable.map { HashMap(it) }
        val snapshotWfInst = workflowInstancesTable.map { HashMap(it) }
        val snapshotWfStepExec = workflowStepExecutionsTable.map { HashMap(it) }
        val snapshotWfTrans = workflowTransitionsTable.map { HashMap(it) }
        val snapshotWfComp = workflowCompensationsTable.map { HashMap(it) }
        val snapshotWfAppPol = workflowApprovalPoliciesTable.map { HashMap(it) }
        val snapshotWfAppReq = workflowApprovalRequestsTable.map { HashMap(it) }
        val snapshotWfAppDec = workflowApprovalDecisionsTable.map { HashMap(it) }
        val snapshotWfEsc = workflowEscalationsTable.map { HashMap(it) }
        val snapshotNsAudit = notificationSecurityAuditTable.map { HashMap(it) }
        val snapshotNsSupp = notificationSuppressionsTable.map { HashMap(it) }
        val snapshotNsRateLimit = notificationRateLimitTable.map { HashMap(it) }
        val snapshotAiAction = aiNotificationActionRecordsTable.map { HashMap(it) }
        val snapshotAiConf = aiNotificationConfirmationsTable.map { HashMap(it) }
        val snapshotAiAudit = aiNotificationAuditTable.map { HashMap(it) }
        val snapshotAlerts = operationalAlertsTable.map { HashMap(it) }
        val snapshotSloDefs = sloDefinitionsTable.map { HashMap(it) }
        val snapshotSloMeas = sloMeasurementsTable.map { HashMap(it) }

        val conn = createMockConnection(tenantContext)
        val txContext = TransactionContext(
            tenantContext = tenantContext,
            sqlExecutor = SqlExecutor(conn),
            connection = conn
        )

        try {
            block(txContext)
        } catch (e: Throwable) {
            // Rollback state
            eventStoreTable.clear()
            eventStoreTable.addAll(snapshotEventStore)
            outboxTable.clear()
            outboxTable.addAll(snapshotOutbox)
            idempotencyTable.clear()
            idempotencyTable.putAll(snapshotIdempotency)
            deadLetterTable.clear()
            deadLetterTable.addAll(snapshotDeadLetter)
            integrationDeliveryTable.clear()
            integrationDeliveryTable.putAll(snapshotIntegration)
            backgroundJobsTable.clear()
            backgroundJobsTable.addAll(snapshotBackgroundJobs)
            workflowDefinitionsTable.clear()
            workflowDefinitionsTable.addAll(snapshotWfDefs)
            workflowVersionsTable.clear()
            workflowVersionsTable.addAll(snapshotWfVers)
            workflowStepsTable.clear()
            workflowStepsTable.addAll(snapshotWfSteps)
            workflowInstancesTable.clear()
            workflowInstancesTable.addAll(snapshotWfInst)
            workflowStepExecutionsTable.clear()
            workflowStepExecutionsTable.addAll(snapshotWfStepExec)
            workflowTransitionsTable.clear()
            workflowTransitionsTable.addAll(snapshotWfTrans)
            workflowCompensationsTable.clear()
            workflowCompensationsTable.addAll(snapshotWfComp)
            workflowApprovalPoliciesTable.clear()
            workflowApprovalPoliciesTable.addAll(snapshotWfAppPol)
            workflowApprovalRequestsTable.clear()
            workflowApprovalRequestsTable.addAll(snapshotWfAppReq)
            workflowApprovalDecisionsTable.clear()
            workflowApprovalDecisionsTable.addAll(snapshotWfAppDec)
            workflowEscalationsTable.clear()
            workflowEscalationsTable.addAll(snapshotWfEsc)
            notificationSecurityAuditTable.clear()
            notificationSecurityAuditTable.addAll(snapshotNsAudit)
            notificationSuppressionsTable.clear()
            notificationSuppressionsTable.addAll(snapshotNsSupp)
            notificationRateLimitTable.clear()
            notificationRateLimitTable.addAll(snapshotNsRateLimit)
            aiNotificationActionRecordsTable.clear()
            aiNotificationActionRecordsTable.addAll(snapshotAiAction)
            aiNotificationConfirmationsTable.clear()
            aiNotificationConfirmationsTable.addAll(snapshotAiConf)
            aiNotificationAuditTable.clear()
            aiNotificationAuditTable.addAll(snapshotAiAudit)
            operationalAlertsTable.clear()
            operationalAlertsTable.addAll(snapshotAlerts)
            sloDefinitionsTable.clear()
            sloDefinitionsTable.addAll(snapshotSloDefs)
            sloMeasurementsTable.clear()
            sloMeasurementsTable.addAll(snapshotSloMeas)
            throw e
        }
    }

    override suspend fun <T> inReadOnly(
        tenantContext: TenantContext,
        block: suspend (TransactionContext) -> T
    ): T {
        if (failNextOperation) {
            failNextOperation = false
            throw java.sql.SQLException("Simulated database failure during read-only operation")
        }

        currentTenantSetting = tenantContext.projectId
        val conn = createMockConnection(tenantContext)
        val txContext = TransactionContext(
            tenantContext = tenantContext,
            sqlExecutor = SqlExecutor(conn),
            connection = conn
        )
        return block(txContext)
    }

    suspend fun <T> inSystemTransaction(
        block: suspend (TransactionContext) -> T
    ): T {
        val systemTenant = TenantContext("system")
        return inTransaction(systemTenant, block)
    }

    suspend fun isHealthy(): Boolean = !failNextOperation

    private fun createMockConnection(tenantContext: TenantContext): Connection {
        return Proxy.newProxyInstance(
            Connection::class.java.classLoader,
            arrayOf(Connection::class.java)
        ) { _, method, args ->
            when (method.name) {
                "prepareStatement" -> createMockPreparedStatement(args[0] as String, tenantContext)
                "close" -> null
                "setAutoCommit", "commit", "rollback" -> null
                else -> null
            }
        } as Connection
    }

    private fun createMockPreparedStatement(sql: String, tenantContext: TenantContext): java.sql.PreparedStatement {
        val params = mutableListOf<Any?>()

        return Proxy.newProxyInstance(
            java.sql.PreparedStatement::class.java.classLoader,
            arrayOf(java.sql.PreparedStatement::class.java)
        ) { _, method, args ->
            when {
                method.name.startsWith("set") && args != null && args.isNotEmpty() && args[0] is Int -> {
                    val idx = (args[0] as Int) - 1
                    while (params.size <= idx) params.add(null)
                    params[idx] = if (method.name == "setNull") null else args.getOrNull(1)
                    null
                }
                method.name == "executeUpdate" -> {
                    executeMockUpdate(sql, params, tenantContext)
                }
                method.name == "executeQuery" -> {
                    executeMockQuery(sql, params, tenantContext)
                }
                method.name == "close" -> null
                else -> null
            }
        } as java.sql.PreparedStatement
    }

    private fun executeMockUpdate(sql: String, params: List<Any?>, tenantContext: TenantContext): Int {
        if (sql.contains("INSERT INTO event_store")) {
            val projectId = params[0] as String
            val eventId = params[1] as String
            if (eventStoreTable.any { it["project_id"] == projectId && it["event_id"] == eventId }) {
                throw java.sql.SQLException("duplicate key value violates unique constraint pk_event_store")
            }

            val row = mutableMapOf(
                "project_id" to projectId,
                "event_id" to eventId,
                "event_type" to params[2],
                "event_version" to params[3],
                "occurred_at" to (params[4] ?: Timestamp(System.currentTimeMillis())),
                "published_at" to (params[5] ?: Timestamp(System.currentTimeMillis())),
                "aggregate_type" to params[6],
                "aggregate_id" to params[7],
                "aggregate_version" to params[8],
                "actor_type" to params[9],
                "actor_id" to params[10],
                "principal_type" to params[11],
                "correlation_id" to params[12],
                "causation_id" to params[13],
                "request_id" to params[14],
                "source" to params[15],
                "payload" to params[16],
                "metadata" to params[17]
            )
            eventStoreTable.add(row)
            return 1
        } else if (sql.contains("INSERT INTO event_outbox")) {
            val row = mutableMapOf<String, Any?>(
                "outbox_id" to params[0],
                "project_id" to (params[1] ?: tenantContext.projectId),
                "event_id" to params[2],
                "event_type" to params[3],
                "event_version" to params[4],
                "aggregate_type" to params[5],
                "aggregate_id" to params[6],
                "aggregate_version" to params[7],
                "status" to "PENDING",
                "attempt_count" to 0,
                "available_at" to (params[8] ?: Timestamp(System.currentTimeMillis())),
                "created_at" to (params[9] ?: Timestamp(System.currentTimeMillis())),
                "payload" to params[10],
                "metadata" to params[11],
                "correlation_id" to params[12],
                "causation_id" to params[13],
                "request_id" to params[14],
                "actor_type" to params[15],
                "actor_id" to params[16],
                "principal_type" to params[17],
                "source" to params[18],
                "claimed_by_worker" to null,
                "claimed_at" to null,
                "lease_expires_at" to null,
                "last_attempt_at" to null,
                "next_attempt_at" to null,
                "published_at" to null,
                "last_error_code" to null,
                "last_error_message" to null
            )
            outboxTable.add(row)
            return 1
        } else if (sql.contains("INSERT INTO event_processing_records")) {
            val projectId = params[1] as String
            val consumerId = params[3] as String
            val eventId = params[2] as String
            val key = "$projectId:$consumerId:$eventId"
            val row = mutableMapOf<String, Any?>(
                "processing_id" to params[0],
                "project_id" to projectId,
                "event_id" to eventId,
                "consumer_id" to consumerId,
                "status" to params[4],
                "failure_reason" to params[5],
                "execution_duration_ms" to params[6],
                "processed_at" to params[7]
            )
            idempotencyTable[key] = row
            return 1
        } else if (sql.contains("INSERT INTO event_dead_letters")) {
            val row = mutableMapOf<String, Any?>(
                "dead_letter_id" to params[0],
                "project_id" to (params[1] ?: tenantContext.projectId),
                "outbox_id" to params[2],
                "event_id" to params[3],
                "event_type" to params[4],
                "event_version" to params[5],
                "aggregate_type" to params[6],
                "aggregate_id" to params[7],
                "payload" to params[8],
                "failure_classification" to params[9],
                "error_code" to params[10],
                "error_message" to params[11],
                "attempt_count" to params[12],
                "first_failure_at" to params[13],
                "final_failure_at" to Timestamp(System.currentTimeMillis()),
                "correlation_id" to params[14],
                "causation_id" to params[15],
                "request_id" to params[16],
                "replayed_at" to null,
                "replayed_by" to null,
                "is_resolved" to false,
                "created_at" to Timestamp(System.currentTimeMillis())
            )
            deadLetterTable.add(row)
            return 1
        } else if (sql.contains("INSERT INTO integration_delivery_records")) {
            val projectId = params[1] as String
            val consumerId = params[3] as String
            val eventId = params[2] as String
            val key = "$projectId:$consumerId:$eventId"
            val row = mutableMapOf<String, Any?>(
                "delivery_id" to params[0],
                "project_id" to projectId,
                "event_id" to eventId,
                "consumer_id" to consumerId,
                "integration_type" to params[4],
                "destination" to params[5],
                "status" to params[6],
                "attempt_count" to (params[7] ?: 1),
                "last_attempt_at" to params[8],
                "next_attempt_at" to params[9],
                "delivered_at" to params[10],
                "failure_classification" to params[11],
                "sanitized_error" to params[12],
                "correlation_id" to params[13],
                "request_id" to params[14],
                "created_at" to (params[15] ?: Timestamp(System.currentTimeMillis()))
            )
            integrationDeliveryTable[key] = row
            return 1
        } else if (sql.contains("UPDATE integration_delivery_records") && sql.contains("SET status = 'DELIVERED'")) {
            val deliveryId = params[1] as String
            val row = integrationDeliveryTable.values.firstOrNull { it["delivery_id"] == deliveryId && it["project_id"] == tenantContext.projectId }
            if (row != null) {
                row["status"] = "DELIVERED"
                row["delivered_at"] = Timestamp(System.currentTimeMillis())
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE integration_delivery_records") && sql.contains("SET status = ?")) {
            val deliveryId = params.last() as String
            val row = integrationDeliveryTable.values.firstOrNull { it["delivery_id"] == deliveryId && it["project_id"] == tenantContext.projectId }
            if (row != null) {
                row["status"] = params[0]
                row["failure_classification"] = params[1]
                row["sanitized_error"] = params[2]
                row["next_attempt_at"] = params[3]
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE event_outbox") && sql.contains("SET status = 'PROCESSING'")) {
            val workerId = params[0] as String
            val claimedAt = params[1] as Timestamp
            val leaseExpiresAt = params[2] as Timestamp
            val outboxId = params[params.size - 1] as String

            val row = outboxTable.firstOrNull { it["outbox_id"] == outboxId && it["project_id"] == tenantContext.projectId }
            if (row != null) {
                row["status"] = "PROCESSING"
                row["claimed_by_worker"] = workerId
                row["claimed_at"] = claimedAt
                row["lease_expires_at"] = leaseExpiresAt
                row["last_attempt_at"] = claimedAt
                row["attempt_count"] = ((row["attempt_count"] as? Int) ?: 0) + 1
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE event_outbox") && sql.contains("SET status = 'PUBLISHED'")) {
            val outboxId = params[1] as String
            val row = outboxTable.firstOrNull { it["outbox_id"] == outboxId && it["project_id"] == tenantContext.projectId }
            if (row != null) {
                row["status"] = "PUBLISHED"
                row["published_at"] = Timestamp(System.currentTimeMillis())
                row["claimed_by_worker"] = null
                row["lease_expires_at"] = null
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE event_outbox") && sql.contains("SET status = 'RETRY_SCHEDULED'")) {
            val outboxId = params[params.size - 1] as String
            val row = outboxTable.firstOrNull { it["outbox_id"] == outboxId && it["project_id"] == tenantContext.projectId }
            if (row != null) {
                row["status"] = "RETRY_SCHEDULED"
                row["available_at"] = params.getOrNull(0) ?: Timestamp(System.currentTimeMillis())
                row["next_attempt_at"] = params.getOrNull(1) ?: Timestamp(System.currentTimeMillis())
                row["last_error_code"] = params.getOrNull(2)
                row["last_error_message"] = params.getOrNull(3)
                row["claimed_by_worker"] = null
                row["lease_expires_at"] = null
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE event_outbox") && sql.contains("SET status = 'DEAD_LETTER'")) {
            val outboxId = params[params.size - 1] as String
            val row = outboxTable.firstOrNull { it["outbox_id"] == outboxId && it["project_id"] == tenantContext.projectId }
            if (row != null) {
                row["status"] = "DEAD_LETTER"
                row["last_error_code"] = params[0]
                row["last_error_message"] = params[1]
                row["claimed_by_worker"] = null
                row["lease_expires_at"] = null
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE event_outbox") && sql.contains("SET status = 'CANCELLED'")) {
            val outboxId = params[1] as String
            val row = outboxTable.firstOrNull { it["outbox_id"] == outboxId && it["project_id"] == tenantContext.projectId }
            if (row != null) {
                row["status"] = "CANCELLED"
                row["last_error_message"] = params[0]
                row["claimed_by_worker"] = null
                row["lease_expires_at"] = null
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE event_dead_letters") && sql.contains("SET replayed_at = NOW()")) {
            val deadLetterId = params[2] as String
            val row = deadLetterTable.firstOrNull { it["dead_letter_id"] == deadLetterId && it["project_id"] == tenantContext.projectId }
            if (row != null) {
                row["replayed_at"] = Timestamp(System.currentTimeMillis())
                row["replayed_by"] = params[0] as String
                row["is_resolved"] = true
                return 1
            }
            return 0
        } else if (sql.contains("INSERT INTO background_jobs")) {
            val projectId = params[1] as String
            val jobId = params[0] as String
            val idempotencyKey = params[29] as? String

            if (idempotencyKey != null && backgroundJobsTable.any { it["project_id"] == projectId && it["idempotency_key"] == idempotencyKey }) {
                return 0 // ON CONFLICT DO NOTHING
            }

            val row = mutableMapOf<String, Any?>(
                "job_id" to jobId,
                "project_id" to projectId,
                "job_type" to params[2],
                "job_version" to (params[3] ?: "v1"),
                "trigger_type" to params[4],
                "priority" to (params[5] ?: 3),
                "status" to (params[6] ?: "QUEUED"),
                "attempt_count" to (params[7] ?: 0),
                "max_attempts" to (params[8] ?: 3),
                "scheduled_at" to (params[9] ?: Timestamp(System.currentTimeMillis())),
                "available_at" to (params[10] ?: Timestamp(System.currentTimeMillis())),
                "started_at" to params[11],
                "completed_at" to params[12],
                "next_attempt_at" to params[13],
                "claimed_by_worker" to params[14],
                "claimed_at" to params[15],
                "lease_expires_at" to params[16],
                "payload" to params[17],
                "metadata" to params[18],
                "correlation_id" to params[19],
                "causation_id" to params[20],
                "request_id" to params[21],
                "actor_type" to (params[22] ?: "SYSTEM"),
                "actor_id" to (params[23] ?: "SYSTEM"),
                "principal_type" to (params[24] ?: "SYSTEM"),
                "source" to (params[25] ?: "sucharu-pro-backend"),
                "last_error_code" to params[26],
                "last_error_message" to params[27],
                "failure_classification" to params[28],
                "idempotency_key" to idempotencyKey,
                "created_at" to (params[30] ?: Timestamp(System.currentTimeMillis())),
                "updated_at" to (params[31] ?: Timestamp(System.currentTimeMillis()))
            )
            backgroundJobsTable.add(row)
            return 1
        } else if (sql.contains("UPDATE background_jobs") && sql.contains("SET status = 'CLAIMED'")) {
            val workerId = params[0] as String
            val claimedAt = params[1] as Timestamp
            val leaseExpiresAt = params[2] as Timestamp
            val startedAt = params[3] as Timestamp
            val projectId = params[4] as String
            val jobId = params[5] as String

            val row = backgroundJobsTable.firstOrNull { it["job_id"] == jobId && it["project_id"] == projectId }
            if (row != null) {
                row["status"] = "CLAIMED"
                row["claimed_by_worker"] = workerId
                row["claimed_at"] = claimedAt
                row["lease_expires_at"] = leaseExpiresAt
                row["started_at"] = startedAt
                row["attempt_count"] = ((row["attempt_count"] as? Int) ?: 0) + 1
                row["updated_at"] = Timestamp(System.currentTimeMillis())
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE background_jobs") && sql.contains("SET status = 'SUCCEEDED'")) {
            val projectId = params[0] as String
            val jobId = params[1] as String
            val row = backgroundJobsTable.firstOrNull { it["job_id"] == jobId && it["project_id"] == projectId }
            if (row != null) {
                row["status"] = "SUCCEEDED"
                row["completed_at"] = Timestamp(System.currentTimeMillis())
                row["claimed_by_worker"] = null
                row["lease_expires_at"] = null
                row["updated_at"] = Timestamp(System.currentTimeMillis())
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE background_jobs") && sql.contains("last_error_code = ?") && sql.contains("status = ?")) {
            val status = params[0] as String
            val errorCode = params[1] as? String
            val errorMessage = params[2] as? String
            val failureClass = params[3] as? String
            val nextAttempt = params[4] as? Timestamp
            val availAt = params[5] as? Timestamp
            val projectId = params[6] as String
            val jobId = params[7] as String

            val row = backgroundJobsTable.firstOrNull { it["job_id"] == jobId && it["project_id"] == projectId }
            if (row != null) {
                row["status"] = status
                row["last_error_code"] = errorCode
                row["last_error_message"] = errorMessage
                row["failure_classification"] = failureClass
                row["next_attempt_at"] = nextAttempt
                row["available_at"] = availAt ?: Timestamp(System.currentTimeMillis())
                row["claimed_by_worker"] = null
                row["lease_expires_at"] = null
                row["updated_at"] = Timestamp(System.currentTimeMillis())
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE background_jobs") && sql.contains("SET status = 'DEAD_LETTER'")) {
            val errorCode = params[0] as? String
            val errorMessage = params[1] as? String
            val failureClass = params[2] as? String
            val projectId = params[3] as String
            val jobId = params[4] as String

            val row = backgroundJobsTable.firstOrNull { it["job_id"] == jobId && it["project_id"] == projectId }
            if (row != null) {
                row["status"] = "DEAD_LETTER"
                row["last_error_code"] = errorCode
                row["last_error_message"] = errorMessage
                row["failure_classification"] = failureClass
                row["claimed_by_worker"] = null
                row["lease_expires_at"] = null
                row["completed_at"] = Timestamp(System.currentTimeMillis())
                row["updated_at"] = Timestamp(System.currentTimeMillis())
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE background_jobs") && sql.contains("SET status = 'CANCELLED'")) {
            val reason = params[0] as? String
            val projectId = params[1] as String
            val jobId = params[2] as String

            val row = backgroundJobsTable.firstOrNull { it["job_id"] == jobId && it["project_id"] == projectId }
            if (row != null) {
                row["status"] = "CANCELLED"
                row["last_error_message"] = reason
                row["claimed_by_worker"] = null
                row["lease_expires_at"] = null
                row["completed_at"] = Timestamp(System.currentTimeMillis())
                row["updated_at"] = Timestamp(System.currentTimeMillis())
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE background_jobs") && sql.contains("lease_expires_at < NOW()")) {
            val projectId = params[0] as String
            var count = 0
            val now = Timestamp(System.currentTimeMillis())
            for (row in backgroundJobsTable) {
                if (row["project_id"] == projectId && (row["status"] == "CLAIMED" || row["status"] == "RUNNING")) {
                    val leaseExp = row["lease_expires_at"] as? Timestamp
                    if (leaseExp != null && leaseExp.before(now)) {
                        row["status"] = "RETRY_SCHEDULED"
                        row["claimed_by_worker"] = null
                        row["claimed_at"] = null
                        row["lease_expires_at"] = null
                        row["available_at"] = now
                        row["updated_at"] = now
                        count++
                    }
                }
            }
            return count
        } else if (sql.contains("INSERT INTO job_executions")) {
            val row = mutableMapOf<String, Any?>(
                "execution_id" to params[0],
                "project_id" to params[1],
                "job_id" to params[2],
                "worker_id" to params[3],
                "attempt_number" to params[4],
                "started_at" to params[5],
                "completed_at" to params[6],
                "duration_ms" to params[7],
                "status" to params[8],
                "error_code" to params[9],
                "error_message" to params[10],
                "failure_classification" to params[11],
                "output_metadata" to params[12],
                "created_at" to params[13]
            )
            jobExecutionsTable.add(row)
            return 1
        } else if (sql.contains("INSERT INTO job_schedules")) {
            val scheduleId = params[0] as String
            val projectId = params[1] as String
            val existing = jobSchedulesTable.firstOrNull { it["schedule_id"] == scheduleId && it["project_id"] == projectId }
            if (existing != null) {
                existing["job_type"] = params[2]
                existing["cron_expression"] = params[3]
                existing["fixed_interval_ms"] = params[4]
                existing["timezone"] = params[5]
                existing["is_enabled"] = params[6]
                existing["payload"] = params[7]
                existing["last_run_at"] = params[8]
                existing["next_run_at"] = params[9]
                existing["schedule_version"] = params[10]
                existing["updated_at"] = Timestamp(System.currentTimeMillis())
            } else {
                val row = mutableMapOf<String, Any?>(
                    "schedule_id" to scheduleId,
                    "project_id" to projectId,
                    "job_type" to params[2],
                    "cron_expression" to params[3],
                    "fixed_interval_ms" to params[4],
                    "timezone" to params[5],
                    "is_enabled" to params[6],
                    "payload" to params[7],
                    "last_run_at" to params[8],
                    "next_run_at" to params[9],
                    "schedule_version" to params[10],
                    "created_at" to params[11],
                    "updated_at" to params[12]
                )
                jobSchedulesTable.add(row)
            }
            return 1
        } else if (sql.contains("UPDATE job_schedules") && sql.contains("SET last_run_at = ?")) {
            val lastRun = params[0] as Timestamp
            val nextRun = params[1] as Timestamp
            val projectId = params[2] as String
            val scheduleId = params[3] as String
            val row = jobSchedulesTable.firstOrNull { it["schedule_id"] == scheduleId && it["project_id"] == projectId }
            if (row != null) {
                row["last_run_at"] = lastRun
                row["next_run_at"] = nextRun
                row["updated_at"] = Timestamp(System.currentTimeMillis())
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE job_schedules") && sql.contains("SET is_enabled = ?")) {
            val enabled = params[0] as Boolean
            val projectId = params[1] as String
            val scheduleId = params[2] as String
            val row = jobSchedulesTable.firstOrNull { it["schedule_id"] == scheduleId && it["project_id"] == projectId }
            if (row != null) {
                row["is_enabled"] = enabled
                row["updated_at"] = Timestamp(System.currentTimeMillis())
                return 1
            }
            return 0
        } else if (sql.contains("INSERT INTO job_dependencies")) {
            val dependencyId = params[0] as String
            val projectId = params[1] as String
            val jobId = params[2] as String
            val dependsOn = params[3] as String
            val req = params[4] as String
            val isSatisfied = params[5] as Boolean
            val created = params[6] as Timestamp

            val existing = jobDependenciesTable.firstOrNull { it["project_id"] == projectId && it["job_id"] == jobId && it["depends_on_job_id"] == dependsOn }
            if (existing != null) {
                existing["required_status"] = req
            } else {
                val row = mutableMapOf<String, Any?>(
                    "dependency_id" to dependencyId,
                    "project_id" to projectId,
                    "job_id" to jobId,
                    "depends_on_job_id" to dependsOn,
                    "required_status" to req,
                    "is_satisfied" to isSatisfied,
                    "created_at" to created
                )
                jobDependenciesTable.add(row)
            }
            return 1
        } else if (sql.contains("UPDATE job_dependencies") && sql.contains("SET is_satisfied = TRUE")) {
            val projectId = params[0] as String
            val dependencyId = params[1] as String
            val row = jobDependenciesTable.firstOrNull { it["dependency_id"] == dependencyId && it["project_id"] == projectId }
            if (row != null) {
                row["is_satisfied"] = true
                return 1
            }
            return 0
        } else if (sql.contains("INSERT INTO job_dead_letters")) {
            val row = mutableMapOf<String, Any?>(
                "dead_letter_id" to params[0],
                "project_id" to params[1],
                "job_id" to params[2],
                "job_type" to params[3],
                "payload" to params[4],
                "metadata" to params[5],
                "attempt_count" to params[6],
                "failure_classification" to params[7],
                "error_code" to params[8],
                "error_message" to params[9],
                "first_failure_at" to params[10],
                "final_failure_at" to params[11],
                "correlation_id" to params[12],
                "causation_id" to params[13],
                "request_id" to params[14],
                "is_resolved" to params[15],
                "replayed_at" to params[16],
                "replayed_by" to params[17],
                "created_at" to params[18]
            )
            jobDeadLettersTable.add(row)
            return 1
        } else if (sql.contains("UPDATE job_dead_letters") && sql.contains("SET is_resolved = TRUE") && sql.contains("replayed_at")) {
            val replayedBy = params[0] as String
            val projectId = params[1] as String
            val deadLetterId = params[2] as String
            val row = jobDeadLettersTable.firstOrNull { it["dead_letter_id"] == deadLetterId && it["project_id"] == projectId }
            if (row != null) {
                row["is_resolved"] = true
                row["replayed_at"] = Timestamp(System.currentTimeMillis())
                row["replayed_by"] = replayedBy
                return 1
            }
            return 0
        } else if (sql.contains("UPDATE job_dead_letters") && sql.contains("SET is_resolved = TRUE")) {
            val projectId = params[0] as String
        } else if (sql.contains("INSERT INTO workflow_definitions")) {
            val defId = params[0] as String
            val projectId = params[1] as String
            val row = mutableMapOf<String, Any?>(
                "definition_id" to defId,
                "project_id" to projectId,
                "workflow_name" to params[2],
                "description" to params[3],
                "is_active" to params[4],
                "created_by" to params[5],
                "created_at" to params[6],
                "updated_at" to params[7]
            )
            workflowDefinitionsTable.removeIf { it["definition_id"] == defId && it["project_id"] == projectId }
            workflowDefinitionsTable.add(row)
            return 1
        } else if (sql.contains("INSERT INTO workflow_versions")) {
            val defId = params[0] as String
            val projectId = params[1] as String
            val verId = params[2] as String
            val row = mutableMapOf<String, Any?>(
                "definition_id" to defId,
                "project_id" to projectId,
                "version_id" to verId,
                "definition_json" to params[3],
                "is_active" to params[4],
                "published_at" to params[5],
                "published_by" to params[6]
            )
            workflowVersionsTable.removeIf { it["definition_id"] == defId && it["project_id"] == projectId && it["version_id"] == verId }
            workflowVersionsTable.add(row)
            return 1
        } else if (sql.contains("INSERT INTO workflow_steps")) {
            val stepId = params[0] as String
            val projectId = params[1] as String
            val defId = params[2] as String
            val verId = params[3] as String
            val row = mutableMapOf<String, Any?>(
                "step_id" to stepId,
                "project_id" to projectId,
                "definition_id" to defId,
                "version_id" to verId,
                "step_name" to params[4],
                "step_type" to params[5],
                "sequence_order" to params[6],
                "config_json" to params[7],
                "retry_policy_json" to params[8],
                "timeout_ms" to params[9],
                "compensation_step_id" to params[10],
                "required_capability" to params[11]
            )
            workflowStepsTable.removeIf { it["step_id"] == stepId && it["project_id"] == projectId && it["definition_id"] == defId && it["version_id"] == verId }
            workflowStepsTable.add(row)
            return 1
        } else if (sql.contains("INSERT INTO workflow_instances")) {
            val wfId = params[0] as String
            val projectId = params[1] as String
            val exists = workflowInstancesTable.any { it["workflow_id"] == wfId && it["project_id"] == projectId }
            if (exists) return 0
            val row = mutableMapOf<String, Any?>(
                "workflow_id" to wfId,
                "project_id" to projectId,
                "definition_id" to params[2],
                "version_id" to params[3],
                "execution_id" to params[4],
                "status" to params[5],
                "current_step_id" to params[6],
                "context_json" to params[7],
                "correlation_id" to params[8],
                "causation_id" to params[9],
                "request_id" to params[10],
                "actor_type" to params[11],
                "actor_id" to params[12],
                "principal_type" to params[13],
                "idempotency_key" to params[14],
                "created_at" to params[15],
                "updated_at" to params[16],
                "completed_at" to params[17],
                "failed_at" to params[18],
                "error_message" to params[19]
            )
            workflowInstancesTable.add(row)
            return 1
        } else if (sql.contains("UPDATE workflow_instances SET")) {
            val status = params[0] as String
            val currentStepId = params[1] as? String
            val contextJson = params[2] as String
            val updatedAt = params[3] as Timestamp
            val completedAt = params[4] as? Timestamp
            val failedAt = params[5] as? Timestamp
            val errorMessage = params[6] as? String
            val projectId = params[7] as String
            val wfId = params[8] as String

            val row = workflowInstancesTable.firstOrNull { it["workflow_id"] == wfId && it["project_id"] == projectId }
            if (row != null) {
                row["status"] = status
                row["current_step_id"] = currentStepId
                row["context_json"] = contextJson
                row["updated_at"] = updatedAt
                row["completed_at"] = completedAt
                row["failed_at"] = failedAt
                row["error_message"] = errorMessage
                return 1
            }
            return 0
        } else if (sql.contains("INSERT INTO workflow_step_executions")) {
            val stepExecId = params[0] as String
            val projectId = params[1] as String
            val row = mutableMapOf<String, Any?>(
                "step_execution_id" to stepExecId,
                "project_id" to projectId,
                "workflow_id" to params[2],
                "execution_id" to params[3],
                "step_id" to params[4],
                "step_name" to params[5],
                "step_type" to params[6],
                "status" to params[7],
                "attempt_number" to params[8],
                "input_json" to params[9],
                "output_json" to params[10],
                "error_message" to params[11],
                "failure_classification" to params[12],
                "started_at" to params[13],
                "completed_at" to params[14]
            )
            workflowStepExecutionsTable.removeIf { it["step_execution_id"] == stepExecId && it["project_id"] == projectId }
            workflowStepExecutionsTable.add(row)
            return 1
        } else if (sql.contains("INSERT INTO workflow_transitions")) {
            val row = mutableMapOf<String, Any?>(
                "transition_id" to params[0],
                "project_id" to params[1],
                "workflow_id" to params[2],
                "execution_id" to params[3],
                "from_status" to params[4],
                "to_status" to params[5],
                "trigger_type" to params[6],
                "actor_type" to params[7],
                "actor_id" to params[8],
                "principal_type" to params[9],
                "metadata_json" to params[10],
                "transitioned_at" to params[11]
            )
            workflowTransitionsTable.add(row)
            return 1
        } else if (sql.contains("INSERT INTO workflow_compensations")) {
            val compId = params[0] as String
            val projectId = params[1] as String
            val row = mutableMapOf<String, Any?>(
                "compensation_id" to compId,
                "project_id" to projectId,
                "workflow_id" to params[2],
                "step_id" to params[3],
                "step_execution_id" to params[4],
                "status" to params[5],
                "attempt_number" to params[6],
                "payload_json" to params[7],
                "result_message" to params[8],
                "error_message" to params[9],
                "started_at" to params[10],
                "completed_at" to params[11]
            )
            workflowCompensationsTable.removeIf { it["compensation_id"] == compId && it["project_id"] == projectId }
            workflowCompensationsTable.add(row)
            return 1
        } else if (sql.contains("INSERT INTO workflow_approval_policies")) {
            val policyId = params[0] as String
            val projectId = params[1] as String
            val row = mutableMapOf<String, Any?>(
                "policy_id" to policyId,
                "project_id" to projectId,
                "policy_name" to params[2],
                "required_role" to params[3],
                "required_capability" to params[4],
                "minimum_approvals" to params[5],
                "allow_self_approval" to params[6],
                "timeout_ms" to params[7],
                "escalation_role" to params[8],
                "created_at" to params[9]
            )
            workflowApprovalPoliciesTable.removeIf { it["policy_id"] == policyId && it["project_id"] == projectId }
            workflowApprovalPoliciesTable.add(row)
            return 1
        } else if (sql.contains("INSERT INTO workflow_approval_requests")) {
            val approvalId = params[0] as String
            val projectId = params[1] as String
            val exists = workflowApprovalRequestsTable.any { it["approval_id"] == approvalId && it["project_id"] == projectId }
            if (exists) return 0
            val row = mutableMapOf<String, Any?>(
                "approval_id" to approvalId,
                "project_id" to projectId,
                "workflow_id" to params[2],
                "step_id" to params[3],
                "policy_id" to params[4],
                "requester_id" to params[5],
                "requester_role" to params[6],
                "requester_principal_type" to params[7],
                "status" to params[8],
                "title" to params[9],
                "summary" to params[10],
                "payload_json" to params[11],
                "expires_at" to params[12],
                "created_at" to params[13],
                "updated_at" to params[14]
            )
            workflowApprovalRequestsTable.add(row)
            return 1
        } else if (sql.contains("UPDATE workflow_approval_requests SET")) {
            val status = params[0] as String
            val updatedAt = params[1] as Timestamp
            val projectId = params[2] as String
            val approvalId = params[3] as String
            val row = workflowApprovalRequestsTable.firstOrNull { it["approval_id"] == approvalId && it["project_id"] == projectId }
            if (row != null) {
                row["status"] = status
                row["updated_at"] = updatedAt
                return 1
            }
            return 0
        } else if (sql.contains("INSERT INTO workflow_approval_decisions")) {
            val row = mutableMapOf<String, Any?>(
                "decision_id" to params[0],
                "project_id" to params[1],
                "approval_id" to params[2],
                "approver_id" to params[3],
                "approver_role" to params[4],
                "approver_principal_type" to params[5],
                "decision_type" to params[6],
                "notes" to params[7],
                "human_confirmation_json" to params[8],
                "decided_at" to params[9]
            )
            workflowApprovalDecisionsTable.add(row)
            return 1
        } else if (sql.contains("INSERT INTO workflow_escalations")) {
            val row = mutableMapOf<String, Any?>(
                "escalation_id" to params[0],
                "project_id" to params[1],
                "approval_id" to params[2],
                "workflow_id" to params[3],
                "from_role" to params[4],
                "to_role" to params[5],
                "reason" to params[6],
                "escalated_at" to params[7]
            )
            workflowEscalationsTable.add(row)
            return 1
        }
        return 0
    }

    private fun executeMockQuery(sql: String, params: List<Any?>, tenantContext: TenantContext): ResultSet {
        val results = mutableListOf<Map<String, Any?>>()

        if (sql.contains("SELECT job_id FROM background_jobs") && sql.contains("FOR UPDATE SKIP LOCKED")) {
            val limit = (params.lastOrNull() as? Int) ?: 10
            val now = Timestamp(System.currentTimeMillis())
            val eligible = backgroundJobsTable.filter {
                it["project_id"] == tenantContext.projectId &&
                        (it["status"] == "QUEUED" || it["status"] == "RETRY_SCHEDULED") &&
                        ((it["available_at"] as? Timestamp)?.before(now) == true || (it["available_at"] as? Timestamp) == now)
            }.sortedWith(compareBy({ (it["priority"] as? Int) ?: 3 }, { (it["available_at"] as? Timestamp)?.time ?: 0L }, { (it["created_at"] as? Timestamp)?.time ?: 0L }))
            .take(limit)
            results.addAll(eligible.map { mapOf("job_id" to it["job_id"]) })
        } else if (sql.contains("FROM background_jobs")) {
            var filtered = backgroundJobsTable.filter { it["project_id"] == tenantContext.projectId }
            if (sql.contains("job_id = ?")) {
                val jobId = params.last() as String
                filtered = filtered.filter { it["job_id"] == jobId }
            } else if (sql.contains("idempotency_key = ?")) {
                val key = params.last() as String
                filtered = filtered.filter { it["idempotency_key"] == key }
            } else if (sql.contains("status IN ('QUEUED', 'RETRY_SCHEDULED', 'WAITING')")) {
                filtered = filtered.filter { it["status"] == "QUEUED" || it["status"] == "RETRY_SCHEDULED" || it["status"] == "WAITING" }
            }
            results.addAll(filtered)
        } else if (sql.contains("FROM job_executions")) {
            val jobId = params.last() as String
            val filtered = jobExecutionsTable.filter { it["project_id"] == tenantContext.projectId && it["job_id"] == jobId }
                .sortedBy { (it["attempt_number"] as? Int) ?: 1 }
            results.addAll(filtered)
        } else if (sql.contains("FROM job_schedules")) {
            var filtered = jobSchedulesTable.filter { it["project_id"] == tenantContext.projectId }
            if (sql.contains("schedule_id = ?")) {
                val id = params.last() as String
                filtered = filtered.filter { it["schedule_id"] == id }
            } else if (sql.contains("is_enabled = TRUE AND next_run_at <= NOW()")) {
                val now = Timestamp(System.currentTimeMillis())
                filtered = filtered.filter { (it["is_enabled"] == true) && ((it["next_run_at"] as? Timestamp)?.before(now) == true || (it["next_run_at"] as? Timestamp) == now) }
                    .sortedBy { (it["next_run_at"] as? Timestamp)?.time ?: 0L }
            }
            results.addAll(filtered)
        } else if (sql.contains("FROM job_dependencies")) {
            if (sql.contains("depends_on_job_id = ?")) {
                val parentId = params.last() as String
                results.addAll(jobDependenciesTable.filter { it["project_id"] == tenantContext.projectId && it["depends_on_job_id"] == parentId })
            } else if (sql.contains("job_id = ?")) {
                val childId = params.last() as String
                results.addAll(jobDependenciesTable.filter { it["project_id"] == tenantContext.projectId && it["job_id"] == childId })
            }
        } else if (sql.contains("FROM job_dead_letters")) {
            var filtered = jobDeadLettersTable.filter { it["project_id"] == tenantContext.projectId }
            if (sql.contains("dead_letter_id = ?")) {
                val id = params.last() as String
                filtered = filtered.filter { it["dead_letter_id"] == id }
            } else if (sql.contains("is_resolved = FALSE")) {
                filtered = filtered.filter { it["is_resolved"] == false }
                    .sortedByDescending { (it["final_failure_at"] as? Timestamp)?.time ?: 0L }
            }
            results.addAll(filtered)
        } else if (sql.contains("FROM event_store")) {
            var filtered = eventStoreTable.filter { it["project_id"] == tenantContext.projectId }
            if (sql.contains("event_id = ?")) {
                val eventId = params.last() as String
                filtered = filtered.filter { it["event_id"] == eventId }
            } else if (sql.contains("aggregate_type = ?") && sql.contains("aggregate_id = ?")) {
                val aggType = params[1] as String
                val aggId = params[2] as String
                filtered = filtered.filter { it["aggregate_type"] == aggType && it["aggregate_id"] == aggId }
            } else if (sql.contains("correlation_id = ?")) {
                val corrId = params[1] as String
                filtered = filtered.filter { it["correlation_id"] == corrId }
            } else if (sql.contains("causation_id = ?")) {
                val causId = params[1] as String
                filtered = filtered.filter { it["causation_id"] == causId }
            } else if (sql.contains("event_type = ?")) {
                val evtType = params[1] as String
                filtered = filtered.filter { it["event_type"] == evtType }
            }
            results.addAll(filtered)
        } else if (sql.contains("SELECT outbox_id FROM event_outbox") && sql.contains("FOR UPDATE SKIP LOCKED")) {
            val limit = (params.lastOrNull() as? Int) ?: 10
            val eligible = outboxTable.filter {
                it["project_id"] == tenantContext.projectId &&
                        (it["status"] == "PENDING" || it["status"] == "RETRY_SCHEDULED" ||
                                (it["status"] == "PROCESSING" && (it["lease_expires_at"] as? Timestamp)?.before(Timestamp(System.currentTimeMillis())) == true))
            }.take(limit)
            results.addAll(eligible.map { mapOf("outbox_id" to it["outbox_id"]) })
        } else if (sql.contains("FROM event_outbox")) {
            var filtered = outboxTable.filter { it["project_id"] == tenantContext.projectId }
            if (sql.contains("outbox_id = ?")) {
                val outboxId = params.last() as String
                filtered = filtered.filter { it["outbox_id"] == outboxId }
            } else if (sql.contains("status IN ('PENDING', 'RETRY_SCHEDULED')")) {
                filtered = filtered.filter { it["status"] == "PENDING" || it["status"] == "RETRY_SCHEDULED" }
            }
            results.addAll(filtered)
        } else if (sql.contains("FROM event_processing_records")) {
            val consumerId = params[1] as String
            val eventId = params[2] as String
            val key = "${tenantContext.projectId}:$consumerId:$eventId"
            val record = idempotencyTable[key]
            if (record != null) {
                if (sql.contains("SELECT 1 FROM")) {
                    if (record["status"] == "PROCESSED") {
                        results.add(mapOf("1" to 1, "processing_id" to record["processing_id"]))
                    }
                } else {
                    results.add(record)
                }
            }
        } else if (sql.contains("FROM event_dead_letters")) {
            var filtered = deadLetterTable.filter { it["project_id"] == tenantContext.projectId }
            if (sql.contains("dead_letter_id = ?")) {
                val id = params.last() as String
                filtered = filtered.filter { it["dead_letter_id"] == id }
            } else if (sql.contains("is_resolved = FALSE")) {
                filtered = filtered.filter { it["is_resolved"] == false }
            }
            results.addAll(filtered)
        } else if (sql.contains("FROM integration_delivery_records")) {
            val consumerId = params[1] as String
            val eventId = params[2] as String
            val key = "${tenantContext.projectId}:$consumerId:$eventId"
            val record = integrationDeliveryTable[key]
            if (record != null) {
                results.add(record)
            }
        } else if (sql.contains("FROM workflow_definitions")) {
            var filtered = workflowDefinitionsTable.filter { it["project_id"] == tenantContext.projectId }
            if (sql.contains("definition_id = ?")) {
                val defId = params.last() as String
                filtered = filtered.filter { it["definition_id"] == defId }
            }
            results.addAll(filtered)
        } else if (sql.contains("FROM workflow_versions")) {
            var filtered = workflowVersionsTable.filter { it["project_id"] == tenantContext.projectId }
            if (sql.contains("definition_id = ?")) {
                val defId = params[1] as String
                filtered = filtered.filter { it["definition_id"] == defId }
            }
            if (sql.contains("version_id = ?")) {
                val verId = params.last() as String
                filtered = filtered.filter { it["version_id"] == verId }
            }
            results.addAll(filtered)
        } else if (sql.contains("FROM workflow_steps")) {
            var filtered = workflowStepsTable.filter { it["project_id"] == tenantContext.projectId }
            if (sql.contains("definition_id = ?")) {
                val defId = params[1] as String
                filtered = filtered.filter { it["definition_id"] == defId }
            }
            if (sql.contains("version_id = ?")) {
                val verId = params.last() as String
                filtered = filtered.filter { it["version_id"] == verId }
            }
            results.addAll(filtered.sortedBy { (it["sequence_order"] as? Int) ?: 0 })
        } else if (sql.contains("FROM workflow_instances")) {
            var filtered = workflowInstancesTable.filter { it["project_id"] == tenantContext.projectId }
            if (sql.contains("workflow_id = ?")) {
                val wfId = params.last() as String
                filtered = filtered.filter { it["workflow_id"] == wfId }
            } else if (sql.contains("status = ?")) {
                val status = params[1] as String
                filtered = filtered.filter { it["status"] == status }
            } else if (sql.contains("idempotency_key = ?")) {
                val key = params.last() as String
                filtered = filtered.filter { it["idempotency_key"] == key }
            }
            results.addAll(filtered)
        } else if (sql.contains("FROM workflow_step_executions")) {
            val wfId = params.last() as String
            results.addAll(workflowStepExecutionsTable.filter { it["project_id"] == tenantContext.projectId && it["workflow_id"] == wfId }.sortedBy { (it["started_at"] as? Timestamp)?.time ?: 0L })
        } else if (sql.contains("FROM workflow_transitions")) {
            val wfId = params.last() as String
            results.addAll(workflowTransitionsTable.filter { it["project_id"] == tenantContext.projectId && it["workflow_id"] == wfId }.sortedBy { (it["transitioned_at"] as? Timestamp)?.time ?: 0L })
        } else if (sql.contains("FROM workflow_compensations")) {
            val wfId = params.last() as String
            results.addAll(workflowCompensationsTable.filter { it["project_id"] == tenantContext.projectId && it["workflow_id"] == wfId }.sortedBy { (it["started_at"] as? Timestamp)?.time ?: 0L })
        } else if (sql.contains("FROM workflow_approval_policies")) {
            val policyId = params.last() as String
            results.addAll(workflowApprovalPoliciesTable.filter { it["project_id"] == tenantContext.projectId && it["policy_id"] == policyId })
        } else if (sql.contains("FROM workflow_approval_requests")) {
            var filtered = workflowApprovalRequestsTable.filter { it["project_id"] == tenantContext.projectId }
            if (sql.contains("approval_id = ?")) {
                val id = params.last() as String
                filtered = filtered.filter { it["approval_id"] == id }
            } else if (sql.contains("status IN ('PENDING', 'ESCALATED')")) {
                filtered = filtered.filter { it["status"] == "PENDING" || it["status"] == "ESCALATED" }
            }
            results.addAll(filtered)
        } else if (sql.contains("FROM workflow_approval_decisions")) {
            val approvalId = params.last() as String
            results.addAll(workflowApprovalDecisionsTable.filter { it["project_id"] == tenantContext.projectId && it["approval_id"] == approvalId }.sortedBy { (it["decided_at"] as? Timestamp)?.time ?: 0L })
        } else if (sql.contains("FROM workflow_escalations")) {
            val approvalId = params.last() as String
            results.addAll(workflowEscalationsTable.filter { it["project_id"] == tenantContext.projectId && it["approval_id"] == approvalId })
        }

        var cursor = -1
        return Proxy.newProxyInstance(
            ResultSet::class.java.classLoader,
            arrayOf(ResultSet::class.java)
        ) { _, method, args ->
            when (method.name) {
                "next" -> {
                    cursor++
                    cursor < results.size
                }
                "getString" -> {
                    val raw = getColValue(results, cursor, args[0])
                    raw?.toString()
                }
                "getLong" -> {
                    val raw = getColValue(results, cursor, args[0])
                    (raw as? Number)?.toLong() ?: 0L
                }
                "getInt" -> {
                    val raw = getColValue(results, cursor, args[0])
                    (raw as? Number)?.toInt() ?: if (raw != null) 1 else 0
                }
                "getBoolean" -> {
                    val raw = getColValue(results, cursor, args[0])
                    raw as? Boolean ?: false
                }
                "getTimestamp" -> {
                    val raw = getColValue(results, cursor, args[0])
                    raw as? Timestamp
                }
                "close" -> null
                else -> null
            }
        } as ResultSet
    }

    private fun getColValue(results: List<Map<String, Any?>>, cursor: Int, arg: Any?): Any? {
        if (cursor < 0 || cursor >= results.size) return null
        val row = results[cursor]
        return when (arg) {
            is String -> row[arg]
            is Number -> {
                val idx = arg.toInt() - 1
                val values = row.values.toList()
                if (idx in values.indices) values[idx] else row.values.firstOrNull()
            }
            else -> null
        }
    }

    fun clear() {
        eventStoreTable.clear()
        outboxTable.clear()
        idempotencyTable.clear()
        deadLetterTable.clear()
        integrationDeliveryTable.clear()
        backgroundJobsTable.clear()
        jobExecutionsTable.clear()
        jobSchedulesTable.clear()
        jobDependenciesTable.clear()
        jobDeadLettersTable.clear()
        workflowDefinitionsTable.clear()
        workflowVersionsTable.clear()
        workflowStepsTable.clear()
        workflowInstancesTable.clear()
        workflowStepExecutionsTable.clear()
        workflowTransitionsTable.clear()
        workflowCompensationsTable.clear()
        workflowApprovalPoliciesTable.clear()
        workflowApprovalRequestsTable.clear()
        workflowApprovalDecisionsTable.clear()
        workflowEscalationsTable.clear()
        notificationSecurityAuditTable.clear()
        notificationSuppressionsTable.clear()
        notificationRateLimitTable.clear()
        aiNotificationActionRecordsTable.clear()
        aiNotificationConfirmationsTable.clear()
        aiNotificationAuditTable.clear()
        operationalAlertsTable.clear()
        sloDefinitionsTable.clear()
        sloMeasurementsTable.clear()
    }
}
