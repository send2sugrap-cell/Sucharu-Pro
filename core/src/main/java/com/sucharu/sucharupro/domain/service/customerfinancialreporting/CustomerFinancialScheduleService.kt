package com.sucharu.sucharupro.domain.service.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*

interface CustomerFinancialScheduleService {

    suspend fun createSchedule(
        tenantId: String,
        projectId: String,
        customerId: String,
        reportType: CustomerFinancialReportType,
        format: CustomerFinancialReportFormat = CustomerFinancialReportFormat.PDF,
        frequency: CustomerFinancialScheduleFrequency,
        timezone: String = "Asia/Dhaka",
        firstRunAt: Long? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialReportSchedule>

    suspend fun updateSchedule(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        format: CustomerFinancialReportFormat?,
        frequency: CustomerFinancialScheduleFrequency?,
        timezone: String?,
        nextRunAt: Long?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialReportSchedule>

    suspend fun pauseSchedule(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialReportSchedule>

    suspend fun resumeSchedule(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialReportSchedule>

    suspend fun cancelSchedule(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerFinancialReportSchedule>

    suspend fun listSchedules(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CustomerFinancialReportScheduleStatus? = null
    ): DomainResult<List<CustomerFinancialReportSchedule>>

    suspend fun getSchedule(
        tenantId: String,
        projectId: String,
        scheduleId: String
    ): DomainResult<CustomerFinancialReportSchedule>

    suspend fun executeDueSchedules(
        tenantId: String,
        projectId: String,
        asOfTimestamp: Long = System.currentTimeMillis(),
        actorId: String = "scheduler_worker",
        actorRole: String = "SYSTEM"
    ): DomainResult<List<CustomerFinancialScheduleExecution>>

    suspend fun listExecutions(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        limit: Int = 50
    ): DomainResult<List<CustomerFinancialScheduleExecution>>
}
