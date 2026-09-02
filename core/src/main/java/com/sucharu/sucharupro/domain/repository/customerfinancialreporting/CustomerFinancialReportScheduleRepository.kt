package com.sucharu.sucharupro.domain.repository.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*

interface CustomerFinancialReportScheduleRepository {
    suspend fun saveSchedule(schedule: CustomerFinancialReportSchedule): DomainResult<CustomerFinancialReportSchedule>
    suspend fun getScheduleById(tenantId: String, projectId: String, scheduleId: String): DomainResult<CustomerFinancialReportSchedule?>
    suspend fun listSchedules(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CustomerFinancialReportScheduleStatus? = null
    ): DomainResult<List<CustomerFinancialReportSchedule>>
    suspend fun getDueSchedules(
        tenantId: String,
        projectId: String,
        asOfTimestamp: Long,
        limit: Int = 50
    ): DomainResult<List<CustomerFinancialReportSchedule>>
    suspend fun recordExecution(execution: CustomerFinancialScheduleExecution): DomainResult<Unit>
    suspend fun listExecutions(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        limit: Int = 50
    ): DomainResult<List<CustomerFinancialScheduleExecution>>
}
