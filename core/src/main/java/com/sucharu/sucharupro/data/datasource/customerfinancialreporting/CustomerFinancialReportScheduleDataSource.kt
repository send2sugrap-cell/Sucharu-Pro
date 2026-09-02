package com.sucharu.sucharupro.data.datasource.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialReportSchedule
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialReportScheduleStatus
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialScheduleExecution

interface CustomerFinancialReportScheduleDataSource {
    suspend fun saveSchedule(schedule: CustomerFinancialReportSchedule)
    suspend fun getScheduleById(tenantId: String, projectId: String, scheduleId: String): CustomerFinancialReportSchedule?
    suspend fun listSchedules(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CustomerFinancialReportScheduleStatus? = null
    ): List<CustomerFinancialReportSchedule>
    suspend fun getDueSchedules(
        tenantId: String,
        projectId: String,
        asOfTimestamp: Long,
        limit: Int = 50
    ): List<CustomerFinancialReportSchedule>
    suspend fun recordExecution(execution: CustomerFinancialScheduleExecution)
    suspend fun listExecutions(
        tenantId: String,
        projectId: String,
        scheduleId: String,
        limit: Int = 50
    ): List<CustomerFinancialScheduleExecution>
}
