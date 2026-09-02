package com.sucharu.sucharupro.data.datasource.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialReportSchedule
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialReportScheduleStatus
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialScheduleExecution
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class FakeCustomerFinancialReportScheduleDataSource : CustomerFinancialReportScheduleDataSource {

    private val schedules = ConcurrentHashMap<String, CustomerFinancialReportSchedule>()
    private val executions = ConcurrentHashMap<String, CopyOnWriteArrayList<CustomerFinancialScheduleExecution>>()

    override suspend fun saveSchedule(schedule: CustomerFinancialReportSchedule) {
        schedules[schedule.scheduleId] = schedule
    }

    override suspend fun getScheduleById(
        tenantId: String,
        projectId: String,
        scheduleId: String
    ): CustomerFinancialReportSchedule? {
        val schedule = schedules[scheduleId] ?: return null
        return if (schedule.tenantId == tenantId && schedule.projectId == projectId) schedule else null
    }

    override suspend fun listSchedules(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerFinancialReportScheduleStatus?
    ): List<CustomerFinancialReportSchedule> {
        return schedules.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun getDueSchedules(
        tenantId: String,
        projectId: String,
        asOfTimestamp: Long,
        limit: Int
    ): List<CustomerFinancialReportSchedule> {
        return schedules.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { it.status == CustomerFinancialReportScheduleStatus.ACTIVE && it.nextRunAt <= asOfTimestamp }
            .sortedBy { it.nextRunAt }
            .take(limit)
    }

    override suspend fun recordExecution(execution: CustomerFinancialScheduleExecution) {
        executions.computeIfAbsent(execution.scheduleId) { CopyOnWriteArrayList() }.add(execution)
    }

    override suspend fun listExecutions(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        limit: Int
    ): List<CustomerFinancialScheduleExecution> {
        return executions[scheduleId]?.filter {
            it.tenantId == tenantId && it.projectId == projectId
        }?.sortedByDescending { it.executedAt }?.take(limit) ?: emptyList()
    }
}
