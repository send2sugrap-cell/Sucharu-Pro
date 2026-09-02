package com.sucharu.sucharupro.data.job.postgres

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.job.model.JobScheduleDefinition
import com.sucharu.sucharupro.domain.job.model.ScheduleType
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Interface for recurring schedule persistence (INFRA-04 Step 04).
 */
interface JobScheduleRepository {
    suspend fun saveSchedule(schedule: JobScheduleDefinition, tenantContext: TenantContext)
    suspend fun getScheduleById(scheduleId: String, tenantContext: TenantContext): JobScheduleDefinition?
    suspend fun getDueSchedules(limit: Int = 10, tenantContext: TenantContext): List<JobScheduleDefinition>
    suspend fun updateScheduleRunTimes(
        scheduleId: String,
        lastRunAt: Long,
        nextRunAt: Long,
        tenantContext: TenantContext
    )
    suspend fun setScheduleEnabled(scheduleId: String, isEnabled: Boolean, tenantContext: TenantContext)
}

/**
 * PostgreSQL implementation of [JobScheduleRepository] with multi-tenant RLS.
 */
class PostgresJobScheduleRepository(
    private val transactionManager: TransactionManager
) : JobScheduleRepository {

    private fun mapRowToSchedule(rs: ResultSet): JobScheduleDefinition {
        val cron = rs.getString("cron_expression")
        val interval = rs.getLong("fixed_interval_ms")
        val intervalMs = if (interval > 0) interval else null
        val scheduleType = if (!cron.isNullOrBlank()) ScheduleType.CRON else ScheduleType.FIXED_INTERVAL

        return JobScheduleDefinition(
            scheduleId = rs.getString("schedule_id"),
            projectId = rs.getString("project_id"),
            jobType = rs.getString("job_type"),
            scheduleType = scheduleType,
            cronExpression = cron,
            fixedIntervalMs = intervalMs,
            timezone = rs.getString("timezone"),
            isEnabled = rs.getBoolean("is_enabled"),
            payloadJson = rs.getString("payload") ?: "{}",
            lastRunAt = rs.getTimestamp("last_run_at")?.time,
            nextRunAt = rs.getTimestamp("next_run_at").time,
            scheduleVersion = rs.getString("schedule_version") ?: "v1",
            createdAt = rs.getTimestamp("created_at").time,
            updatedAt = rs.getTimestamp("updated_at").time
        )
    }

    override suspend fun saveSchedule(schedule: JobScheduleDefinition, tenantContext: TenantContext) {
        require(schedule.projectId == tenantContext.projectId) {
            "Tenant isolation mismatch: schedule projectId '${schedule.projectId}' != tenant '${tenantContext.projectId}'"
        }

        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                INSERT INTO job_schedules (
                    schedule_id, project_id, job_type, cron_expression, fixed_interval_ms,
                    timezone, is_enabled, payload, last_run_at, next_run_at,
                    schedule_version, created_at, updated_at
                ) VALUES (
                    ?, ?, ?, ?, ?,
                    ?, ?, ?::jsonb, ?, ?,
                    ?, ?, ?
                )
                ON CONFLICT (project_id, schedule_id) DO UPDATE
                SET job_type = EXCLUDED.job_type,
                    cron_expression = EXCLUDED.cron_expression,
                    fixed_interval_ms = EXCLUDED.fixed_interval_ms,
                    timezone = EXCLUDED.timezone,
                    is_enabled = EXCLUDED.is_enabled,
                    payload = EXCLUDED.payload,
                    last_run_at = EXCLUDED.last_run_at,
                    next_run_at = EXCLUDED.next_run_at,
                    schedule_version = EXCLUDED.schedule_version,
                    updated_at = NOW()
            """.trimIndent()

            val lastRun = schedule.lastRunAt?.let { Timestamp(it) }
            val nextRun = Timestamp(schedule.nextRunAt)
            val created = Timestamp(schedule.createdAt)
            val updated = Timestamp(schedule.updatedAt)

            txContext.sqlExecutor.executeUpdate(
                sql = sql,
                params = listOf(
                    schedule.scheduleId,
                    tenantContext.projectId,
                    schedule.jobType,
                    schedule.cronExpression,
                    schedule.fixedIntervalMs,
                    schedule.timezone,
                    schedule.isEnabled,
                    schedule.payloadJson,
                    lastRun,
                    nextRun,
                    schedule.scheduleVersion,
                    created,
                    updated
                )
            )
        }
    }

    override suspend fun getScheduleById(scheduleId: String, tenantContext: TenantContext): JobScheduleDefinition? {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = "SELECT * FROM job_schedules WHERE project_id = ? AND schedule_id = ?"
            txContext.sqlExecutor.querySingleOrNull(sql, listOf(tenantContext.projectId, scheduleId)) { rs ->
                mapRowToSchedule(rs)
            }
        }
    }

    override suspend fun getDueSchedules(limit: Int, tenantContext: TenantContext): List<JobScheduleDefinition> {
        return transactionManager.inReadOnly(tenantContext) { txContext ->
            val sql = """
                SELECT * FROM job_schedules
                WHERE project_id = ? AND is_enabled = TRUE AND next_run_at <= NOW()
                ORDER BY next_run_at ASC
                LIMIT ?
            """.trimIndent()

            txContext.sqlExecutor.queryList(sql, listOf(tenantContext.projectId, limit)) { rs ->
                mapRowToSchedule(rs)
            }
        }
    }

    override suspend fun updateScheduleRunTimes(
        scheduleId: String,
        lastRunAt: Long,
        nextRunAt: Long,
        tenantContext: TenantContext
    ) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE job_schedules
                SET last_run_at = ?,
                    next_run_at = ?,
                    updated_at = NOW()
                WHERE project_id = ? AND schedule_id = ?
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(
                sql,
                listOf(Timestamp(lastRunAt), Timestamp(nextRunAt), tenantContext.projectId, scheduleId)
            )
        }
    }

    override suspend fun setScheduleEnabled(scheduleId: String, isEnabled: Boolean, tenantContext: TenantContext) {
        transactionManager.inTransaction(tenantContext) { txContext ->
            val sql = """
                UPDATE job_schedules
                SET is_enabled = ?,
                    updated_at = NOW()
                WHERE project_id = ? AND schedule_id = ?
            """.trimIndent()

            txContext.sqlExecutor.executeUpdate(sql, listOf(isEnabled, tenantContext.projectId, scheduleId))
        }
    }
}
