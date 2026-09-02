package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.CustomerFinancialReportScheduleDataSource
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import java.sql.ResultSet

class PostgresCustomerFinancialReportScheduleDataSource(
    private val transactionManager: TransactionManager
) : CustomerFinancialReportScheduleDataSource {

    private fun mapRowToSchedule(rs: ResultSet): CustomerFinancialReportSchedule {
        return CustomerFinancialReportSchedule(
            scheduleId = rs.getString("schedule_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            reportType = CustomerFinancialReportType.valueOf(rs.getString("report_type")),
            format = CustomerFinancialReportFormat.valueOf(rs.getString("format")),
            frequency = CustomerFinancialScheduleFrequency.valueOf(rs.getString("frequency")),
            timezone = rs.getString("timezone"),
            status = CustomerFinancialReportScheduleStatus.valueOf(rs.getString("status")),
            nextRunAt = rs.getLong("next_run_at"),
            lastRunAt = rs.getObject("last_run_at")?.let { rs.getLong("last_run_at") },
            lastRunStatus = rs.getString("last_run_status"),
            consecutiveFailures = rs.getInt("consecutive_failures"),
            createdBy = rs.getString("created_by"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
            version = rs.getLong("version")
        )
    }

    private fun mapRowToExecution(rs: ResultSet): CustomerFinancialScheduleExecution {
        return CustomerFinancialScheduleExecution(
            executionId = rs.getString("execution_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            scheduleId = rs.getString("schedule_id"),
            customerId = rs.getString("customer_id"),
            reportType = CustomerFinancialReportType.valueOf(rs.getString("report_type")),
            format = CustomerFinancialReportFormat.valueOf(rs.getString("format")),
            executedAt = rs.getLong("executed_at"),
            status = CustomerFinancialScheduleExecutionStatus.valueOf(rs.getString("status")),
            documentDeliveryId = rs.getString("document_delivery_id"),
            errorMessage = rs.getString("error_message"),
            correlationId = rs.getString("correlation_id")
        )
    }

    override suspend fun saveSchedule(schedule: CustomerFinancialReportSchedule) {
        val sql = """
            INSERT INTO customer_financial_report_schedules (
                schedule_id, tenant_id, project_id, customer_id, report_type, format,
                frequency, timezone, status, next_run_at, last_run_at, last_run_status,
                consecutive_failures, created_by, created_at, updated_at, version
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (schedule_id) DO UPDATE SET
                format = EXCLUDED.format,
                frequency = EXCLUDED.frequency,
                timezone = EXCLUDED.timezone,
                status = EXCLUDED.status,
                next_run_at = EXCLUDED.next_run_at,
                last_run_at = EXCLUDED.last_run_at,
                last_run_status = EXCLUDED.last_run_status,
                consecutive_failures = EXCLUDED.consecutive_failures,
                updated_at = EXCLUDED.updated_at,
                version = customer_financial_report_schedules.version + 1
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(schedule.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, schedule.scheduleId)
                stmt.setString(2, schedule.tenantId)
                stmt.setString(3, schedule.projectId)
                stmt.setString(4, schedule.customerId)
                stmt.setString(5, schedule.reportType.name)
                stmt.setString(6, schedule.format.name)
                stmt.setString(7, schedule.frequency.name)
                stmt.setString(8, schedule.timezone)
                stmt.setString(9, schedule.status.name)
                stmt.setLong(10, schedule.nextRunAt)
                if (schedule.lastRunAt != null) stmt.setLong(11, schedule.lastRunAt) else stmt.setNull(11, java.sql.Types.BIGINT)
                stmt.setString(12, schedule.lastRunStatus)
                stmt.setInt(13, schedule.consecutiveFailures)
                stmt.setString(14, schedule.createdBy)
                stmt.setLong(15, schedule.createdAt)
                stmt.setLong(16, schedule.updatedAt)
                stmt.setLong(17, schedule.version)
                stmt.executeUpdate()
            }
        }
    }

    override suspend fun getScheduleById(
        tenantId: String,
        projectId: String,
        scheduleId: String
    ): CustomerFinancialReportSchedule? {
        val sql = """
            SELECT * FROM customer_financial_report_schedules
            WHERE tenant_id = ? AND project_id = ? AND schedule_id = ?
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, scheduleId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapRowToSchedule(rs) else null
                }
            }
        }
    }

    override suspend fun listSchedules(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerFinancialReportScheduleStatus?
    ): List<CustomerFinancialReportSchedule> {
        val sqlBuilder = StringBuilder(
            "SELECT * FROM customer_financial_report_schedules WHERE tenant_id = ? AND project_id = ?"
        )
        val params = mutableListOf<String>(tenantId, projectId)

        if (!customerId.isNullOrBlank()) {
            sqlBuilder.append(" AND customer_id = ?")
            params.add(customerId)
        }
        if (status != null) {
            sqlBuilder.append(" AND status = ?")
            params.add(status.name)
        }

        sqlBuilder.append(" ORDER BY created_at DESC")

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sqlBuilder.toString()).use { stmt ->
                params.forEachIndexed { index, param ->
                    stmt.setString(index + 1, param)
                }
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<CustomerFinancialReportSchedule>()
                    while (rs.next()) {
                        list.add(mapRowToSchedule(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun getDueSchedules(
        tenantId: String,
        projectId: String,
        asOfTimestamp: Long,
        limit: Int
    ): List<CustomerFinancialReportSchedule> {
        val sql = """
            SELECT * FROM customer_financial_report_schedules
            WHERE tenant_id = ? AND project_id = ? AND status = 'ACTIVE' AND next_run_at <= ?
            ORDER BY next_run_at ASC
            LIMIT ?
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setLong(3, asOfTimestamp)
                stmt.setInt(4, limit)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<CustomerFinancialReportSchedule>()
                    while (rs.next()) {
                        list.add(mapRowToSchedule(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun recordExecution(execution: CustomerFinancialScheduleExecution) {
        val sql = """
            INSERT INTO customer_financial_report_schedule_executions (
                execution_id, tenant_id, project_id, schedule_id, customer_id,
                report_type, format, executed_at, status, document_delivery_id,
                error_message, correlation_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        transactionManager.inTransaction(TenantContext(execution.projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, execution.executionId)
                stmt.setString(2, execution.tenantId)
                stmt.setString(3, execution.projectId)
                stmt.setString(4, execution.scheduleId)
                stmt.setString(5, execution.customerId)
                stmt.setString(6, execution.reportType.name)
                stmt.setString(7, execution.format.name)
                stmt.setLong(8, execution.executedAt)
                stmt.setString(9, execution.status.name)
                stmt.setString(10, execution.documentDeliveryId)
                stmt.setString(11, execution.errorMessage)
                stmt.setString(12, execution.correlationId)
                stmt.executeUpdate()
            }
        }
    }

    override suspend fun listExecutions(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        limit: Int
    ): List<CustomerFinancialScheduleExecution> {
        val sql = """
            SELECT * FROM customer_financial_report_schedule_executions
            WHERE tenant_id = ? AND project_id = ? AND schedule_id = ?
            ORDER BY executed_at DESC
            LIMIT ?
        """.trimIndent()

        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            tx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, projectId)
                stmt.setString(3, scheduleId)
                stmt.setInt(4, limit)
                stmt.executeQuery().use { rs ->
                    val list = mutableListOf<CustomerFinancialScheduleExecution>()
                    while (rs.next()) {
                        list.add(mapRowToExecution(rs))
                    }
                    list
                }
            }
        }
    }
}
